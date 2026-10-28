package android.util;
public class Log {
 public static int warnings, errors;
 public static int w(String tag, String message) { warnings++; return 0; }
 public static int e(String tag, String message) { errors++; return 0; }
 public static int e(String tag, String message, Throwable error) { errors++; return 0; }
}
