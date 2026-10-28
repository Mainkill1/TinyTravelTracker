Run `scripts/test-map-overlay-pause.sh` with JDK 17 (no Android SDK required).

The harness compiles the actual `GpsTrailerOverlay`, `SuperThread`,
`SuperThreadManager`, and `ReadWriteThreadManager`. Android UI, Tangram, and
cache/database collaborators have small host stubs. The cache collaborator
provides 20 refinement passes and can request a pause during the first pass;
the activity collaborator can request a pause after the first drawing round.

The scenarios verify:

- A pause stops the next refinement inside a long redraw batch.
- A pause after a drawing round stops the next outer round.
- Actual cache and view-node writers acquire their locks while the map is paused.
- Resume completes calculation, draws the location point with the existing
  corner markers, retains the visible time range, and avoids repeating distance.
- Shutdown wakes a paused worker, exits without more calculation, and releases
  cache access.

The original code fails by running all 20 refinement passes after pause.
Waiting at either checkpoint while holding cache access also fails the writer
assertion. Rendering, native Tangram behavior, database throughput, and elapsed
pause latency on an Android device still need device-level validation. A single
cache calculation or drawing operation already in flight may finish before
pausing; the checkpoints bound further passes, not those operations' duration.
