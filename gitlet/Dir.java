package gitlet;

import java.io.File;

/**
 * represents files system.
 * @author Zwea Htet
 */
public class Dir {
    /** Current Working Directory. */
    static final File CWD = new File(".");

    /** Main metadata folder. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

    /** All commits folder. */
    static final File COMMITS_DIR = Utils.join(GITLET_FOLDER, "commits");

    /** All blobs folder. */
    static final File BLOBS_DIR = Utils.join(GITLET_FOLDER, "blobs");

    /** All branches folder. */
    static final File BRANCHES = Utils.join(GITLET_FOLDER, "branches");

    /** Store Master branch pointer. */
    static final File MASTER = Utils.join(BRANCHES, "master");

    /** Store current HEAD branch pointer. */
    static final File HEAD = Utils.join(GITLET_FOLDER, "HEAD.txt");

    /** Staging Area. */
    static final File STAGING_AREA = Utils.join(GITLET_FOLDER, "staging");
}
