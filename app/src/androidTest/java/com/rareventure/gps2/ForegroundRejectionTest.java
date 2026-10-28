package com.rareventure.gps2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import androidx.core.content.ContextCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ForegroundRejectionTest {
    @Test public void acceptedForegroundStartWithoutPermissionStopsWithoutKillingApp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Run on a fresh emulator before LocationDeliveryTest grants permission.
        Assume.assumeTrue(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED);
        String command = "am start-foreground-service -n " + context.getPackageName()
                + "/com.rareventure.gps2.GpsTrailerService";
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (ParcelFileDescriptor descriptor = InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation().executeShellCommand(command);
             FileInputStream output = new FileInputStream(descriptor.getFileDescriptor())) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = output.read(buffer)) != -1) result.write(buffer, 0, count);
        }
        assertFalse(result.toString("UTF-8"), result.toString("UTF-8").contains("Error"));
        // A pending foreground-start obligation can crash the process after stopSelf().
        // Give Android time to deliver that delayed failure; a process kill fails instrumentation.
        Thread.sleep(12_000);
        assertNull("Rejected recording must not leave a collector alive", GTG.service);
    }
}
