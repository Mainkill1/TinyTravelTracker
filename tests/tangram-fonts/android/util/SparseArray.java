package android.util;
public class SparseArray<E> {
    private final java.util.TreeMap<Integer,E> values = new java.util.TreeMap<>();
    public int indexOfKey(int key) { return new java.util.ArrayList<>(values.keySet()).indexOf(key); }
    public void put(int key, E value) { values.put(key, value); }
    public E get(int key) { return values.get(key); }
    public int size() { return values.size(); }
    public int keyAt(int index) { return new java.util.ArrayList<>(values.keySet()).get(index); }
    public E valueAt(int index) { return new java.util.ArrayList<>(values.values()).get(index); }
}
