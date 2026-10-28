package com.rareventure.gps2.gpx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;
import org.xml.sax.SAXException;

/** Real parser tests: unrelated XML must never be accepted as a replacement for recorded history. */
public final class GpxValidationTest {
    static final String START = "<gpx xmlns='http://www.topografix.com/GPX/1/1' version='1.1'>";
    static final String POINT = "<trk><trkseg><trkpt lat='37' lon='-122'><time>2020-01-02T03:04:05Z</time></trkpt></trkseg></trk>";
    static final GpxReader.GpxReaderCallback NOOP = new GpxReader.GpxReaderCallback() {
        public void readGpx() {} public void readTrk() {} public void readTrkSeg() {}
        public void readTrkPt(double lon,double lat,double altitude,long time,TimeZone tz) {}
    };
    public static void main(String[] args) throws Exception {
        String[] invalid = {
            "<!DOCTYPE gpx>"+START+POINT+"</gpx>",
            "<notes/>", "<svg xmlns='http://www.w3.org/2000/svg'/>",
            "<notes>"+START+POINT+"</gpx></notes>",
            "<gpx xmlns='http://example.invalid/fake' version='1.1'>"+POINT+"</gpx>",
            "<gpx xmlns='http://www.topografix.com/GPX/1/1' version='1.0'>"+POINT+"</gpx>",
            START+START+POINT+"</gpx></gpx>", START+POINT,
            "<!DOCTYPE gpx [<!ENTITY bad SYSTEM 'file:///nonexistent-secret'>]>"+START+"<metadata><name>&bad;</name></metadata>"+POINT+"</gpx>"
        };
        for (String xml : invalid) {
            try { new GpxReader(NOOP).doIt(input(xml)); throw new AssertionError("Non-GPX document accepted: "+xml); }
            catch (SAXException expected) { }
        }
        final int[] points={0};
        new GpxReader(new GpxReader.GpxReaderCallback() {
            public void readGpx() {} public void readTrk() {} public void readTrkSeg() {}
            public void readTrkPt(double lon,double lat,double alt,long time,TimeZone tz) {
                if (lat != 37 || lon != -122 || time != 1577934245000L) throw new AssertionError("Valid point changed");
                points[0]++;
            }
        }).doIt(input(START+POINT+"</gpx>"));
        if(points[0]!=1)throw new AssertionError("Valid GPX point lost");
        // Parser recognizes an empty GPX document; restore preflight must explicitly reject replacing history with it.
        new GpxReader(NOOP).doIt(input(START+"</gpx>"));
        String[] nonRestorable = {"<notes/>", START+"</gpx>",
                START+"<trk><trkseg><trkpt lat='999' lon='10'><time>2020-01-02T03:04:05Z</time></trkpt></trkseg></trk></gpx>"};
        for (String xml : nonRestorable) {
            try { GpxReader.validateForRestore(input(xml)); throw new AssertionError("Empty or non-GPX restore would erase current history"); }
            catch (SAXException expected) { }
        }
        GpxReader.validateForRestore(input(START+POINT+"</gpx>"));
        final RuntimeException cancellation = new RuntimeException("cancel preflight");
        try {
            GpxReader.validateForRestore(input(START+POINT+"</gpx>"), () -> { throw cancellation; });
            throw new AssertionError("Restore preflight ignored cancellation");
        } catch (RuntimeException expected) { if (expected != cancellation) throw expected; }
        GpxReader.validateForRestore(input("<gpx xmlns='http://www.topografix.com/GPX/1/0' version='1.0'>"+POINT+"</gpx>"));
        System.out.println("GpxValidationTest: unrelated XML, fake/nested roots, namespace/version mismatch, malformed GPX and valid point passed");
    }
    static InputStream input(String xml) { return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)); }
}
