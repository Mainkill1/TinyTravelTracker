package com.rareventure.gps2.gpx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.*;
import de.idyl.winzipaes.*;
import de.idyl.winzipaes.impl.*;

/** Exercises the app's actual legacy AES archive implementation across provider streams. */
public final class BackupArchiveRoundTripTest {
    public static void main(String[] args) throws Exception {
        File directory = Files.createTempDirectory("ttt-archive").toFile();
        byte[] gpx = "<?xml version=\"1.0\"?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" version=\"1.1\"><trk><name>旅行</name><trkseg><trkpt lat=\"37.1\" lon=\"-122.2\"><time>2020-01-02T03:04:05.000Z</time></trkpt></trkseg></trk></gpx>".getBytes(StandardCharsets.UTF_8);
        try {
            for (boolean encrypted : new boolean[]{false, true}) {
                File archive = new File(directory, "export.zip");
                if (encrypted) {
                    AesZipFileEncrypter writer = new AesZipFileEncrypter(archive, new AESEncrypterBC());
                    writer.add("route.gpx", new ByteArrayInputStream(gpx), "secret");
                    writer.close();
                } else {
                    try (ZipOutputStream writer = new ZipOutputStream(new FileOutputStream(archive))) {
                        writer.putNextEntry(new ZipEntry("route.gpx"));
                        writer.write(gpx);
                    }
                }
                ByteArrayOutputStream provider = new ByteArrayOutputStream();
                BackupDocumentStreams.publish(archive, provider);
                File imported = BackupDocumentStreams.stage(new ByteArrayInputStream(provider.toByteArray()), directory, "cloud.zip");
                InputStream content;
                AesZipFileDecrypter reader = null;
                if (encrypted) {
                    reader = new AesZipFileDecrypter(imported, new AESDecrypterBC());
                    ExtZipEntry entry = reader.getEntryList().get(0);
                    if (!entry.isEncrypted()) throw new AssertionError("Encryption lost across provider streams");
                    byte[] compressed = reader.extractEntryToZippedByteArray(entry, "secret", new AesZipFileDecrypter.ProgressListener() {
                        public void notifyProgress(int count, int total) { }
                        public boolean isCanceled() { return false; }
                    });
                    CrcIgnoringZipInputStream zip = new CrcIgnoringZipInputStream(new ByteArrayInputStream(compressed));
                    zip.getNextEntry();
                    content = zip;
                } else {
                    ZipInputStream zip = new ZipInputStream(new FileInputStream(imported));
                    zip.getNextEntry();
                    content = zip;
                }
                ByteArrayOutputStream extracted = new ByteArrayOutputStream();
                try (InputStream input = content) {
                    byte[] buffer = new byte[71]; int count;
                    while ((count = input.read(buffer)) != -1) extracted.write(buffer, 0, count);
                }
                GpxReader.validateForRestore(new ByteArrayInputStream(extracted.toByteArray()));
                if (reader != null) reader.close();
                if (!Arrays.equals(gpx, extracted.toByteArray())) throw new AssertionError("GPX contents changed across " + (encrypted ? "encrypted" : "plain") + " document round trip");
                archive.delete(); imported.delete();
            }
            // A .gpx entry name is insufficient: a ZIP can carry unrelated well-formed XML.
            ByteArrayOutputStream misleadingArchive = new ByteArrayOutputStream();
            try (ZipOutputStream writer = new ZipOutputStream(misleadingArchive)) {
                writer.putNextEntry(new ZipEntry("misleading.gpx"));
                writer.write("<notes/>".getBytes(StandardCharsets.UTF_8));
            }
            File misleading = BackupDocumentStreams.stage(new ByteArrayInputStream(misleadingArchive.toByteArray()), directory, "not-gpx.zip");
            try (ZipInputStream input = new ZipInputStream(new FileInputStream(misleading))) {
                input.getNextEntry();
                try { GpxReader.validateForRestore(input); throw new AssertionError("Non-GPX ZIP entry accepted for destructive restore"); }
                catch (org.xml.sax.SAXException expected) { }
            }
            misleading.delete();
            System.out.println("BackupArchiveRoundTripTest: plain and AES encrypted GPX archives passed");
        } finally { for (File file : directory.listFiles()) file.delete(); directory.delete(); }
    }
}
