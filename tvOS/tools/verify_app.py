#!/usr/bin/env python3
"""Verify a built tvOS .app bundle.

Usage:
    python3 tools/verify_app.py path/to/IPVIOStream.app [--require-release]

Checks the bundle metadata, the Mach-O architecture (arm64), and that the
KSPlayer/FFmpeg frameworks are embedded. Works on Windows, macOS and Linux.
"""
from __future__ import annotations

import argparse
import plistlib
import struct
import sys
from pathlib import Path

ARM64 = 0x0100000C
X86_64 = 0x01000007
CPU_NAMES = {ARM64: "arm64", X86_64: "x86_64", 7: "i386", 12: "arm"}


def macho_architectures(path: Path) -> list[str]:
    data = path.read_bytes()[:4096]
    if len(data) < 8:
        return []
    magic = data[:4]
    if magic == b"\xcf\xfa\xed\xfe":  # 64-bit little endian
        cpu = struct.unpack_from("<I", data, 4)[0]
        return [CPU_NAMES.get(cpu, f"0x{cpu:08x}")]
    if magic == b"\xce\xfa\xed\xfe":  # 32-bit little endian
        cpu = struct.unpack_from("<I", data, 4)[0]
        return [CPU_NAMES.get(cpu, f"0x{cpu:08x}")]
    if magic == b"\xca\xfe\xba\xbe":  # fat binary, big endian
        count = struct.unpack_from(">I", data, 4)[0]
        result = []
        offset = 8
        for _ in range(min(count, 32)):
            if offset + 20 > len(data):
                break
            cpu, _sub, _off, _size, _align = struct.unpack_from(">IIIII", data, offset)
            result.append(CPU_NAMES.get(cpu, f"0x{cpu:08x}"))
            offset += 20
        return result
    return []


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("app", type=Path)
    parser.add_argument("--require-release", action="store_true",
                        help="fail if debug/preview dylibs are present")
    args = parser.parse_args()

    app = args.app
    if not app.is_dir():
        print(f"error: {app} is not a directory", file=sys.stderr)
        return 2

    info_path = app / "Info.plist"
    if not info_path.exists():
        print("error: Info.plist missing", file=sys.stderr)
        return 1
    with info_path.open("rb") as fh:
        info = plistlib.load(fh)

    executable = app / str(info.get("CFBundleExecutable", "IPVIOStream"))
    if not executable.exists():
        print(f"error: executable missing: {executable}", file=sys.stderr)
        return 1

    print(f"Bundle:      {app}")
    print(f"Identifier:  {info.get('CFBundleIdentifier')}")
    print(f"Version:     {info.get('CFBundleShortVersionString')} ({info.get('CFBundleVersion')})")
    print(f"Platform:    {info.get('CFBundleSupportedPlatforms')}")
    print(f"Minimum OS:  {info.get('MinimumOSVersion')}")
    print(f"Executable:  {executable.name} ({executable.stat().st_size:,} bytes)")

    main_arch = macho_architectures(executable)
    print(f"Architecture:{' ' + ', '.join(main_arch) if main_arch else ' unknown'}")
    if "arm64" not in main_arch:
        print("error: executable is not arm64 (Apple TV device)", file=sys.stderr)
        return 1

    frameworks = app / "Frameworks"
    ksplayer = frameworks / "KSPlayer.framework" / "KSPlayer"
    if not ksplayer.exists():
        print("error: KSPlayer.framework is not embedded", file=sys.stderr)
        return 1
    print(f"KSPlayer:    {ksplayer.stat().st_size:,} bytes, arch {macho_architectures(ksplayer)}")

    checked = 0
    missing_arm64 = []
    for framework in sorted(frameworks.glob("*.framework")):
        binary = framework / framework.stem
        if not binary.exists():
            continue
        checked += 1
        archs = macho_architectures(binary)
        if "arm64" not in archs:
            missing_arm64.append((framework.name, archs))
    print(f"Frameworks:  {checked} checked")
    if missing_arm64:
        print(f"error: frameworks without arm64: {missing_arm64}", file=sys.stderr)
        return 1

    debug_files = [p.name for p in app.iterdir() if p.name.endswith(".debug.dylib") or p.name == "__preview.dylib"]
    if debug_files:
        message = f"debug/preview files present: {debug_files}"
        print(("error: " if args.require_release else "warning: ") + message, file=sys.stderr)
        if args.require_release:
            return 1

    print("OK: tvOS app bundle looks valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
