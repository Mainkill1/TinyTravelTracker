package com.mapzen.tangram;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Checks the rebuilt JNI boundary; full GL rendering still needs a device check. */
@RunWith(AndroidJUnit4.class)
public class NativeMapTest {
    @Test public void rebuiltLibraryCreatesAndDisposesANativeMap() {
        System.loadLibrary("tangram");
        MapController controller = new MapController(InstrumentationRegistry.getInstrumentation().getTargetContext());
        try {
            CameraPosition camera = controller.getCameraPosition();
            assertNotNull(camera);
            assertFalse(Float.isNaN(camera.zoom));
            assertFalse(Float.isInfinite(camera.zoom));
        } finally {
            controller.dispose();
        }
    }
}
