package android.content;
import android.net.Uri;
import java.util.*;
public class Intent {
 public static final String ACTION_CREATE_DOCUMENT="android.intent.action.CREATE_DOCUMENT", ACTION_OPEN_DOCUMENT="android.intent.action.OPEN_DOCUMENT", CATEGORY_OPENABLE="android.intent.category.OPENABLE", EXTRA_TITLE="android.intent.extra.TITLE";
 private String action, type; private Uri data; private final Map<String,String> extras=new HashMap<>(); private final Set<String> categories=new HashSet<>();
 public Intent(String action) { this.action=action; } public String getAction(){return action;} public Intent addCategory(String c){categories.add(c);return this;} public boolean hasCategory(String c){return categories.contains(c);} public Intent setType(String t){type=t;return this;} public String getType(){return type;} public Intent putExtra(String k,String v){extras.put(k,v);return this;} public String getStringExtra(String k){return extras.get(k);} public Intent setData(Uri u){data=u;return this;} public Uri getData(){return data;}
}
