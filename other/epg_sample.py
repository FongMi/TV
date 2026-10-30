#!/usr/bin/env python3
"""Local, synthetic XMLTV/catch-up fixture; no IPTV credentials required.

Run: python3 other/epg_sample.py --bind 0.0.0.0 --port 8769
Set the mobile live configuration to http://<computer-LAN-IP>:8769/config.json.
The two channels play a generated ten-minute silent WAV. Historical selections
request the same synthetic media with catch-up parameters; this checks EPG UI
and URL formatting, not real historical video availability.
"""

import argparse
import datetime as dt
import json
import re
import struct
import xml.etree.ElementTree as ET
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit
from zoneinfo import ZoneInfo


def epg(today, zone):
    root = ET.Element("tv")
    for channel in ("demo1", "demo2"):
        ET.SubElement(ET.SubElement(root, "channel", id=channel), "display-name").text = channel
        for offset in range(-7, 3):
            day = today + dt.timedelta(days=offset)
            for hour in range(24):
                start = dt.datetime.combine(day, dt.time(hour), zone)
                stop = dt.datetime.combine(day + dt.timedelta(days=hour == 23), dt.time((hour + 1) % 24), zone)
                programme = ET.SubElement(root, "programme", channel=channel,
                                          start=start.strftime("%Y%m%d%H%M%S %z"),
                                          stop=stop.strftime("%Y%m%d%H%M%S %z"))
                ET.SubElement(programme, "title").text = f"{channel} {day} {hour:02}:00"
    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


WAV_BYTES = 8000 * 2 * 600
WAV_HEADER = struct.pack("<4sI4s4sIHHIIHH4sI", b"RIFF", WAV_BYTES + 36, b"WAVE", b"fmt ",
                         16, 1, 1, 8000, 16000, 2, 16, b"data", WAV_BYTES)


class Handler(BaseHTTPRequestHandler):
    zone = ZoneInfo("Asia/Shanghai")

    def do_HEAD(self):
        self.respond(head=True)

    def do_GET(self):
        self.respond(head=False)

    def respond(self, head):
        path = urlsplit(self.path).path
        base = "http://" + self.headers.get("Host", "127.0.0.1:8769")
        if path == "/config.json":
            data = json.dumps({"lives": [{"name": "EPG demo", "url": base + "/live.m3u",
                                        "epg": base + "/epg.xml", "timeZone": self.zone.key}]}).encode()
            mime = "application/json"
        elif path == "/live.m3u":
            lines = [f'#EXTM3U x-tvg-url="{base}/epg.xml"']
            for channel in ("demo1", "demo2"):
                lines.extend([f'#EXTINF:-1 tvg-id="{channel}" catchup="append" catchup-days="7" '
                              f'catchup-source="?utc={{utc:}}&utcend={{utcend:}}",{channel}',
                              f"{base}/demo.wav?channel={channel}"])
            data = ("\n".join(lines) + "\n").encode()
            mime = "audio/x-mpegurl"
        elif path == "/epg.xml":
            data = epg(dt.datetime.now(self.zone).date(), self.zone)
            mime = "application/xml"
        elif path == "/demo.wav":
            self.media(head)
            return
        else:
            self.send_error(404)
            return
        self.send_response(200)
        self.send_header("Content-Type", mime)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        if not head:
            self.wfile.write(data)

    def media(self, head):
        total = len(WAV_HEADER) + WAV_BYTES
        start, end = 0, total - 1
        requested = self.headers.get("Range")
        if requested:
            match = re.fullmatch(r"bytes=(\d*)-(\d*)", requested)
            if not match or not any(match.groups()):
                self.send_error(416)
                return
            left, right = match.groups()
            if left:
                start = int(left)
                end = min(int(right), end) if right else end
            else:
                start = max(0, total - int(right))
            if not 0 <= start <= end < total:
                self.send_response(416)
                self.send_header("Content-Range", f"bytes */{total}")
                self.end_headers()
                return
        self.send_response(206 if requested else 200)
        self.send_header("Content-Type", "audio/wav")
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(end - start + 1))
        if requested:
            self.send_header("Content-Range", f"bytes {start}-{end}/{total}")
        self.end_headers()
        if head:
            return
        try:
            if start < len(WAV_HEADER):
                self.wfile.write(WAV_HEADER[start:min(end + 1, len(WAV_HEADER))])
                start = len(WAV_HEADER)
            while start <= end:
                size = min(65536, end - start + 1)
                self.wfile.write(bytes(size))
                start += size
        except (BrokenPipeError, ConnectionResetError):
            pass


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bind", default="127.0.0.1", help="use 0.0.0.0 for a phone on the same LAN")
    parser.add_argument("--port", type=int, default=8769)
    parser.add_argument("--zone", default="Asia/Shanghai")
    args = parser.parse_args()
    Handler.zone = ZoneInfo(args.zone)
    print(f"Synthetic EPG fixture listening on {args.bind}:{args.port}", flush=True)
    ThreadingHTTPServer((args.bind, args.port), Handler).serve_forever()
