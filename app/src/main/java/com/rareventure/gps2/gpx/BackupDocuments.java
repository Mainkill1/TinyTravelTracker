package com.rareventure.gps2.gpx;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

/** System document pickers grant access to the selected file without storage permissions. */
public final class BackupDocuments {
    private BackupDocuments() { }

    public static Intent create(String filename) {
        return new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/zip").putExtra(Intent.EXTRA_TITLE, filename);
    }

    public static String entryName(String filename) {
        String name = filename.replaceFirst("(?i)\\.zip$", "");
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".gpx") ? name : name + ".gpx";
    }

    public static Intent open() {
        // GPX is served under several MIME types by local and cloud providers.
        return new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
    }

    public static Uri selected(int resultCode, Intent result) {
        return resultCode == Activity.RESULT_OK && result != null ? result.getData() : null;
    }

    public static String displayName(ContentResolver resolver, Uri uri) {
        try (Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0 && cursor.getString(column) != null) return cursor.getString(column);
            }
        } catch (RuntimeException ignored) {
            // Some providers omit metadata but still allow stream access.
        }
        return "backup.gpx";
    }
}
