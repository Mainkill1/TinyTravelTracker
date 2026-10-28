package android.content;
import android.net.Uri;
import android.database.Cursor;
public abstract class ContentResolver { public abstract Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort); }
