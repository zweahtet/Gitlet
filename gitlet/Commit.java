package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.TreeMap;

/**
 * Represents a commit object.
 * @author Zwea Htet
 */
public class Commit implements Serializable {
    /** Date for a commit. */
    private Date _date;

    /** Author name. */
    private String _author;

    /** Commit message. */
    private String _message;

    /** Treemap for mapping file names to blob references. */
    private TreeMap<String, String> _fileNames;

    /** Pointers as string to first parent and second parent commits. */
    private String _firstParent, _secondParent;

    /** Commit Id. */
    private String _id;

    /** Commit constructor.
     * @param msg String
     * @param firstParent String
     * @param secondParent String
     */
    public Commit(String msg, String firstParent, String secondParent) {
        if (this._firstParent == null) {
            this._date = new Date(0);
        } else {
            this._date = new Date();
        }
        this._message = msg;
        this._fileNames = new TreeMap<>();
        this._firstParent = firstParent;
        this._secondParent = secondParent;
        this._id = "";
    }

    /** Clone a new Commit.
     * @param commit0 Commit object
     */
    public Commit(Commit commit0) {
        this._message = commit0._message;
        this._date = new Date();
        this._firstParent = commit0._firstParent;
        this._secondParent = commit0._secondParent;
        this._fileNames = new TreeMap<>();
        this._fileNames.putAll(commit0._fileNames);
        this._id = "";
    }

    /** Return a string message of the current commit. */
    public String getMessage() {
        return this._message;
    }

    /** Return a string representation of the date. */
    public String getDate() {
        return String.format("%1$ta %1$tb %1$te %1$tT %1$tY %1$tz", this._date);
    }

    /** Return the pointer of string representation
     * to first parent of the current commit. */
    public String getFirstParent() {
        return this._firstParent;
    }

    /** Return the pointer of string representation
     * to first parent of the current commit. */
    public String getSecondParent() {
        return this._secondParent;
    }

    /** Return blob reference from mapping of filenames to blob refs.
     * @param fileName String representing the name of the file.
     */
    public String getBlobHash(String fileName) {
        return this._fileNames.get(fileName);
    }

    /** Return file references.
     * @return Treemap representing the mapping of file names
     * to blob references.
     */
    public TreeMap<String, String> getFileNames() {
        return this._fileNames;
    }

    /** Return a commit id. */
    public String getCommitId() {
        return this._id;
    }

    /** Update mapping of filenames to blob references.
     * @param fileName String representing the name of the file.
     * @param blobHash String representing the Sha-1 id of the file contents.
     */
    public void track(String fileName, String blobHash) {
        this._fileNames.put(fileName, blobHash);
    }

    /** Set commit message.
     * @param msg String representing a commit message.
     * */
    public void setMsg(String msg) {
        this._message = msg;
    }

    /** Set commit timestamp. */
    public void setDate() {
        this._date = new Date();
    }

    /** Set first parent hash.
     * @param parentHash String representing the Sha-1 id of first parent.
     * */
    public void setFirstParent(String parentHash) {
        this._firstParent = parentHash;
    }

    /** Set second parent hash.
     * @param parentHash String representing the Sha-1 id of second parent.
     * */
    public void setSecondParent(String parentHash) {
        this._secondParent = parentHash;
    }

    /** Create a commit hash from a commit object.
     */
    public void createCommitId() {
        this._id = Utils.sha1(Utils.serialize(this));
    }

    /** Untrack files that are staged for removal.
     * @param filename String representing the name of the file.
     */
    public void untrack(String filename) {
        this._fileNames.remove(filename);
    }

    /** Check if a file is tracked in the commit.
     * @param filename String representing the name of the file.
     * @return true if file is tracked in the current commit.
     * */
    public boolean isTracked(String filename) {
        return this._fileNames.containsKey(filename);
    }
}
