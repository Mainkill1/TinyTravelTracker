package android.content;
public class Context { public static final String VIBRATOR_SERVICE="vibrator"; public Object getSystemService(String s) { return new android.os.Vibrator(); } public android.content.res.AssetManager getAssets() { return new android.content.res.AssetManager(); } public android.content.res.Resources getResources() { return new android.content.res.Resources(); } }
