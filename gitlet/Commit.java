package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class Commit implements Serializable {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");

    private String message;
    private String hashValue;
    private String time;
    private HashMap<String, Blob> blobs;
    private String parent;
    private String mergeParent = null;

    public Commit(String message, HashMap<String, Blob> blobs, String parent) {
        this.message = message;
        this.blobs = blobs;
        this.parent = parent;
        time = simpleDateFormat.format(new Date(System.currentTimeMillis()));
    }

    public static Commit Initial() {
        Commit initial = new Commit("initial commit", new HashMap<>(), null);
        initial.time = simpleDateFormat.format(new Date(0));
        return initial;
    }

    public static Commit Sentinel() {
        Commit sentinel = new Commit("sentinel commit", null, null);
        sentinel.time = simpleDateFormat.format(new Date(0));
        return sentinel;
    }

    public boolean contains(String fileName, String hashValue) {
        return blobs.containsKey(fileName) && blobs.get(fileName).getHashValue().equals(hashValue);
    }

    public String getMessage() {
        return message;
    }

    public String getHashValue() {
        return hashValue;
    }

    public String getTime() {
        return time;
    }

    public HashMap<String, Blob> getBlobs() { return blobs; }

    public String getParent() {
        return parent;
    }

    public String getMergeParent() {
        return mergeParent;
    }

    public void setParent(String parent) { this.parent = parent;}

    public void setMergeParent(String mergeParent) {
        this.mergeParent = mergeParent;
    }

    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }
}
