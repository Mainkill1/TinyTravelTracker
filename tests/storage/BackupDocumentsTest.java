package com.rareventure.gps2.gpx;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
public final class BackupDocumentsTest {
 public static void main(String[] args) throws Exception {
  Intent create=BackupDocuments.create("route.gpx.zip");
  check(Intent.ACTION_CREATE_DOCUMENT.equals(create.getAction()),"Export must use the create-document picker");
  check(create.hasCategory(Intent.CATEGORY_OPENABLE),"Export destination must accept streams");
  check("application/zip".equals(create.getType()),"Export not offered as ZIP");
  check("route.gpx.zip".equals(create.getStringExtra(Intent.EXTRA_TITLE)),"Suggested name lost");
  Intent open=BackupDocuments.open();
  check(Intent.ACTION_OPEN_DOCUMENT.equals(open.getAction()) && open.hasCategory(Intent.CATEGORY_OPENABLE),"Import cannot read document providers");
  Intent data=new Intent("result").setData(Uri.parse("content://provider/backup"));
  check(BackupDocuments.selected(Activity.RESULT_CANCELED,data)==null,"Canceled picker returned a writable document");
  check(BackupDocuments.selected(Activity.RESULT_OK,null)==null,"Missing result accepted");
  check(BackupDocuments.selected(Activity.RESULT_OK,data)==data.getData(),"Provider URI lost");
  check("trip.gpx".equals(BackupDocuments.entryName("trip.ZIP")),"ZIP without a GPX suffix cannot be restored");
  check("trip.GPX".equals(BackupDocuments.entryName("trip.GPX.ZIP")),"Uppercase ZIP suffix not removed from GPX entry");
  System.out.println("BackupDocumentsTest: 10 checks passed");
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
