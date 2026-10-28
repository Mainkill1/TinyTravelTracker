package com.rareventure.gps2.gpx;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
public final class PendingBackupRequestTest {
 public static void main(String[] args) {
  PendingBackupRequest original = new PendingBackupRequest("private.gpx.zip", true, "keep this secret");
  check(!original.needsPassword(),"Original request forgot password before picker launch");
  Bundle saved = new Bundle();
  original.save(saved);
  PendingBackupRequest restored = PendingBackupRequest.restore(saved);
  check("private.gpx.zip".equals(restored.filename),"Process recreation lost backup filename");
  check(restored.passwordProtected,"Process recreation silently disabled archive encryption");
  check(restored.password == null && restored.needsPassword(),"Secret password must be re-entered after process recreation");
  Intent result = new Intent("picker").setData(Uri.parse("content://provider/private"));
  check(BackupDocuments.selected(Activity.RESULT_CANCELED,result)==null,"Canceled recreated request accepted an output destination");
  PendingBackupRequest plain = new PendingBackupRequest("plain.zip",false,null);
  Bundle plainState = new Bundle(); plain.save(plainState);
  PendingBackupRequest restoredPlain = PendingBackupRequest.restore(plainState);
  check(!restoredPlain.passwordProtected && !restoredPlain.needsPassword(),"Plain archive unexpectedly requires a password after recreation");
  check(PendingBackupRequest.restore(new Bundle())==null,"An absent request was recreated");
  System.out.println("PendingBackupRequestTest: process recreation, secret omission, plain export and cancellation passed");
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
