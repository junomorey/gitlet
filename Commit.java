package gitlet;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import static gitlet.Utils.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class Commit implements Serializable {

    /** Creates a new Commit object given BLOBS, MYPARENT, MSG. */
    Commit(HashMap<String, String> blobs, Commit myParent, String msg, String bran) {
        Date currTime = new Date();
        this.timeStamp = new Timestamp(currTime.getTime());
        timeStamp.setNanos(0);
        time = timeStamp.toString().substring(0 , timeLength);
        this.parent = myParent;
        String filesSha1 = "";
        for (String sha1Key : blobs.keySet()) {
            filesSha1 += sha1Key;
        }
        this.message = msg;
        this.shaCode = sha1(filesSha1, parent.getShaCode(),
                message, time).substring(0, shaCodeLength);
        this.blobsMap = blobs;
        this.branch = bran;
    }

    /** Creates a new commit given a MSG. */
    Commit(String msg) {
        Date currTime = new Date();
        this.timeStamp = new Timestamp(currTime.getTime());
        timeStamp.setNanos(0);
        time = timeStamp.toString().substring(0 , timeLength);
        this.shaCode = sha1(msg, time).substring(0, shaCodeLength);
        this.message = msg;
        this.blobsMap = new HashMap<String, String>();
        this.branch = "master";
    }

    /** Updates the bytes of the current commit. */
    void save() {
        try {
            File commit = new File("./.gitlet/log/" + shaCode + ".ser");
            FileOutputStream fileOut = new FileOutputStream(commit);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(this);
        } catch (IOException e) {
            String msg = "IOException while saving " + shaCode;
            System.out.println(msg);
        }
    }

    /** returns the commit object given its SHA code. */
    public static Commit load(String sha) {
        Commit commitObject = null;
        File commitFile = new File("./.gitlet/log/" + sha);
        if (commitFile.exists()) {
            try {
                FileInputStream fileIn = new FileInputStream(commitFile);
                ObjectInputStream objectIn = new ObjectInputStream(fileIn);
                commitObject = (Commit) objectIn.readObject();
            } catch (IOException e) {
                String msg = "IOException while loading the commit.";
                System.out.println(msg);
            } catch (ClassNotFoundException e) {
                String msg = "ClassNotFoundException while loading the commit.";
                System.out.println(msg);
            }
        }
        return commitObject;
    }

    /** Accessor method for commit's shacode. */
    public String getShaCode() {
        return shaCode;
    }

    /** Accessor method for commit's Branch name. */
    public String getBranchName() {
        return branch;
    }

    /** Accessor method for commit's Parent Commit. */
    public Commit getParent() {
        return parent;
    }

    /** Accessor method for commit's Time. */
    public String getTime() {
        return time;
    }

    /** Accessor method for commit's message. */
    public String getMessage() {
        return message;
    }

    /** Accessor method for this commit's BlobsMap. */
    public HashMap<String, String> getBlobsMap() {
        return blobsMap;
    }

    /** The Timestamp. */
    private Timestamp timeStamp;
    /** The previous commit. */
    private Commit parent;
    /** The specific hashcode of this commit. */
    private String shaCode;
    /** The time that this commit was made. */
    private String message;

    private String branch;
    /** Hashmap of the files that this commit contains. The key is the
     * SHA-1 and the value is the file name. */
    private HashMap<String, String> blobsMap;
    /** My time. */
    private String time;
    /** Limit for time  string length. */
    private final int timeLength = 19;
    /** Limit for sha code length. */
    private final int shaCodeLength = 6;
}
