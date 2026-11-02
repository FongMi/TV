# EPG source checks

Run from the repository root with JDK 21, an Android SDK containing platform 37.0,
and Gradle 9.7.1 (the wrapper also works):

```sh
./gradlew -p other/epg-checks -PandroidSdk="$ANDROID_HOME" test
```

This standalone Java/Robolectric project compiles the production EPG parser,
API, view model, timeline, refresh policy, replay controller and mobile information
bar directly from `app/src`. It does not copy their implementations. It can run
without the app's custom Media3/MpvPlayer AARs.

`fixtures/` supplies boundary substitutes for the channel/source configuration,
network, filesystem, lifecycle notifications, player metadata and generated view
binding. Android views use Robolectric. These checks exercise scheduling,
normalization, refresh failure, response scoping and replay request transitions;
they do not establish full app compilation, decoder behavior, lifecycle races,
TV remote acceptance or acceptance on an Android device.

The suite includes full/partial/empty guides, midnight carry-over and split episodes,
duplicates and overlaps, DST days, local-only selection, request end clipping,
continuation within a programme and across midnight, live-edge return, no-DVR
channels, old-channel metadata isolation, refresh backoff and stale completion
protection. Information-bar layout checks cover 800×360, 640×320, 390×844 and
320×640 viewports and failing metadata/refresh/provider callbacks.

For manual integration testing:

```sh
python3 other/epg_sample.py --bind 0.0.0.0 --port 8769
```

Load `http://<computer-LAN-IP>:8769/config.json` on a device. The synthetic source
provides complete, partial, empty and split-midnight guides through both XMLTV
and the date API. Its silent WAV is synthetic and cannot validate an operator's
historical video availability. Check changing dates/channels, replay through a
gap and midnight, live-edge return, and single/double taps on the device. Keep the
normal TV controls and remote mappings in the TV flavor during later integration.
