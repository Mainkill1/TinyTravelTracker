package com.rareventure.gps2.gpx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Stages non-seekable document-provider streams for the random-access ZIP reader. */
public final class BackupDocumentStreams {
    private BackupDocumentStreams() { }

    /** Takes ownership of the provider stream; incomplete imports are never retained. */
    public static File stage(InputStream source, File cacheDirectory, String displayName) throws IOException {
        if (source == null) throw new IOException("Document provider returned no input stream");
        File staged = null;
        boolean complete = false;
        try {
            try (InputStream input = source) {
                String suffix = displayName != null && displayName.toLowerCase(java.util.Locale.ROOT).endsWith(".zip") ? ".zip" : ".gpx";
                staged = File.createTempFile("gpx-import-", suffix, cacheDirectory);
                try (OutputStream output = new FileOutputStream(staged)) {
                    copy(input, output);
                }
            }
            complete = true;
            return staged;
        } finally {
            if (!complete && staged != null) staged.delete();
        }
    }

    /** Publishes only a finalized local archive, and closes the provider stream. */
    public static void publish(File archive, OutputStream destination) throws IOException {
        publish(archive, destination, new BackupCancellation());
    }

    static void publish(File archive, OutputStream destination, BackupCancellation cancellation) throws IOException {
        if (destination == null) throw new IOException("Document provider returned no output stream");
        try (OutputStream output = destination; InputStream input = new FileInputStream(archive)) {
            cancellation.register(output);
            copy(input, output, cancellation);
        } finally {
            cancellation.unregister(destination);
        }
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        copy(input, output, new BackupCancellation());
    }

    private static void copy(InputStream input, OutputStream output, BackupCancellation cancellation) throws IOException {
        byte[] buffer = new byte[65536];
        while (true) {
            cancellation.check();
            int count = input.read(buffer);
            if (count == -1) break;
            cancellation.check();
            output.write(buffer, 0, count);
        }
        cancellation.check();
    }
}
