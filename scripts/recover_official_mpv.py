"""Restore the verified 5.6.6 MPV disc-menu binary to the matching Media3 build.

The public MPV binary has libdvdnav/libbluray, but lacks mpv's `discnav` command.
Only libmpv.so is replaced. FFmpeg and the Android libplayer JNI bridge remain
source-build dependencies and are checked against the replacement's ELF symbols.
"""

import argparse
import hashlib
import re
import struct
import subprocess
import tempfile
import zipfile
from pathlib import Path


APK_HASHES = {
    "arm64-v8a": "0906ebed0424c25df4886735efc40146044f9c78ee2b7e7a5d1c470da56c6173",
    "armeabi-v7a": "ff6bfa26006a50c6825b64d2f6a3107dfa99b8277509909bd10987b6ca09fcc5",
}
ELF_MACHINE = {"arm64-v8a": (2, 183), "armeabi-v7a": (1, 40)}
MENU_MARKERS = (b"discnav\0", b"disc-menu-active\0", b"dvdnav_menu_call\0", b"bd_menu_call\0")
FFMPEG_LIBS = (
    "avcodec", "avfilter", "avformat", "avutil", "avdevice", "swresample", "swscale"
)
SYMBOL = re.compile(r"\s*\d+:\s+\S+\s+\d+\s+\w+\s+\w+\s+\w+\s+(\S+)\s+(\S+)")


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def symbols(path, *, undefined):
    lines = subprocess.check_output(["readelf", "-W", "--dyn-syms", str(path)], text=True)
    result = set()
    for line in lines.splitlines():
        match = SYMBOL.match(line)
        if match and (match[1] == "UND") == undefined:
            result.add(match[2].replace("@@", "@"))
    return result


def verify_elf(payload, abi):
    elf_class, machine = ELF_MACHINE[abi]
    if (len(payload) < 20 or payload[:4] != b"\x7fELF" or payload[4] != elf_class
            or payload[5] != 1 or struct.unpack_from("<H", payload, 16)[0] != 3
            or struct.unpack_from("<H", payload, 18)[0] != machine):
        raise ValueError(f"Invalid {abi} MPV ELF")
    if not all(marker in payload for marker in MENU_MARKERS):
        raise ValueError(f"Official {abi} MPV does not contain the menu commands")


def verify_linkage(official, source_root, abi):
    dependencies = source_root / "libraries" / "decoder_ffmpeg" / "src" / "main" / "jniLibs" / abi
    bridge = source_root / "libraries" / "mpvplayer" / "src" / "main" / "jniLibs" / abi / "libplayer.so"
    with tempfile.TemporaryDirectory() as tmp:
        binary = Path(tmp) / "libmpv.so"
        binary.write_bytes(official)
        required = symbols(binary, undefined=True)
        exports = set().union(*(symbols(dependencies / f"lib{name}.so", undefined=False)
                                for name in FFMPEG_LIBS))
        missing_ffmpeg = sorted(symbol for symbol in required if symbol.startswith(
            ("av_", "avcodec_", "avfilter_", "avformat_", "avio_", "swr_", "sws_"))
            and "@LIB" in symbol and symbol not in exports)
        if missing_ffmpeg:
            raise ValueError(f"{abi} MPV requires missing FFmpeg symbols: {missing_ffmpeg}")
        mpv_exports = {symbol.split("@")[0] for symbol in symbols(binary, undefined=False)}
        bridge_imports = {symbol.split("@")[0] for symbol in symbols(bridge, undefined=True)
                          if symbol.startswith("mpv_")}
        missing_bridge = sorted(bridge_imports - mpv_exports)
        if missing_bridge:
            raise ValueError(f"{abi} libplayer JNI requires missing MPV symbols: {missing_bridge}")
        print(f"{abi}: MPV menu commands and JNI/FFmpeg symbol linkage verified")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--arm64", type=Path)
    parser.add_argument("--armeabi", type=Path)
    parser.add_argument("--media3-root", type=Path, required=True)
    args = parser.parse_args()
    apks = {abi: path for abi, path in (("arm64-v8a", args.arm64),
                                       ("armeabi-v7a", args.armeabi)) if path is not None}
    if not apks:
        parser.error("Provide at least one official APK")

    verified = {}
    for abi, apk in apks.items():
        digest = sha256(apk)
        if digest != APK_HASHES[abi].lower():
            raise ValueError(f"Official {abi} APK SHA256 mismatch: {digest}")
        with zipfile.ZipFile(apk) as archive:
            binary = archive.read(f"lib/{abi}/libmpv.so")  # Also verifies ZIP CRC.
        verify_elf(binary, abi)
        verify_linkage(binary, args.media3_root, abi)
        verified[abi] = binary

    for abi, binary in verified.items():
        target = (args.media3_root / "libraries" / "mpvplayer" / "src" / "main"
                  / "jniLibs" / abi / "libmpv.so")
        target.write_bytes(binary)
        print(f"{abi}: restored {len(binary)} verified bytes to {target}")


if __name__ == "__main__":
    main()
