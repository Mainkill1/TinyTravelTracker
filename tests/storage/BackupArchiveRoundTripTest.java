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
        byte[] gpx = "<?xml version=\"1.0\"?><gpx><trk><name>旅行</name><trkseg><trkpt lat=\"37.1\" lon=\"-122.2\"/></trkseg></trk></gpx>".getBytes(StandardCharsets.UTF_8);
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
                if (reader != null) reader.close();
                if (!Arrays.equals(gpx, extracted.toByteArray())) throw new AssertionError("GPX contents changed across " + (encrypted ? "encrypted" : "plain") + " document round trip");
                archive.delete(); imported.delete();
            }
            System.out.println("BackupArchiveRoundTripTest: plain and AES encrypted GPX archives passed");
        } finally { for (File file : directory.listFiles()) file.delete(); directory.delete(); }
    }
}
