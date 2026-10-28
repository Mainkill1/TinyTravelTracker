package com.rareventure.gps2.gpx;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

/** Provider streams may be short, non-seekable, or fail midway. */
public final class BackupDocumentStreamsTest {
    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("ttt-documents").toFile();
        try {
            byte[] original = new byte[160003];
            for (int i = 0; i < original.length; i++) original[i] = (byte) (i * 37);
            // A short-read provider must not truncate an archive or discard zero-valued bytes.
            InputStream shortReads = new ByteArrayInputStream(original) {
                public synchronized int read(byte[] b, int off, int len) {
                    return super.read(b, off, Math.min(len, 7));
                }
            };
            File staged = BackupDocumentStreams.stage(shortReads, dir, "route.zip");
            require(Arrays.equals(original, Files.readAllBytes(staged.toPath())), "staged archive changed");
            require(staged.getName().endsWith(".zip"), "ZIP extension lost for encrypted archive detection");
            ByteArrayOutputStream destination = new ByteArrayOutputStream();
            BackupDocumentStreams.publish(staged, destination);
            require(Arrays.equals(original, destination.toByteArray()), "published archive changed");
            try {
                BackupDocumentStreams.publish(staged, new OutputStream() {
                    public void write(int value) throws IOException { throw new IOException("destination full"); }
                });
                throw new AssertionError("failed destination reported successful export");
            } catch (IOException expected) { }
            staged.delete();

            InputStream failed = new InputStream() {
                int count;
                public int read() throws IOException { if (++count > 100) throw new IOException("provider failed"); return 3; }
            };
            try {
                BackupDocumentStreams.stage(failed, dir, "route.gpx");
                throw new AssertionError("failed provider accepted");
            } catch (IOException expected) { }
            require(dir.list().length == 0, "failed import left a partial backup in cache");

            InputStream badClose = new ByteArrayInputStream(new byte[]{1}) {
                public void close() throws IOException { throw new IOException("provider close failed"); }
            };
            try {
                BackupDocumentStreams.stage(badClose, dir, "route.zip");
                throw new AssertionError("failed provider close accepted");
            } catch (IOException expected) { }
            require(dir.list().length == 0, "failed provider close retained incomplete import");

            // Creating a new import must never clobber an earlier selection.
            File first = BackupDocumentStreams.stage(new ByteArrayInputStream(new byte[]{1,2,3}), dir, "same.gpx");
            File second = BackupDocumentStreams.stage(new ByteArrayInputStream(new byte[]{9}), dir, "same.gpx");
            require(!first.equals(second), "duplicate display names overwrite a prior backup");
            require(Arrays.equals(new byte[]{1,2,3}, Files.readAllBytes(first.toPath())), "previous backup overwritten");
            first.delete(); second.delete();
            System.out.println("BackupDocumentStreamsTest: 9 checks passed");
        } finally { for (File f : dir.listFiles()) f.delete(); dir.delete(); }
    }
    static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
