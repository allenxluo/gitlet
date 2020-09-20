package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {

    private File fileName;
    private String hashValue;

    public Blob(File fileName, String hashValue) {
        this.fileName = fileName;
        this.hashValue = hashValue;
    }

    public String getHashValue() {
        return hashValue;
    }
}
