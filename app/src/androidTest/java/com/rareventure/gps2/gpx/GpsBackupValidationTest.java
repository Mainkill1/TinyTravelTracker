package com.rareventure.gps2.gpx;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.fail;

/** Uses Android's real SAX/Expat implementation, rather than the host JDK parser. */
@RunWith(AndroidJUnit4.class)
public class GpsBackupValidationTest {
    private static final String ROOT = "<gpx xmlns='http://www.topografix.com/GPX/1/1' version='1.1'>";
    private static final String POINT = "<trk><trkseg><trkpt lat='37.1' lon='-122.2'><time>2020-01-02T03:04:05.000Z</time></trkpt></trkseg></trk>";

    @Test public void validGpx11And10PassOnAndroid() throws Exception {
        validate(ROOT + POINT + "</gpx>");
        validate("<gpx xmlns='http://www.topografix.com/GPX/1/0' version='1.0'>" + POINT + "</gpx>");
    }

    @Test public void unrelatedXmlCannotReplaceHistory() throws Exception {
        rejected("<notes/>");
        rejected("<svg xmlns='http://www.w3.org/2000/svg'/>");
        rejected("<notes>" + ROOT + POINT + "</gpx></notes>");
        rejected(ROOT + ROOT + POINT + "</gpx></gpx>");
    }

    @Test public void emptyOrMalformedGpxCannotReplaceHistory() throws Exception {
        rejected(ROOT + "</gpx>");
        rejected(ROOT + POINT);
        rejected("<gpx xmlns='http://www.topografix.com/GPX/1/1' version='1.0'>" + POINT + "</gpx>");
    }

    @Test public void dtdAndExternalEntitiesAreRejectedByAndroidExpat() throws Exception {
        rejected("<!DOCTYPE gpx>" + ROOT + POINT + "</gpx>");
        rejected("<!DOCTYPE gpx [<!ENTITY note 'unexpected'>]>" + ROOT
                + "<metadata><name>&note;</name></metadata>" + POINT + "</gpx>");
        rejected("<!DOCTYPE gpx [<!ENTITY secret SYSTEM 'file:///proc/self/cmdline'>]>" + ROOT
                + "<metadata><name>&secret;</name></metadata>" + POINT + "</gpx>");
    }

    private static void validate(String xml) throws Exception {
        try (ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            GpxReader.validateForRestore(input);
        }
    }

    private static void rejected(String xml) throws Exception {
        try { validate(xml); fail("Unsafe GPX restore input was accepted"); }
        catch (SAXException expected) { }
    }
}
