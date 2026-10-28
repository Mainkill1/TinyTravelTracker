package android.database;
public interface Cursor extends java.io.Closeable { boolean moveToFirst(); int getColumnIndex(String name); String getString(int column); void close(); }
