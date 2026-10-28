Run `scripts/test-map-distance-scale.sh` with JDK 17 or newer. The test compiles
and executes the actual `OsmMapView.java`, with small Android/Tangram/UI boundary
stubs. The production scale method and `updateScaleWidget` path remain intact.
No Android SDK, Gradle dependency resolution, or downloaded test code is needed.

Fixtures use the existing equatorial conversion (111131.75 meters per degree),
then independently check the east-west ground distances at 0, 45, and +/-60
degrees. The viewport's center latitude differs from its top edge. A longitude
arc at 60 degrees is half as long as at the equator, so the previous reciprocal
cosine made meters-per-pixel four times too large; at 45 degrees it doubled it.
The test also checks the reciprocal passed to the scale widget and proportional
changes in span/viewport width.

This fixes the cosine direction while preserving the app's existing local,
spherical ground-distance approximation and equatorial constant. No ellipsoidal
geodesic or tilted-camera change is included. On-device follow-up should compare
the drawn scale against a known distance at low and high latitudes.
