Run `scripts/test-map-overlay-pause.sh` with JDK 17 (no Android SDK required).

The harness compiles the actual `GpsTrailerOverlay`, `SuperThread`,
`SuperThreadManager`, `ReadWriteThreadManager`, and `ViewNode`. Android UI,
Tangram, and cache/database collaborators have small host stubs. A small javac
AST extractor inserts the exact production `discardViewNodes` method into the
cache fixture, without rewriting its body. The cache collaborator supplies
refinement passes with a partially dirty real view node; the activity
collaborator can request a pause after the first drawing round.

The five scenarios verify:

- A pause stops the next refinement inside a long redraw batch.
- A pause after a drawing round stops the next outer round and distance work.
- Actual cache and view-node writers acquire their locks while the map is paused.
- Resume completes calculation, draws the location point with the existing
  corner markers, retains the visible time range, and refreshes distance.
- A GPS writer while paused sees a fresh unknown head, while the discarded real
  `ViewNode` rejects incremental updates because it is dirty. New GPS data and a
  changed requested view are reflected in the resumed output.
- Shutdown wakes a paused worker, and shutdown during a drawing round discards
  the partial tree before releasing cache access.

The original code runs all refinement passes after pause. Simply dropping its
read registration lets a cache writer reach the real `ViewNode.addPointToHead`
assertion: incremental updates require a clean tree. Waiting with cache access
held fails the writer-access assertion instead. The corrected worker discards
partial view state and returns through its existing cleanup, allowing the
scheduler to pause with no cache locks and fresh locals on resume.

Removing the head replacement or either line-index clear from the production
`discardViewNodes` method makes the regression fail. The real cache producer and
database traversal remain stubbed; rendering, native Tangram behavior, database
throughput, and elapsed pause latency still need
Android device validation. A cache calculation or drawing operation already in
flight may finish before yielding; checkpoints bound further passes, not those
operations' duration.
