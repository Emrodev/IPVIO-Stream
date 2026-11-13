"""Mock Xtream Codes server for the IPVIO end-to-end test on the emulator (reach it as http://10.0.2.2:PORT).

Serves get.php (M3U), player_api.php (minimal JSON) and the stream files (one real MKV, with Range support).
Test data:
- 40 live categories with 6 channels each (Right/Left focus deep in the Live TV category list)
- "EN | MOVIES BIG": 300 movies (categories preview beyond the old 40-title cap)
- the same 12 movies in "EN | LATEST" and "FR | FILMS" (search version labels)
- a few "Batman" titles for the search test
- one series with 2 seasons x 5 episodes
- "news" in a channel, a movie and a series (search from Home)
Usage: python3 mock_xtream.py [--video FILE.mkv] [--port 8098] [--host 10.0.2.2]
  --host: address the TV uses to reach this computer (10.0.2.2 = the host as seen from the Android emulator).
  --video: any MKV/MP4 served for every stream (make one with:
           ffmpeg -f lavfi -i testsrc=duration=300:size=1280x720:rate=25 -f lavfi -i sine=duration=300 -c:v libx264 -c:a aac test.mkv).
           Without it, streams answer 404 (navigation tests still work, playback tests fail).
Playlist address for the app: http://<host>:<port>/get.php?username=demo&password=demo&type=m3u_plus
"""
import json, os, re, sys, time
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

import argparse
_a = argparse.ArgumentParser()
_a.add_argument("--video", default=None)
_a.add_argument("--port", type=int, default=8098)
_a.add_argument("--host", default="10.0.2.2")
ARGS = _a.parse_args()
PORT = ARGS.port
HOST = f"http://{ARGS.host}:{PORT}"
U, P = "demo", "demo"
VIDEO = os.path.abspath(ARGS.video) if ARGS.video else None
NOW = 1_763_000_000

live, movies, series = [], [], []
sid = 1000
for c in range(1, 41):
    for n in range(1, 7):
        sid += 1
        live.append((sid, f"Channel {c:02d}-{n}", f"LIVE | CATEGORY {c:02d}"))
for name in ("World News 24", "News Live HD", "Business News"):  # T10 searches "news" across all areas
    sid += 1; live.append((sid, name, "NEWS | WORLD"))
mid = 5000
for n in range(1, 301):
    mid += 1
    movies.append((mid, f"Big Movie {n:03d} (2020)", "EN | MOVIES BIG", NOW - (300 - n) * 3600))
shared = ["Merry Little Batman (2023)", "The Batman (2022)", "Batman Begins (2005)", "Dune (2021)", "Arrival (2016)",
          "Inception (2010)", "Interstellar (2014)", "Tenet (2020)", "Heat (1995)", "Alien (1979)", "Up (2009)", "Coco (2017)"]
for i, t in enumerate(shared):
    mid += 1; movies.append((mid, t, "EN | LATEST", NOW - i * 60))
    mid += 1; movies.append((mid, t, "FR | FILMS", NOW - i * 60 - 30))
mid += 1; movies.append((mid, "Batman Ninja (2018)", "ANIME | MOVIES", NOW - 7200))
mid += 1; movies.append((mid, "Newsies (1992)", "EN | LATEST", NOW - 9000))
eps = []
eid = 9000
for s in (1, 2):
    for e in range(1, 6):
        eid += 1
        eps.append((eid, f"Mock Show S{s:02d}E{e:02d}", "EN | SERIES"))
for e in range(1, 4):
    eid += 1; eps.append((eid, f"The Newsroom S01E{e:02d}", "EN | SERIES"))


def m3u():
    out = ["#EXTM3U"]
    for i, name, g in live:
        out.append(f'#EXTINF:-1 tvg-id="ch{i}" group-title="{g}",{name}')
        out.append(f"{HOST}/live/{U}/{P}/{i}.ts")
    for i, name, g, _ in movies:
        out.append(f'#EXTINF:-1 group-title="{g}",{name}')
        out.append(f"{HOST}/movie/{U}/{P}/{i}.mkv")
    for i, name, g in eps:
        out.append(f'#EXTINF:-1 group-title="{g}",{name}')
        out.append(f"{HOST}/series/{U}/{P}/{i}.mkv")
    return "\n".join(out) + "\n"


def api(q):
    a = q.get("action", [""])[0]
    if a == "":
        return {"user_info": {"username": U, "auth": 1, "status": "Active", "exp_date": None, "max_connections": "1"},
                "server_info": {"url": "10.0.2.2", "port": str(PORT), "server_protocol": "http"}}
    if a == "get_live_categories":
        return [{"category_id": str(c), "category_name": f"LIVE | CATEGORY {c:02d}"} for c in range(1, 41)]
    if a in ("get_vod_categories", "get_series_categories", "get_series", "get_vod_streams", "get_live_streams"):
        return []
    return {}


class H(BaseHTTPRequestHandler):
    def do_HEAD(self): self.route(False)
    def do_GET(self): self.route(True)

    def route(self, body):
        u = urlparse(self.path); q = parse_qs(u.query)
        print(time.strftime("%H:%M:%S"), self.command, u.path, self.headers.get("Range") or "", flush=True)
        if u.path == "/get.php":
            return self.send(m3u().encode(), "audio/x-mpegurl", body)
        if u.path == "/player_api.php":
            return self.send(json.dumps(api(q)).encode(), "application/json", body)
        if re.match(r"^/(live|movie|series)/", u.path):
            if not VIDEO or not os.path.isfile(VIDEO): return self.send_error(404)
            return self.file(VIDEO, body)
        self.send_error(404)

    def send(self, data, ctype, body):
        self.send_response(200); self.send_header("Content-Type", ctype); self.send_header("Content-Length", str(len(data))); self.end_headers()
        if body: self.wfile.write(data)

    def file(self, p, body):
        size = os.path.getsize(p); rng = self.headers.get("Range"); start, end = 0, size - 1
        m = re.match(r"bytes=(\d*)-(\d*)", rng or "")
        if m:
            if m.group(1): start = int(m.group(1))
            if m.group(2): end = min(int(m.group(2)), size - 1)
            self.send_response(206); self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        else:
            self.send_response(200)
        self.send_header("Content-Type", "video/x-matroska"); self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(end - start + 1)); self.end_headers()
        if not body: return
        with open(p, "rb") as f:
            f.seek(start); left = end - start + 1
            try:
                while left > 0:
                    b = f.read(min(65536, left))
                    if not b: break
                    self.wfile.write(b); left -= len(b)
            except (BrokenPipeError, ConnectionResetError):
                pass

    def log_message(self, *a): pass


ThreadingHTTPServer(("0.0.0.0", PORT), H).serve_forever()
