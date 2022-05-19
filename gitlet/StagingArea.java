package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

import static gitlet.Dir.STAGING_AREA;

/**
 * Represents a staging area.
 * @author Zwea Htet
 */
public class StagingArea implements Serializable {
    /** Staged for addition (Key: filename, Value: blobHash). */
    private TreeMap<String, String> addition;

    /** Staged for removal (filenames). */
    private ArrayList<String> removal;

    /** Constructor. */
    public StagingArea() {
        this.addition = new TreeMap<>();
        this.removal = new ArrayList<>();
    }

    /** Update Staging Area (Addition).
     * @param filename String to represent the name of the file.
     * @param blobHash String to represent the Sha-1 id of the blob.
     * */
    public void stageAddition(String filename, String blobHash) {
        this.addition.put(filename, blobHash);
    }

    /** Check for filename in staging area.
     * @param filename String representing the name of the file.
     * @return true if addition area contains the filename.
     * */
    public boolean checkAddition(String filename) {
        return this.addition.containsKey(filename);
    }

    /** Remove from staging area.
     * @param filename String to represent the name of the file.
     * */
    public void unstageAddition(String filename) {
        this.addition.remove(filename);
    }
    /** Update Staging Area (Removal).
     * @param filename String to represent the name of the file.
     * */
    public void stageRemoval(String filename) {
        this.removal.add(filename);
    }

    /** Remove from staging area.
     * @param filename String to represent the name of the file.
     * */
    public void unstageRemoval(String filename) {
        this.removal.remove(filename);
    }


    /** Check if staging area is emtpy.
     * @param which String to represent the staging area.
     * @return true if the staging area (addition/removal) is empty.
     * */
    public boolean isEmpty(String which) {
        if (which.equals("addition")) {
            return this.addition.isEmpty();
        }
        return this.removal.isEmpty();
    }

    /** Convert addition treemap to set object to be iterable.
     * @return addition area represented as Set.
     * */
    public TreeMap<String, String> getAddition() {
        return this.addition;
    }

    /** Get removal area.
     * @return removal represented as ArrayList.
     * */
    public ArrayList<String> getRemoval() {
        return this.removal;
    }

    /** Clear staging area.
     * @param which a string representing which area to be cleared.
     * */
    public void clear(String which) {
        this.addition.clear();
        Utils.writeObject(Utils.join(STAGING_AREA, which), new StagingArea());
    }
}
