package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {

    static final File GITLET_FOLDER = new File(".gitlet");
    static final File STAGING_AREA = new File(".gitlet/staging");
    static final File STAGING_ADDITION = new File(".gitlet/staging/addition");
    static final File STAGING_REMOVAL = new File(".gitlet/staging/removal");
    static final File COMMITS_FOLDER = new File(".gitlet/commits");
    static final File INFO_FOLDER = new File(".gitlet/info");
    static final File BLOBS = new File(".gitlet/commits/blobs");
    static final File COMMITS = new File(".gitlet/commits/commits");
    static final File HEAD = new File(".gitlet/info/head");
    static final File INITIAL = new File(".gitlet/info/initial");
    static final File MASTER = new File(".gitlet/info/branches/master");
    static final File CURRENT_BRANCH = new File(".gitlet/info/current-branch");
    static final File BRANCHES = new File(".gitlet/info/branches");

    public static void main(String... args) {
        checkFailureCases(args);
        switch (args[0]) {
            case "init":
                init();
                break;
            case "add":
                add(args[1]);
                break;
            case "commit":
                commit(args[1], null);
                break;
            case "log":
                log();
                break;
            case "rm":
                rm(args[1]);
                break;
            case "find":
                find(args[1]);
                break;
            case "status":
                status();
                break;
            case "branch":
                branch(args[1]);
                break;
            case "checkout":
                checkoutHelper(args);
                break;
            case "global-log":
                globalLog();
                break;
            case "rm-branch":
                rmBranch(args[1]);
                break;
            case "reset":
                reset(args[1]);
                break;
            case "merge":
                merge(args[1]);
        }
    }

    private static void checkoutHelper(String[] args) {
        if (args.length == 2) {
            checkoutBranch(args[1]);
        } else if (args.length == 3) {
            if (!args[1].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            checkoutFile(HEAD, args[2]);
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            checkoutCommitFile(args[1], args[3]);
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    private static void validateNumArgs(String[] args, Integer n) {
        if (args.length != n) {
            System.out.println("Incorrect operands");
            System.exit(0);
        }
    }

    private static void checkFailureCases(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command");
            System.exit(0);
        }
        switch (args[0]) {
            case "init":
                validateNumArgs(args, 1);
                if (GITLET_FOLDER.isDirectory()) {
                    System.out.println("A Gitlet version-control system already exists in the current directory.");
                    System.exit(0);
                }
                break;
            case "rm":
            case "add":
            case "commit":
            case "branch":
            case "find":
            case "rm-branch":
            case "reset":
            case "merge":
                validateNumArgs(args, 2);
                if (!GITLET_FOLDER.isDirectory()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                break;
            case "log":
            case "status":
            case "global-log":
                validateNumArgs(args, 1);
                if (!GITLET_FOLDER.isDirectory()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    System.exit(0);
                }
                break;
            case "checkout":
                break;
            default :
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }

    private static void init() {
        GITLET_FOLDER.mkdir();
        STAGING_AREA.mkdir();
        STAGING_ADDITION.mkdir();
        STAGING_REMOVAL.mkdir();
        COMMITS_FOLDER.mkdir();
        INFO_FOLDER.mkdir();
        BLOBS.mkdir();
        COMMITS.mkdir();
        BRANCHES.mkdir();

        Commit init = Commit.Initial();
        init.setHashValue(Utils.sha1(Utils.serialize(init)));
        File initFile = new File(COMMITS + "/" + init.getHashValue());

        Commit sentinel = Commit.Sentinel();
        sentinel.setParent(init.getHashValue());
        sentinel.setHashValue("sentinel");
        init.setParent(sentinel.getHashValue());
        File sentinelFile = new File(COMMITS + "/" + sentinel.getHashValue());

        try {
            INITIAL.createNewFile();
            HEAD.createNewFile();
            MASTER.createNewFile();
            CURRENT_BRANCH.createNewFile();
        } catch (IOException e) {
        }

        Utils.writeObject(INITIAL, sentinel);
        Utils.writeObject(HEAD, init);
        Utils.writeObject(MASTER, init);
        Utils.writeContents(CURRENT_BRANCH, "master");
        Utils.writeObject(initFile, init);
        Utils.writeObject(sentinelFile, sentinel);
    }

    private static void add(String fileName) {
        File copyFile = new File(fileName);

        if (!copyFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        File addFile = new File(STAGING_ADDITION + "/" + fileName);

        try {
            addFile.createNewFile();
            Utils.writeContents(addFile, Utils.readContents(copyFile));
        } catch (IOException exception) {
        }

        String hashValue = Utils.sha1(Utils.readContents(addFile));
        if (Utils.readObject(HEAD, Commit.class).contains(fileName, hashValue)) {
            addFile.delete();
        }
        File checkRemFile = new File(STAGING_REMOVAL + "/" + fileName);
        if (checkRemFile.exists()) {
            checkRemFile.delete();
        }
    }

    private static void commit(String message, String mergeParent) {
        if (STAGING_ADDITION.listFiles().length == 0 && STAGING_REMOVAL.listFiles().length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        if (message.isBlank()) {
            System.out.println("Please enter a commit message.");
        }
        Commit c = new Commit(message, Utils.readObject(HEAD, Commit.class).getBlobs(),Utils.readObject(HEAD, Commit.class).getHashValue());
        if (mergeParent != null) {
            c.setMergeParent(mergeParent);
        }

        for (File f : STAGING_ADDITION.listFiles()) {
            File commitFile = new File(BLOBS + "/" + Utils.sha1(Utils.readContents(f)) + f.getName());
            try {
                commitFile.createNewFile();
                Utils.writeContents(commitFile, Utils.readContents(f));
                if (c.getBlobs().containsKey(f.getName())) {
                    c.getBlobs().replace(f.getName(), new Blob(commitFile, Utils.sha1(Utils.readContents(commitFile))));
                } else {
                    c.getBlobs().put(f.getName(), new Blob(commitFile, Utils.sha1(Utils.readContents(commitFile))));
                }
            } catch (IOException exception) {
            }
        }

        for (File f : STAGING_REMOVAL.listFiles()) {
            c.getBlobs().remove(f.getName());
        }

        for (File f : STAGING_REMOVAL.listFiles()) {
            f.delete();
        }
        for (File f : STAGING_ADDITION.listFiles()) {
            f.delete();
        }

        c.setHashValue(Utils.sha1(Utils.serialize(c)));
        Commit sentinel = Utils.readObject(INITIAL, Commit.class);
        sentinel.setParent(c.getHashValue());
        Utils.writeObject(INITIAL, sentinel);
        Utils.writeObject(new File(COMMITS + "/sentinel"), sentinel);
        Utils.writeObject(HEAD, c);
        Utils.writeObject(new File(BRANCHES + "/" + Utils.readContentsAsString(CURRENT_BRANCH)), c);
        File commitFile = new File(COMMITS + "/" + c.getHashValue());
        Utils.writeObject(commitFile, c);
    }

    private static void log() {
        Commit current = Utils.readObject(HEAD, Commit.class);
        Commit init = Utils.readObject(INITIAL, Commit.class);
        while (!current.getHashValue().equals(init.getHashValue())) {
            System.out.println("===");
            System.out.println("commit " + current.getHashValue());
            if (current.getMergeParent() != null) {
                System.out.println("Merge: " + current.getParent().substring(0, 7) + " " + current.getMergeParent().substring(0, 7));
            }
            System.out.println("Date: " + current.getTime());
            System.out.println(current.getMessage());
            System.out.println();
            current = Utils.readObject(new File(COMMITS + "/" + current.getParent()), Commit.class);
        }
    }

    private static void globalLog() {
        Set<String> commits = new HashSet<>();
        for (File f : BRANCHES.listFiles()) {
            Commit branchHead = Utils.readObject(f, Commit.class);
            Commit init = Utils.readObject(INITIAL, Commit.class);
            while (true) {
                if (commits.contains(branchHead.getHashValue())) {
                    break;
                }
                if (!branchHead.getHashValue().equals("sentinel")) {
                    System.out.println("===");
                    System.out.println("commit " + branchHead.getHashValue());
                    if (branchHead.getMergeParent() != null) {
                        System.out.println("Merge: " + branchHead.getParent().substring(0, 7) + " " + branchHead.getMergeParent().substring(0, 7));
                    }
                    System.out.println("Date: " + branchHead.getTime());
                    System.out.println(branchHead.getMessage());
                    System.out.println();
                }
                commits.add(branchHead.getHashValue());
                branchHead = Utils.readObject(new File(COMMITS + "/" + branchHead.getParent()), Commit.class);
            }
        }
    }

    private static void rm(String fileName) {
        File f = new File(STAGING_ADDITION + "/" + fileName);
        if (!f.exists() && !Utils.readObject(HEAD, Commit.class).getBlobs().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        if (f.exists()) {
            f.delete();
        }
        if (Utils.readObject(HEAD, Commit.class).getBlobs().containsKey(fileName)) {
            try {
                new File(STAGING_REMOVAL + "/" + fileName).createNewFile();
                Utils.restrictedDelete(fileName);
            } catch (IOException e) {
            }
        }
    }

    private static void find(String message) {
        boolean foundCommit = false;
        Set<String> commits = new HashSet<>();
        for (File f : BRANCHES.listFiles()) {
            Commit branchHead = Utils.readObject(f, Commit.class);
            Commit init = Utils.readObject(INITIAL, Commit.class);
            while (true) {
                if (commits.contains(branchHead.getHashValue())) {
                    break;
                }
                if (branchHead.getMessage().equals(message)) {
                    foundCommit = true;
                    System.out.println(branchHead.getHashValue());
                }
                commits.add(branchHead.getHashValue());
                branchHead = Utils.readObject(new File(COMMITS + "/" + branchHead.getParent()), Commit.class);
            }
        }
        if (!foundCommit) {
            System.out.println("Found no commit with that message");
        }
    }

    private static void status() {
        System.out.println("=== Branches ===");
        for (File f : BRANCHES.listFiles()) {
            if (Utils.readContentsAsString(CURRENT_BRANCH).equals(f.getName())) {
                System.out.println("*" + f.getName());
            } else {
                System.out.println(f.getName());
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (File f : STAGING_ADDITION.listFiles()) {
            System.out.println(f.getName());
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (File f : STAGING_REMOVAL.listFiles()) {
            System.out.println(f.getName());
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
    }

    private static void branch(String name) {
        File branch = new File(BRANCHES + "/" + name);
        if (!branch.exists()) {
            try {
                branch.createNewFile();
                Utils.writeObject(branch, Utils.readObject(HEAD, Commit.class));
            } catch (IOException exception) {
            }
        } else {
            System.out.println("A branch with that name already exists.");
        }
    }

    private static void checkoutFile(File commitFile, String fileName) {
        Commit head = Utils.readObject(commitFile, Commit.class);
        if (!head.getBlobs().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        try {
            File checkoutFile = new File(fileName);
            checkoutFile.createNewFile();
            Utils.writeContents(checkoutFile, Utils.readContents(new File(BLOBS + "/" + head.getBlobs().get(fileName).getHashValue() + fileName)));
        } catch (IOException exception) {
        }
    }

    private static File abbrevCommitHelper(String id) {
        Set<String> commits = new HashSet<>();
        for (File f : BRANCHES.listFiles()) {
            Commit branchHead = Utils.readObject(f, Commit.class);
            Commit init = Utils.readObject(INITIAL, Commit.class);
            while (!branchHead.getHashValue().equals(init.getHashValue())) {
                if (commits.contains(branchHead.getHashValue())) {
                    break;
                }
                if (branchHead.getHashValue().substring(0, id.length()).equals(id)) {
                    return new File(COMMITS + "/" + branchHead.getHashValue());
                }
                commits.add(branchHead.getHashValue());
                branchHead = Utils.readObject(new File(COMMITS + "/" + branchHead.getParent()), Commit.class);
            }
        }
        return null;
    }

    private static void checkoutCommitFile(String id, String fileName) {
        File commitFile;
        if (id.length() == 40) {
            commitFile = new File(COMMITS + "/" + id);
            if (!commitFile.exists()) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            checkoutFile(commitFile, fileName);
        } else if (id.length() < 40) {
            commitFile = abbrevCommitHelper(id);
            if (commitFile == null) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            checkoutFile(commitFile, fileName);
        } else {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
    }

    private static void checkUntrackedFiles(Commit curr, Commit given, boolean merge) {
        for (Map.Entry blob : given.getBlobs().entrySet()) {
            String fileName = (String) blob.getKey();
            String hashValue = ((Blob) blob.getValue()).getHashValue();
            File headFile = new File(fileName);
            if (merge) {
                if (headFile.exists() && ((curr.getBlobs().containsKey(fileName) && !curr.contains(fileName, hashValue) && !curr.contains(fileName, Utils.sha1(Utils.readContents(headFile))))
                        || !curr.getBlobs().containsKey(fileName))) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            } else {
                if (headFile.exists() && !curr.getBlobs().containsKey(fileName)) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }
    }

    private static void checkoutBranch(String branchName) {
        File branch = new File(BRANCHES+ "/" + branchName);
        if (branchName.equals(Utils.readContentsAsString(CURRENT_BRANCH))) {
            System.out.println("No need to checkout the current branch.");
        }
        changeCommitHelper(branch, "No such branch exists.", false);
    }

    private static void rmBranch(String branchName) {
        File branch = new File(BRANCHES + "/" + branchName);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(Utils.readContentsAsString(CURRENT_BRANCH))) {
            System.out.println("Cannot remove the current branch");
            System.exit(0);
        }
        branch.delete();
    }

    private static void changeCommitHelper(File commitFile, String message, Boolean reset) {
        if (!commitFile.exists()) {
            System.out.println(message);
            System.exit(0);
        }
        Commit c = Utils.readObject(commitFile, Commit.class);
        checkUntrackedFiles(Utils.readObject(HEAD, Commit.class), c, false);
        for (Map.Entry blob : c.getBlobs().entrySet()) {
            String fileName = (String) blob.getKey();
            checkoutFile(commitFile, fileName);
        }
        for (String fileName : Utils.plainFilenamesIn(System.getProperty("user.dir"))) {
            if (!fileName.equals(".gitignore") && !fileName.equals("proj2.iml")) {
                if (!c.getBlobs().containsKey(fileName)) {
                    new File(fileName).delete();
                }
            }
        }
        Utils.writeObject(HEAD, c);
        if (reset) {
            Utils.writeObject(new File(BRANCHES + "/" + Utils.readContentsAsString(CURRENT_BRANCH)), c);
        } else {
            Utils.writeContents(CURRENT_BRANCH, commitFile.getName());
        }
        for (File f : STAGING_REMOVAL.listFiles()) {
            f.delete();
        }
        for (File f : STAGING_ADDITION.listFiles()) {
            f.delete();
        }
    }

    private static void reset(String id) {
        File commitFile;
        if (id.length() == 40) {
            commitFile = new File(COMMITS + "/" + id);
            changeCommitHelper(commitFile, "No commit with that id exists.", true);
        } else if (id.length() < 40) {
            commitFile = abbrevCommitHelper(id);
            if (commitFile == null) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            changeCommitHelper(commitFile, "No commit with that id exists.", true);
        } else {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
    }

    private static Commit findCloserCommit(Commit curr, Commit sp1, Commit sp2) {
        int sp1Counter = 0;
        int sp2Counter = 0;
        while (true) {
            if (curr.getHashValue().equals(sp1.getHashValue())) {
                break;
            }
            curr = Utils.readObject(new File(COMMITS + "/" + curr.getParent()), Commit.class);
            sp1Counter += 1;
        }
        while (true) {
            if (curr.getHashValue().equals(sp2.getHashValue())) {
                break;
            }
            if (curr.getMergeParent() != null) {
                curr = Utils.readObject(new File(COMMITS + "/" + curr.getMergeParent()), Commit.class);
            } else {
                curr = Utils.readObject(new File(COMMITS + "/" + curr.getParent()), Commit.class);
            }
            sp2Counter += 1;
        }
        if (sp1Counter < sp2Counter) {
            return sp1;
        } else {
            return sp2;
        }
    }

    private static Commit findSplitPoint(Commit curr, Commit merge, Set<String> currCommits, Set<String> mergeCommits, String branchName) {
        Commit currSplitPoint = null;
        Commit mergeSplitPoint = null;
        Set<String> tempCurrCommits = currCommits;
        Set<String> tempMergeCommits = mergeCommits;
        while (true) {
            currCommits.add(curr.getHashValue());
            mergeCommits.add(merge.getHashValue());
            if (curr.getMergeParent() != null) {
                currSplitPoint = findSplitPoint(Utils.readObject(new File(COMMITS + "/" + curr.getMergeParent()), Commit.class), Utils.readObject(new File(COMMITS + "/" + merge.getParent()), Commit.class), tempCurrCommits, tempMergeCommits, branchName);
            }
            if (merge.getMergeParent() != null) {
                mergeSplitPoint = findSplitPoint(Utils.readObject(new File(COMMITS + "/" + curr.getParent()), Commit.class), Utils.readObject(new File(COMMITS + "/" + merge.getMergeParent()), Commit.class), tempCurrCommits, tempMergeCommits, branchName);
            }
            if (mergeCommits.contains(curr.getHashValue())) {
                if (currSplitPoint != null) {
                    return findCloserCommit(Utils.readObject(new File(BRANCHES + "/" + Utils.readContentsAsString(CURRENT_BRANCH)), Commit.class), curr, currSplitPoint);
                } else {
                    return curr;
                }
            }
            if (currCommits.contains(merge.getHashValue())) {
                if (mergeSplitPoint != null) {
                    return findCloserCommit(Utils.readObject(new File(BRANCHES + "/" + branchName), Commit.class), curr, mergeSplitPoint);
                } else {
                    return merge;
                }
            }
            curr = Utils.readObject(new File(COMMITS + "/" + curr.getParent()), Commit.class);
            merge = Utils.readObject(new File(COMMITS + "/" + merge.getParent()), Commit.class);
        }
    }

    private static void merge(String branchName) {
        File mergeBranch = new File(BRANCHES + "/" + branchName);
        if (!mergeBranch.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (STAGING_ADDITION.listFiles().length != 0 || STAGING_REMOVAL.listFiles().length != 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (mergeBranch.getName().equals(Utils.readContentsAsString(CURRENT_BRANCH))) {
            System.out.println("Cannot merge branch with itself");
            System.exit(0);
        }
        Commit merge = Utils.readObject(mergeBranch, Commit.class);
        Commit curr = Utils.readObject(new File(BRANCHES + "/" + Utils.readContentsAsString(CURRENT_BRANCH)), Commit.class);
        Set<String> currCommits = new HashSet<>();
        Set<String> mergeCommits = new HashSet<>();
        Commit splitPoint = findSplitPoint(curr, merge, currCommits, mergeCommits, branchName);
        checkUntrackedFiles(Utils.readObject(HEAD, Commit.class), Utils.readObject(new File(BRANCHES + "/" + branchName), Commit.class), true);
        if (splitPoint.getHashValue().equals(merge.getHashValue())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPoint.getHashValue().equals(curr.getHashValue())) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwaded.");
            System.exit(0);
        }
        if (mergeConflictHelper(splitPoint, curr, merge)) {
            System.out.println("Encountered a merge conflict.");
        }
        String message = "Merged " + branchName + " into " + Utils.readContentsAsString(CURRENT_BRANCH) + ".";
        commit(message, merge.getHashValue());
        Utils.writeObject(new File(BRANCHES + "/" + branchName), Utils.readObject(HEAD, Commit.class));
    }

    private static boolean mergeConflictHelper(Commit splitPoint, Commit curr, Commit given) {
        boolean test = false;
        boolean mergeConflict = false;
        for (Map.Entry blob : given.getBlobs().entrySet()) {
            String fileName = (String) blob.getKey();
            String hashValue = ((Blob) blob.getValue()).getHashValue();
            // merge conflict; contents are different
            // present and different in all 3
            if (splitPoint.getBlobs().containsKey(fileName) && !splitPoint.contains(fileName, hashValue) && curr.getBlobs().containsKey(fileName) && !curr.contains(fileName, hashValue) && !splitPoint.contains(fileName, curr.getBlobs().get(fileName).getHashValue())
                    // present at split, different at given, absent at curr
                    || splitPoint.getBlobs().containsKey(fileName) && !splitPoint.contains(fileName, hashValue) && !curr.getBlobs().containsKey(fileName)
                    // absent at split and different at given and curr
                    || !splitPoint.getBlobs().containsKey(fileName) && curr.getBlobs().containsKey(fileName) && !curr.contains(fileName, hashValue)) {
                mergeConflict = true;
                File replacedFile = new File(fileName);
                String currContent;
                if (!curr.getBlobs().containsKey(fileName)) {
                    currContent = "";
                } else {
                    currContent = Utils.readContentsAsString(new File(BLOBS + "/" + curr.getBlobs().get(fileName).getHashValue() + fileName));
                }
                String contents = "<<<<<<< HEAD\n" + currContent + "=======\n" + Utils.readContentsAsString(new File(BLOBS + "/" + hashValue + fileName)) + ">>>>>>>\n";
                Utils.writeContents(replacedFile, contents);
                add(fileName);
                continue;
            }
            // present at split
            if (splitPoint.getBlobs().containsKey(fileName)) {
                // modified in given branch but same in curr
                if (!splitPoint.contains(fileName, hashValue) && curr.contains(fileName, splitPoint.getBlobs().get(fileName).getHashValue())) {
                    checkoutCommitFile(given.getHashValue(), fileName);
                    add(fileName);
                }
                // unmodified in given branch but absent or modified in curr
                if (splitPoint.contains(fileName, hashValue) &&  (!curr.getBlobs().containsKey(fileName) || !curr.contains(fileName, hashValue))) {
                    test = true;
                }
                // modified in the same way
                if (!splitPoint.contains(fileName, hashValue) && curr.contains(fileName, hashValue)) {
                    test = true;
                }
            } else {
                // not present at split
                // present only in given branch
                if (!curr.getBlobs().containsKey(fileName)) {
                    checkoutCommitFile(given.getHashValue(), fileName);
                    add(fileName);
                }
            }
        }
        for (Map.Entry blob : curr.getBlobs().entrySet()) {
            String fileName = (String) blob.getKey();
            String hashValue = ((Blob) blob.getValue()).getHashValue();
            // not present at split and present only in given branch
            if (!splitPoint.getBlobs().containsKey(fileName) && !given.getBlobs().containsKey(fileName)) {
                test = false;
            }
            // unmodified in current branch and absent in given branch;
            if (splitPoint.contains(fileName, hashValue) && !given.getBlobs().containsKey(fileName)) {
                File delFile = new File(fileName);
                rm(fileName);
                delFile.delete();
            }
            // present at split, absent at given, different at curr
            if (splitPoint.getBlobs().containsKey(fileName) && !splitPoint.contains(fileName, hashValue) && !given.getBlobs().containsKey(fileName)) {
                mergeConflict = true;
                File replacedFile = new File(fileName);
                String givenContent;
                if (!given.getBlobs().containsKey(fileName)) {
                    givenContent = "";
                } else {
                    givenContent = Utils.readContentsAsString(new File(BLOBS + "/" + given.getBlobs().get(fileName).getHashValue() + fileName));
                }
                String contents = "<<<<<<< HEAD\n" + Utils.readContentsAsString(new File(BLOBS + "/" + hashValue + fileName)) + "=======\n" + givenContent + ">>>>>>>\n";
                Utils.writeContents(replacedFile, contents);
                add(fileName);
            }
        }
        return mergeConflict;
    }
}
