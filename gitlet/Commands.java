package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Formatter;
import java.util.Set;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gitlet.Dir.*;

/**
 * This class represents all the commands supported by our Gitlet version.
 * @author Zwea Htet
 */
public class Commands {
    /**
     * Does required filesystem operations to allow for persistence.
     * (creates any necessary folders or files)
     * create new gitlet VCS (create a .gitlet folder)
     * create all necessary files in our .gitlet directory
     *
     * Current File structure:
     *
     * .gitlet/ -- top level folder for all persistent data
     *    - commits/ -- folder containing all of the persistent data for commit
     *    - stagingArea/ -- folder containing
     *    - blobs/ -- folder containing all file contents
     */
    public static void setupPersistence() throws IOException {
        GITLET_FOLDER.mkdir();
        COMMITS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        BRANCHES.mkdir();
        HEAD.createNewFile();
    }

    /**
     * Usage: java gitlet.Main init
     *
     * Description: Creates a new Gitlet version-control system in the
     * current directory. This system will automatically start with one
     * commit: a commit that contains no files and has the commit message
     * initial commit (just like that, with no punctuation). It will have
     * a single branch: master, which initially points to this initial
     * commit, and master will be the current branch. The timestamp for
     * this initial commit will be 00:00:00 UTC, Thursday, 1 January 1970
     * in whatever format you choose for dates (this is called "The (Unix)
     * Epoch", represented internally by the time 0.) Since the initial
     * commit in all repositories created by Gitlet will have exactly the
     * same content, it follows that all repositories will automatically
     * share this commit (they will all have the same UID) and all commits
     * in all repositories will trace back to it.
     * @param args no argument
     * */
    public static void init(String[] args) throws IOException {
        validateNumArgs(args, 1, 1);
        if (GITLET_FOLDER.exists()) {
            throw Utils.error("A Gitlet version-control "
                        + "system already exists in the current directory.");
        }
        setupPersistence();

        Utils.writeContents(HEAD, MASTER.getPath());

        StagingArea stagingArea = new StagingArea();
        Utils.writeObject(Utils.join(GITLET_FOLDER, "staging"), stagingArea);

        Commit initial = new Commit("initial commit",
                null, null);

        initial.createCommitId();
        String commitHash = initial.getCommitId();
        Utils.writeObject(Utils.join(COMMITS_DIR, commitHash), initial);
        Utils.writeContents(Utils.join(BRANCHES, "master"), commitHash);
    }

    /**
     * Usage: java gitlet.Main add [file name].
     * @param args Array in format: {'add', filename}
     */
    public static void add(String[] args) {
        validateNumArgs(args, 2, 2);
        String filename = args[1];
        if (!Utils.join(CWD, filename).exists()) {
            throw Utils.error("File does not exist.");
        }

        StagingArea stagingArea = Utils.readObject(STAGING_AREA,
                StagingArea.class);

        byte[] newBlob = Utils.readContents(Utils.join(CWD, filename));
        String newBlobHash = Utils.sha1(newBlob);

        String headHash = readHeadId();
        Commit head = makeCommit(headHash);

        String currentBlobHash =  head.getBlobHash(filename);
        if (newBlobHash.equals(currentBlobHash)) {
            stagingArea.unstageAddition(filename);
            stagingArea.unstageRemoval(filename);
        } else {
            stagingArea.stageAddition(filename, newBlobHash);
            Utils.writeContents(Utils.join(BLOBS_DIR, newBlobHash), newBlob);
        }

        Utils.writeObject(STAGING_AREA, stagingArea);
    }

    /**
     *  Note:
     *  1. never adds, changes, or removes files in the working directory.
     *  2. Any changes made to files after staging for addition or removal
     *  are ignored by the commit commandstill track files in commits if
     *  you remove a tracked file using the Unix rm command (rather than
     *  Gitlet's command of the same name).
     *  3. contains the date and time it was made.
     *  4. each commit is identified by its SHA-1 id (include file references,
     *  parent reference, log message, and commit time).
     * Usage: java gitlet.Main commit [message]
     * @param args Array in format: {'commit', message}
     *
     */
    public static void commit(String[] args) {
        validateNumArgs(args, 2, 2);
        String msg = args[1];
        if (msg.equals("")) {
            throw Utils.error("Please enter a commit message.");
        }

        StagingArea stagingArea = Utils.readObject(STAGING_AREA,
                StagingArea.class);

        if (stagingArea.isEmpty("addition")
                && stagingArea.isEmpty("removal")) {
            throw Utils.error("No changes added to the commit.");
        }

        String headHash = readHeadId();
        Commit head = makeCommit(headHash);

        Commit newCommit = new Commit(head);

        newCommit.setMsg(msg);

        for (Map.Entry<String, String> entry: stagingArea
                .getAddition().entrySet()) {
            newCommit.track(entry.getKey(), entry.getValue());
        }

        for (String filename: stagingArea.getRemoval()) {
            newCommit.untrack(filename);
        }

        newCommit.setFirstParent(headHash);

        newCommit.createCommitId();
        String newCommitId = newCommit.getCommitId();
        modifyHeadId(newCommitId);

        Utils.writeObject(Utils.join(COMMITS_DIR, newCommitId), newCommit);

        Utils.writeObject(STAGING_AREA, new StagingArea());
    }

    /**
     * The rm command will remove such files, as well as
     * staging them for removal, so that they will be untracked after a commit.
     * @param args Array in format: {'commit', message}
     */
    public static void remove(String[] args) {
        validateNumArgs(args, 2, 2);
        String filename = args[1];

        StagingArea stagingArea = Utils.readObject(STAGING_AREA,
                StagingArea.class);
        boolean isStaged = false, isTracked = false;

        String currentHash = readHeadId();
        Commit currentCommit = makeCommit(currentHash);

        if (stagingArea.checkAddition(filename)) {
            stagingArea.unstageAddition(filename);
            isStaged = true;
        }
        if (currentCommit.isTracked(filename)) {
            stagingArea.stageRemoval(filename);
            Utils.restrictedDelete(Utils.join(CWD, filename));
            isTracked = true;
        }
        if (!isStaged && !isTracked) {
            throw Utils.error("No reason to remove the file.");
        }

        Utils.writeObject(STAGING_AREA, stagingArea);
    }

    /**
     * Display a description of each commit starting from the most
     * recent commit to initial commit in the following format.
     *
     * >>> ===
     * >>> commit e881c9575d180a215d1a636545b8fd9abfb1d2bb
     * >>> Date: Wed Dec 31 16:00:00 1969 -0800
     * >>> initial commit
     *
     * @param args Array in format: {'log'}
     */
    public static void log(String[] args) {
        validateNumArgs(args, 1, 1);
        Formatter formatter = new Formatter();
        String hash = readHeadId();
        Commit pointer = makeCommit(hash);

        logHelper(formatter, pointer);
        System.out.println(formatter);
    }

    /**
     * displays information about all commits ever made.
     * Usage: java gitlet.Main global-log
     * @param args Array in format: {'global-log'}
     */
    public static void globalLog(String[] args) {
        validateNumArgs(args, 1, 1);
        List<String> commitIds = Utils.plainFilenamesIn(COMMITS_DIR);
        Formatter formatter = new Formatter();

        for (String id : commitIds) {
            Commit pointer = makeCommit(id);
            logHelper(formatter, pointer);
        }
        System.out.println(formatter);
    }

    /**
     * Helper method for log and global-log methods.
     * @param formatter Formatter
     * @param pointer Commit object that points to most recent commit in
     *                a branch.
     */
    private static void logHelper(Formatter formatter, Commit pointer) {
        while (pointer != null) {
            formatter.format("=== \n"
                            + "commit %s\n"
                            + "Date: %s\n"
                            + "%s\n\n",
                    pointer.getCommitId(),
                    pointer.getDate(),
                    pointer.getMessage());
            String firstParent = pointer.getFirstParent();
            pointer = firstParent != null ? makeCommit(firstParent) : null;
        }
    }

    /**
     * Usages:
     * 1. java gitlet.Main checkout -- [file name]
     * 2. java gitlet.Main checkout [commit id] -- [file name]
     * 3. java gitlet.Main checkout [branch name]
     * @param args Array in format: {'commit', message}
     */
    public static void checkout(String[] args) {
        String fileName = "", commitId = "", branch = "";
        validateFormat(args);
        if (args.length == 3) {
            fileName = args[2];
        } else if (args.length == 4) {
            commitId = args[1];
            fileName = args[3];
        } else if (args.length == 2) {
            branch = args[1];
        }

        String currentBranch = readHeadId();
        Commit current = makeCommit(currentBranch);
        Commit pointer = current;
        if (fileName != "") {
            if (args.length == 4) {
                while (pointer != null && !pointer.getCommitId()
                        .equals(commitId)) {
                    String firstParent = pointer.getFirstParent();
                    pointer = firstParent != null
                            ? makeCommit(firstParent) : null;
                }
                if (pointer == null) {
                    throw Utils.error("No commit with that id exists.");
                }
            }
            if (!pointer.isTracked(fileName)) {
                throw Utils.error("File does not exist in that commit.");
            } else if (!current.isTracked(fileName)) {
                throw Utils.error("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }

            byte[] content = Utils.readContents(Utils.join(BLOBS_DIR,
                    pointer.getBlobHash(fileName)));
            Utils.writeContents(Utils.join(CWD, fileName), content);
        } else {
            if (!Utils.join(BRANCHES, branch).exists()) {
                throw Utils.error("No such branch exists.");
            }

            String checkedBranch = Utils
                    .readContentsAsString(Utils.join(BRANCHES, branch));
            Commit checkedCommit = makeCommit(checkedBranch);

            if (currentBranch.equals(checkedBranch)) {
                throw Utils.error("No need to checkout "
                        +  "the current branch.");
            } else {
                checkUntrackedFiles(current, checkedCommit);
            }

            updateCWD(current, checkedCommit);

            moveHeadTo(branch);

            clearStagingArea();
        }
    }

    private static void validateFormat(String[] args) {
        String str = String.join(" ", args);
        Pattern p = null;
        Matcher m;
        if (args.length == 3) {
            p = Pattern.compile("^(checkout)\\s(--)\\s(\\w+.\\w+)");
        } else if (args.length == 4) {
            p = Pattern.compile("^(checkout)\\s(\\w+)\\s(--)\\s(\\w+.\\w+)");
        } else if (args.length == 2) {
            p = Pattern.compile("^(checkout)\\s(\\w+)");
        }

        m = p.matcher(str);
        if (!m.matches()) {
            throw Utils.error("Incorrect operands.");
        }
    }

    /**
     * Prints out the ids of all commits that have given commit
     * message.
     * Usage: java gitlet.Main find [commit message]
     * @param args Array in format: {'find', message}
     */
    public static void find(String[] args) {
        validateNumArgs(args, 2, 2);
        String msg = args[1];
        int count = 0;
        List<String> commidIds = Utils.plainFilenamesIn(COMMITS_DIR);
        Formatter formatter = new Formatter();
        for (String id: commidIds) {
            Commit pointer = makeCommit(id);
            if (msg.equals(pointer.getMessage())) {
                formatter.format("%s\n", id);
                count += 1;
            }
        }

        if (count > 0) {
            System.out.println(formatter);
        } else {
            throw Utils.error("Found no commit with that message.");
        }
    }

    /**
     * Usage: java gitlet.Main status.
     * @param args Array in format {'status'}
     */
    public static void status(String[] args) {
        validateNumArgs(args, 1, 1);
        List<String> branches = Utils.plainFilenamesIn(BRANCHES);
        Collections.sort(branches);
        String b = "";
        String pathToHead = Utils.readContentsAsString(HEAD);

        for (String branch: branches) {
            if (Utils.join(BRANCHES, branch).toString().equals(pathToHead)) {
                b += String.format("*%s\n", branch);
            } else {
                b += String.format("%s\n", branch);
            }
        }

        StagingArea stagingArea = Utils.readObject(STAGING_AREA,
                StagingArea.class);
        Set<String> stagedFiles = stagingArea.getAddition().keySet();
        String s = "";
        for (String filename: stagedFiles) {
            s += String.format("%s\n", filename);
        }

        ArrayList<String> removedFiles = stagingArea.getRemoval();
        String r = "";
        for (String filename: removedFiles) {
            r += String.format("%s\n", filename);
        }

        String headId = readHeadId();
        Commit head = makeCommit(headId);
        List<String> files = Utils.plainFilenamesIn(CWD);

        String m = "";

        String u = "";
        for (String file: files) {
            if (!head.isTracked(file)) {
                if (!stagingArea.checkAddition(file)) {
                    u += String.format("%s\n", file);
                }
            } else {
                if (!stagingArea.checkAddition(file)) {
                    m += String.format("%s (modified)\n", file);
                } else if () {
                    m += String.format("%s (deleted)\n", file);
                }
            }
        }

        Formatter formatter = new Formatter();
        formatter.format("=== Branches ===\n"
                        + "%s\n"
                        + "=== Staged Files ===\n"
                        + "%s\n"
                        + "=== Removed Files ===\n"
                        + "%s\n"
                        + "=== Modifications Not Staged For Commit ===\n"
                        + "%s\n"
                        + "=== Untracked Files ===\n"
                        + "%s\n",
                b, s, r, m, u);
        System.out.println(formatter);
    }

    /**
     * Create a new branch with the given name, and points it
     * at the current head node.
     * Usage: java gitlet.Main branch [branch name]
     * @param args Array in format: {'branch', branch name}
     */
    public static void branch(String[] args) {
        validateNumArgs(args, 2, 2);
        String branchName = args[1];

        if (Utils.join(BRANCHES, branchName).exists()) {
            throw Utils.error("A branch with that "
                    + "name already exists.");
        }

        String headPointer = readHeadId();
        Utils.writeContents(Utils.join(BRANCHES,
                branchName), headPointer);
    }

    /**
     * Usage: java gitlet.Main rm-branch [branch name].
     * @param args Array in format: {'rm-branch', branch name}
     */
    public static void removeBranch(String[] args) {
        validateNumArgs(args, 2, 2);
        String name = args[1];
        String currentBranch = readHeadId();
        String b = Utils
                .readContentsAsString(Utils.join(BRANCHES, name));
        if (!Utils.join(BRANCHES, name).exists()) {
            throw Utils.error("A branch with that name "
                    + "does not exist.");
        } else if (currentBranch.equals(b)) {
            throw Utils.error("Cannot remove the current "
                    + "branch.");
        } else {
            Utils.join(BRANCHES, name).delete();
        }
    }

    /**
     * Usage: java gitlet.Main reset [commit id].
     * @param args Array in format: {'reset', commit id}
     */
    public static void reset(String[] args) {
        validateNumArgs(args, 2, 2);
        String commitId = args[1];
        Commit c = makeCommit(commitId);

        String currentId = readHeadId();
        Commit current = makeCommit(currentId);

        checkUntrackedFiles(current, c);

        updateCWD(current, c);

        modifyHeadId(commitId);

        clearStagingArea();
    }

    /**
     * Usage: java gitlet.Main merge [branch name].
     * @param args Array in format: {'merge', branch name}
     */
    public static void merge(String[] args) {
        validateNumArgs(args, 2, 2);
        ArrayList<Commit> splitPoints = new ArrayList<>();
        String givenBranch = args[1];
        String givenCommitId = readCommitId(givenBranch);
        Commit givenCommit = makeCommit(givenCommitId);
        Commit givenPointer = givenCommit;
        int distFromGiven = 0;

        String currentHeadId = readHeadId();
        Commit currentCommit = makeCommit(currentHeadId);
        Commit currentPointer = currentCommit;
        int distFromCurr = 0;

        while (!haveSameParents(currentPointer, givenPointer)) {
            currentPointer = makeCommit(currentPointer.getFirstParent());
            givenPointer = makeCommit(givenPointer.getFirstParent());
        }

    }

    private static boolean haveSameParents(Commit current, Commit other) {
        String currFirstParent = current.getFirstParent();
        String currSecondParent = current.getSecondParent();
        String otherFirstParent = other.getFirstParent();
        String otherSecondParent = other.getSecondParent();

        if (currFirstParent != null && otherFirstParent != null) {
            return currFirstParent.equals(otherFirstParent);
        }
        return false;
    }

    /**
     * Read the recent commit id of the given branch.
     * @param branchName String
     * @return an SHA-1 hash string.
     */
    private static String readCommitId(String branchName) {
        return Utils.readContentsAsString(Utils.join(BRANCHES, branchName));
    }

    /** Return hash that Master and HEAD points to. */
    private static String readHeadId() {
        String pathToHead = Utils.readContentsAsString(HEAD);
        File head = new File(pathToHead);
        return Utils.readContentsAsString(head);
    }

    /** Return commit object with hash from COMMITS_DIR.
     * @param hash String
     */
    private static Commit makeCommit(String hash) {
        Commit result;
        try {
            result = Utils.readObject(Utils.join(COMMITS_DIR,
                    hash), Commit.class);
        } catch (IllegalArgumentException err) {
            throw Utils.error(" No commit with that id exists.");
        }
        return result;
    }

    /** Modify the saved commit id in HEAD pointer.
     * @param newCommitId String commit id.
     */
    private static void modifyHeadId(String newCommitId) {
        String pathToHead = Utils.readContentsAsString(HEAD);
        File head = new File(pathToHead);
        Utils.writeContents(head, newCommitId);
    }

    /**
     * Update current Head pointer to a new branch.
     * @param branchName
     */
    private static void moveHeadTo(String branchName) {
        Utils.writeContents(HEAD, Utils.join(BRANCHES,
                branchName).getPath());
    }

    /**
     * Clear the staging area.
     */
    private static void clearStagingArea() {
        Utils.writeObject(STAGING_AREA, new StagingArea());
    }

    /**
     * Add or remove files in CWD.
     * @param current a commit object
     * @param other a commit object
     */
    private static void updateCWD(Commit current, Commit other) {
        for (Map.Entry<String, String> entry: current.getFileNames()
                .entrySet()) {
            String filename = entry.getKey();
            if (!other.isTracked(filename)) {
                Utils.restrictedDelete(Utils.join(CWD, filename));
            }
        }

        for (Map.Entry<String, String> entry: other.getFileNames().entrySet()) {
            String filename = entry.getKey();
            String blobId = entry.getValue();
            byte[] content = Utils.readContents(Utils.join(BLOBS_DIR, blobId));
            Utils.writeContents(Utils.join(CWD, filename), content);
        }
    }

    /**
     *
     * Throw an error if there is a working file in CWD of the current
     * branch that is not tracked, but that is tracked in the other
     * branch and will be overwritten by the command.
     * @param curr Commit object
     * @param other Commit object
     */
    private static void checkUntrackedFiles(Commit curr, Commit other) {
        List<String> files = Utils.plainFilenamesIn(CWD);
        for (String filename: files) {
            if (!curr.isTracked(filename)
                    && other.isTracked(filename)) {
                throw Utils.error("There is an "
                        + "untracked file in the "
                        + "way; delete it, or add "
                        + "and commit it first.");
            }
        }
    }

    /**
     * Checks the number of arguments between [min, max],
     * throws a GitletException if they do not match.
     *
     * @param args Argument array from command line
     * @param min Minimum Number of arguments
     * @param max Maximum Number of arguments
     */
    private static void validateNumArgs(String[] args, int min, int max) {
        if (min > args.length || args.length > max) {
            throw Utils.error("Incorrect operands.");
        }
    }

}
