Run `scripts/test-cache-scheduling.sh` with a JDK (17 or newer in CI).

The harness uses the Java compiler's parser to extract the exact `run()`,
`notifyMediaDirty()`, and `shutdown()` method source from the production cache
creator on every run, compiles those unchanged bodies into a host fixture, and
executes them on real threads. No scheduling logic is copied into the fixture.
Storage, media deletion, alerts, and the map activity are test doubles; GPS
loading returns the real method's documented caught-up/unfinished batch result.
This does not exercise the cache constructor, Android UI, or database integration.

The regression catches the disabled media flag suppressing waits with the map
open. It also checks idle waits without the map, notification wake/re-wait,
immediate retries for unfinished GPS batches, periodic loading of new GPS work,
and waking an idle worker on shutdown. Bounded waits detect hangs; loop counts
and actual `TIMED_WAITING` state detect spinning without CPU or battery claims.
