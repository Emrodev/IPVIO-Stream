package com.ipvio.stream

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.text.SubtitleParser

/**
 * Matroska without a Cues index (issue #10): Media3 publishes such files as unseekable, so skip, jump and "Resume"
 * restarted the movie at 0:00. When the duration and the file size are known, this wrapper publishes an estimated
 * seek map instead. After a seek it looks for the next Cluster, reads its timecode and corrects the byte position
 * (see [MkvSeekLogic]) until the Cluster is close to the target, then hands over to the normal extractor.
 * Files with Cues and all other formats are not touched.
 */
@OptIn(UnstableApi::class)
class CuelessMkvExtractor(private val delegate: Extractor) : Extractor {
    private var durationUs = C.TIME_UNSET
    private var length = C.LENGTH_UNSET.toLong()
    private var estimating = false
    private var scanning = false
    private var targetUs = 0L
    private var hops = 0
    private var windows = 0
    private val points = ArrayList<MkvSeekLogic.Point>()
    private val buf = ByteArray(WINDOW)

    override fun sniff(input: ExtractorInput): Boolean = delegate.sniff(input)

    override fun init(output: ExtractorOutput) {
        delegate.init(object : ExtractorOutput by output {
            override fun seekMap(seekMap: SeekMap) {
                val d = seekMap.durationUs
                if (!seekMap.isSeekable && d != C.TIME_UNSET && d > 0 && length > 0) {
                    durationUs = d
                    estimating = true
                    output.seekMap(EstimatedMap(d, length))
                } else output.seekMap(seekMap)
            }
        })
    }

    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        if (input.length != C.LENGTH_UNSET.toLong()) length = input.length
        return if (scanning) scan(input, seekPosition) else delegate.read(input, seekPosition)
    }

    override fun seek(position: Long, timeUs: Long) {
        scanning = estimating && timeUs > 0 && position > 0
        if (scanning) { targetUs = timeUs; hops = 0; windows = 0; points.clear() }
        delegate.seek(position, timeUs)
    }

    override fun release() = delegate.release()

    override fun getUnderlyingImplementation(): Extractor = delegate.getUnderlyingImplementation()

    /** Looks for the next Cluster from the current position and decides: hand over, jump again, or give up. */
    private fun scan(input: ExtractorInput, seekPosition: PositionHolder): Int {
        while (true) {
            val start = input.position
            val n = peekWindow(input)
            val hit = MkvSeekLogic.findCluster(buf, n)
            input.resetPeekPosition()
            if (hit == null) {
                if (n < OVERLAP * 2 || ++windows > MAX_WINDOWS) return giveUp(seekPosition)
                input.skipFully(n - OVERLAP)
                continue
            }
            windows = 0
            val pos = start + hit.offset
            // Timecodes are assumed in the default scale (1 ms); a value beyond the duration means another scale
            val t = hit.timecode * 1000L
            if (t > durationUs + durationUs / 20 + 1_000_000L) return giveUp(seekPosition)
            points.add(MkvSeekLogic.Point(pos, t))
            if (MkvSeekLogic.closeEnough(t, targetUs)) {
                input.skipFully(hit.offset)
                return handOver(input, seekPosition, pos)
            }
            if (++hops >= MkvSeekLogic.MAX_HOPS) return settle(input, seekPosition, pos, hit.offset)
            val next = MkvSeekLogic.refine(targetUs, points, durationUs, length)
            // No progress (same spot again): take the best point measured so far
            if (points.count { it.pos == pos } > 1 || next == start) return settle(input, seekPosition, pos, hit.offset)
            seekPosition.position = next
            return Extractor.RESULT_SEEK
        }
    }

    /** Out of hops: the latest Cluster at or before the target, or the current one if none is before it. */
    private fun settle(input: ExtractorInput, seekPosition: PositionHolder, pos: Long, offset: Int): Int {
        val best = points.filter { it.timeUs <= targetUs }.maxByOrNull { it.timeUs }
        if (best == null || best.pos == pos) { input.skipFully(offset); return handOver(input, seekPosition, pos) }
        scanning = false
        delegate.seek(best.pos, targetUs)
        seekPosition.position = best.pos
        return Extractor.RESULT_SEEK
    }

    /** The input stands at a Cluster start: the normal extractor continues from here. */
    private fun handOver(input: ExtractorInput, seekPosition: PositionHolder, pos: Long): Int {
        scanning = false
        delegate.seek(pos, targetUs)
        return delegate.read(input, seekPosition)
    }

    /** Like before this change: start again from the beginning. */
    private fun giveUp(seekPosition: PositionHolder): Int {
        scanning = false
        estimating = false
        delegate.seek(0L, 0L)
        seekPosition.position = 0L
        return Extractor.RESULT_SEEK
    }

    /** Peeks up to one window; returns the number of bytes available (fewer at the end of the file). */
    private fun peekWindow(input: ExtractorInput): Int {
        var n = 0
        while (n < buf.size) {
            val r = input.peek(buf, n, buf.size - n)
            if (r == C.RESULT_END_OF_INPUT) break
            n += r
        }
        return n
    }

    private class EstimatedMap(private val durationUs: Long, private val length: Long) : SeekMap {
        override fun isSeekable() = true
        override fun getDurationUs() = durationUs
        override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints =
            SeekMap.SeekPoints(SeekPoint(timeUs, if (timeUs <= 0) 0L else MkvSeekLogic.estimate(timeUs, durationUs, length)))
    }

    /** Extractors as by default; only Matroska ones get the wrapper. Settings are passed on to the default factory. */
    class Factory(private val inner: DefaultExtractorsFactory = DefaultExtractorsFactory()) : ExtractorsFactory {
        override fun createExtractors(): Array<Extractor> = wrap(inner.createExtractors())
        override fun createExtractors(uri: Uri, responseHeaders: Map<String, List<String>>): Array<Extractor> =
            wrap(inner.createExtractors(uri, responseHeaders))
        override fun experimentalSetTextTrackTranscodingEnabled(enabled: Boolean): ExtractorsFactory {
            inner.experimentalSetTextTrackTranscodingEnabled(enabled); return this
        }
        override fun setSubtitleParserFactory(f: SubtitleParser.Factory): ExtractorsFactory {
            inner.setSubtitleParserFactory(f); return this
        }
        override fun experimentalSetCodecsToParseWithinGopSampleDependencies(codecs: Int): ExtractorsFactory {
            inner.experimentalSetCodecsToParseWithinGopSampleDependencies(codecs); return this
        }
        override fun setParseHagcMetadata(parse: Boolean): ExtractorsFactory {
            inner.setParseHagcMetadata(parse); return this
        }
        private fun wrap(a: Array<Extractor>) =
            Array(a.size) { i -> if (a[i].underlyingImplementation is MatroskaExtractor) CuelessMkvExtractor(a[i]) else a[i] }
    }

    private companion object {
        const val WINDOW = 64 * 1024
        const val OVERLAP = 32
        /**
         * Give up after this many windows without a Cluster (12 MiB). Muxers start a new Cluster at about 5 MB at the
         * latest (ffmpeg, mkvmerge), so a high-bitrate file can have several MB between two Cluster headers.
         */
        const val MAX_WINDOWS = 192
    }
}
