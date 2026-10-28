Run `scripts/test-map-gestures.sh` with a JDK (17 or newer). It compiles the
actual application `MyMapController` and `MyTouchInput` classes. Android gesture
detectors and Tangram's native renderer are the stubbed boundaries: the tests
deliver detector callbacks and inspect events/arguments passed across them.
They do not simulate Android's detector timing or native rendering.

The quick-scale fixture models factor 0.88 over 1 ms, which Tangram 0.10.1 turns
into velocity -120/s. Its separate scale detector runs even when the double-tap
responder consumes a tap. The fix preserves direct scaling and forwards zero
velocity for both pinch and quick-scale; the intentional tradeoff is no zoom
inertia after release. Invalid scale factors never reach the renderer.

Long-press fixtures cover cancellation before custom ownership, continued custom
drag, release without a stray SDK UP, CANCEL without committing selection, and
fresh-DOWN recovery when an old terminal event is missing. Cancellation propagates
through the controller responder chain; overlays discard the preview rectangle
without adding a selected area.

Primary sources:
- https://github.com/tangrams/tangram-es/blob/0.10.1/platforms/android/tangram/src/main/java/com/mapzen/tangram/TouchInput.java
- https://github.com/tangrams/tangram-es/blob/0.10.1/platforms/android/tangram/src/main/java/com/mapzen/tangram/MapController.java

Device follow-up: pinch and double-tap-with-small-drift at several zoom levels;
long-press drag/release; interrupt a long press with system navigation and then
pan/tap again. No emulator/device execution is claimed by these host tests.
