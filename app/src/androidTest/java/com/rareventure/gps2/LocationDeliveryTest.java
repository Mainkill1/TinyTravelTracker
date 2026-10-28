package com.rareventure.gps2;

import android.Manifest;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import com.rareventure.android.GpsReader;
import com.rareventure.android.ProcessThread;
import java.io.FileInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Exercises the real Android location callback, queue and processing thread. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class LocationDeliveryTest {
    @Test public void registeredGpsDeliversCoordinatesAnd64BitFixTime() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String pkg = context.getPackageName();
        android.app.UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        automation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_COARSE_LOCATION);
        automation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_FINE_LOCATION);
        if (android.os.Build.VERSION.SDK_INT >= 29)
            automation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        try (ParcelFileDescriptor command = automation.executeShellCommand("appops set " + pkg + " android:mock_location allow");
             FileInputStream output = new FileInputStream(command.getFileDescriptor())) {
            byte[] buffer = new byte[1024];
            while (output.read(buffer) != -1) { }
        }
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        manager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false,
                true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
        manager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        CountDownLatch delivered = new CountDownLatch(1);
        double[] coordinates = new double[3];
        long[] time = new long[1];
        GpsReader reader = new GpsReader(null, context, (lon, lat, alt, timestamp) -> {
            coordinates[0] = lon; coordinates[1] = lat; coordinates[2] = alt;
            time[0] = timestamp;
            delivered.countDown();
        }, "GpsPlatformTest", Looper.getMainLooper());
        ProcessThread processor = new ProcessThread(context, "GpsPlatformTest", reader);
        processor.start();
        try {
            reader.turnOn();
            assertTrue(reader.isGpsOn());
            assertTrue(GpsReader.isProviderRegistered());
            Location fix = new Location(LocationManager.GPS_PROVIDER);
            fix.setLatitude(45.123456789);
            fix.setLongitude(-123.987654321);
            fix.setAltitude(123.5);
            fix.setAccuracy(3);
            fix.setTime(1_700_000_000_123L);
            fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            manager.setTestProviderLocation(LocationManager.GPS_PROVIDER, fix);
            assertTrue("Android must deliver a GPS callback", delivered.await(20, TimeUnit.SECONDS));
            assertEquals(-123.987654321, coordinates[0], 0.000000001);
            assertEquals(45.123456789, coordinates[1], 0.000000001);
            assertEquals(123.5, coordinates[2], 0.001);
            assertEquals(1_700_000_000_123L, time[0]);
            assertTrue(GpsReader.getDiagnosticStatus().contains("last GPS fix"));
        } finally {
            reader.turnOff();
            processor.notifyShutdown();
            processor.join(5000);
            manager.removeTestProvider(LocationManager.GPS_PROVIDER);
        }
        assertFalse(reader.isGpsOn());
        assertFalse(GpsReader.isProviderRegistered());
        assertFalse("Processing thread must stop", processor.isAlive());
    }
}
