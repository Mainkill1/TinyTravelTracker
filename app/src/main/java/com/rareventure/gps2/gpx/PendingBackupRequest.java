package com.rareventure.gps2.gpx;

import android.os.Bundle;

/** Metadata survives a document-picker process restart; the password stays in memory only. */
final class PendingBackupRequest {
    private static final String NAME = "backup.pending.name";
    private static final String ENCRYPTED = "backup.pending.encrypted";
    final String filename;
    final boolean passwordProtected;
    final String password;

    PendingBackupRequest(String filename, boolean passwordProtected, String password) {
        this.filename = filename;
        this.passwordProtected = passwordProtected;
        this.password = password;
    }

    boolean needsPassword() { return passwordProtected && password == null; }

    void save(Bundle state) {
        state.putString(NAME, filename);
        state.putBoolean(ENCRYPTED, passwordProtected);
    }

    static PendingBackupRequest restore(Bundle state) {
        if (state == null || state.getString(NAME) == null) return null;
        return new PendingBackupRequest(state.getString(NAME), state.getBoolean(ENCRYPTED), null);
    }
}
