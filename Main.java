package gitlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** Driver class for Gitlet, the mini version-control system.
 *  @author Yonas/Juno
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException,
            ClassNotFoundException {
        _args = args;
        statement();
        serialize();
    }

    static void statement() throws IOException, ClassNotFoundException {
        if (_args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        switch (_args[0]) {
        case "init":
            initStatement();
            break;
        case "add":
            deserialize();
            addStatement();
            break;
        case "commit":
            deserialize();
            commitStatement();
            break;
        case "rm":
            deserialize();
            rmStatement();
            break;
        case "log":
            deserialize();
            logStatement(head);
            break;
        case "global-log":
            deserialize();
            globalLogStatement();
            break;
        case "find":
            deserialize();
            findStatement();
            break;
        case "status":
            deserialize();
            statusStatement();
            break;
        case "checkout":
            deserialize();
            checkoutStatement();
            break;
        case "branch":
            deserialize();
            branchStatement();
            break;
        case "rm-branch":
            deserialize();
            rmBranchStatement();
            break;
        case "reset":
            deserialize();
            resetStatement();
            break;
        case "merge":
            deserialize();
            mergeStatement();
            break;
        default:
            System.out.println("No command with that name exists.");
        }
    }

    /** Creates a new gitlet version-control system in the current
     * directory. Automatically starts with one commit that contains no
     * files, has the commit message initial commit, and has a single
     * branch: master, which initially points to this initial commit.
     * master will be the current branch. */
    public static void initStatement() {
        File dotgitlet = new File("./.gitlet");
        if (dotgitlet.exists()) {
            System.out.println("A gitlet version-control system already "
                    + "exists in the current directory.");
        } else {
            dotgitlet.mkdir();
            new File("./.gitlet/log").mkdir();
            new File("./.gitlet/stage").mkdir();
            new File("./.gitlet/CommitFiles").mkdir();
            new File("./.gitlet/Objects").mkdir();
            new File("./.gitlet/RemovedFiles").mkdir();
            Commit init = new Commit("initial commit");
            init.save();
            head = init;
            branch = "master";
            tree = new Tree(branch, init);
        }
    }

    /** Adds a copy of the file as it currently exists to the staging area.
     * For this reason, adding a file is also called staging the file. The
     * staging area should be somewhere in .gitlet. If the current working
     * version of the file is identical to the version in the repository,
     * do nothing .*/
    public static void addStatement() {
        String fileName = _args[1];
        HashMap<String, String> headFiles = head.getBlobsMap();
        String[] workingFiles = new File("./").list();
        String[] removedPaths = new File("./.gitlet/RemovedFiles").list();
        if (!Arrays.asList(workingFiles).contains(fileName)) {
            System.out.println("File does not exist.");
            return;
        }
        if (Arrays.asList(removedPaths).contains(fileName)) {
            File c = new File("./.gitlet/RemovedFiles/" + fileName);
            c.delete();
        }
        File currFile = new File("./" + fileName);
        byte[] currentContent = Utils.readContents(currFile);
        String currShaCode = Utils.sha1(currentContent);
        if (!headFiles.containsKey(fileName)) {
            File newStaged = new File("./.gitlet/stage/" + fileName);
            Utils.writeContents(newStaged, currentContent);
        } else if (currShaCode.equals(headFiles.get(fileName))) {
            return;
        } else {
            File newStaged = new File("./.gitlet/stage/" + fileName);
            Utils.writeContents(newStaged, currentContent);
        }
    }


    /** Saves a snapshot of certain files in the current commit and staging
     * area, creating a new commit. The commit is said to be tracking the saved
     * files. A commit only updates files it is tracking that have been staged
     * at the time of commit. A commit will save and start tracking any files
     * that were staged but weren't tracked by its parent. */
    public static void commitStatement() {
        if (_args.length < 2 || _args[1].equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        String msg = _args[1];
        String[] stagedPaths = new File("./.gitlet/stage").list();
        String[] removedPaths = new File("./.gitlet/RemovedFiles").list();
        if (stagedPaths.length == 0 && removedPaths.length == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }
        HashMap<String, String> blobs;
        blobs = new HashMap<String, String>(head.getBlobsMap());
        for (String r: removedPaths) {
            if (blobs.containsKey(r)
                    && (Arrays.asList(removedPaths).contains(r))) {
                blobs.remove(r);
            }
            File c = new File("./.gitlet/RemovedFiles/" + r);
            c.delete();
        }
        for (String f: stagedPaths) {
            File file = new File(f);
            blobs.put(file.getName(), Utils.sha1(Utils.readContents(file)));
            File addtoCommitFiles;
            addtoCommitFiles = new File("./.gitlet/CommitFiles/"
                    + Utils.sha1(Utils.readContents(file)));
            Utils.writeContents(addtoCommitFiles, Utils.readContents(file));
            File d = new File("./.gitlet/stage/" + f);
            d.delete();
        }
        Commit newCommit = new Commit(blobs, head, msg, branch);
        newCommit.save();
        head = newCommit;
        tree.makeBranch(branch, newCommit);
    }

    /** Untracks a file - Indicates that a file is not to be included in the
     * next commit, even if it is tracked in the current commit. Removes the
     * file from the working directory if it was tracked in the current commit.
     * If the file had been staged, then it is unstaged, but is NOT removed
     * from the working directory unless it was tracked in the current commit.
     * */
    public static void rmStatement() {
        String fileName = _args[1];
        File newFile = new File("./" + fileName);
        String[] stagedPaths = new File("./.gitlet/stage").list();
        String[] workingPaths = new File("./").list();
        HashMap<String, String> headFiles = head.getBlobsMap();
        if (!newFile.exists()) {
            File removedFile = new File("./.gitlet/RemovedFiles/" + fileName);
            try {
                removedFile.createNewFile();
            } catch (IOException e) {

            }
            return;
        } else if (!Arrays.asList(stagedPaths).contains(fileName)
                    && !headFiles.containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        if (Arrays.asList(stagedPaths).contains(fileName)) {
            File d = new File("./.gitlet/stage/" + fileName);
            d.delete();
        } 
        if (headFiles.containsKey(fileName)) {
            byte[] newContent = Utils.readContents(newFile);
            File removedFile = new File("./.gitlet/RemovedFiles/" + fileName);
            Utils.writeContents(removedFile, newContent);
            File w = new File("./" + fileName);
            w.delete();
        }
    }

    /** display information about each commit backwards along the commit tree
     * until the initial commit. Shows commit id, timestamp, and message COMMIT.
     * */
    public static void logStatement(Commit commit) {
        if (commit.getParent() != null) {
            System.out.println("===");
            System.out.println("Commit " + commit.getShaCode());
            System.out.println(commit.getTime());
            System.out.println(commit.getMessage() + "\n");
            logStatement(commit.getParent());
        } else {
            System.out.println("===");
            System.out.println("Commit " + commit.getShaCode());
            System.out.println(commit.getTime());
            System.out.println(commit.getMessage() + "\n");
        }
    }

    /** Displays all commits ever made; order does not matter.*/
    public static void globalLogStatement() {
        String[] paths = new File("./.gitlet/log").list();
        for (String com: paths) {
            Commit commit = Commit.load(com);
            System.out.println("===");
            System.out.println("Commit " + commit.getShaCode());
            System.out.println(commit.getTime());
            System.out.println(commit.getMessage() + "\n");
        }
    }

    /** Prints out the ids of all commits that have the given commit message,
     * one per line. If there are multiple such commits, it prints the ids
     * out on separate lines. */
    public static void findStatement() {
        String message = _args[1];
        int count = 0;
        String[] paths = new File("./.gitlet/log").list();
        for (String name: paths) {
            Commit commit = Commit.load(name);
            if (message.equals(commit.getMessage())) {
                System.out.println(commit.getShaCode());
                count += 1;
            }
        }
        if (count == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Displays what branches currently exist, and marks the current branch
     * with a *. Also displays what files have been staged or marked for
     * untracking.*/
    public static void statusStatement() {
        HashMap<String, String> branches = tree.getBranches();
        System.out.println("=== Branches ===");

        for (String key: branches.keySet()) {
            if (branch.equals(key)) {
                System.out.println("*" + key);
            } else {
                System.out.println(key);
            }
        }
        System.out.println();

        String[] stagedFiles = new File("./.gitlet/stage").list();
        System.out.println("=== Staged Files ===");
        for (String file: stagedFiles) {
            System.out.println(file);
        }
        System.out.println();

        HashMap<String, String> headFiles = head.getBlobsMap();
        Object[] objects = headFiles.keySet().toArray();
        String[] strObjects;
        strObjects = Arrays.copyOf(objects, objects.length, String[].class);
        String[] committedFiles = strObjects;
        String[] workingFiles = new File("./").list();
        String[] removedFiles = new File("./.gitlet/RemovedFiles").list();
        System.out.println("=== Removed Files ===");
        for (String file: removedFiles) {
            System.out.println(file);
        }
        System.out.println();

        String[] removeddFiles = new File("./.gitlet/RemovedFiles").list();
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String file: committedFiles) {
            if (Arrays.asList(workingFiles).contains(file)
                    && !Arrays.asList(stagedFiles).contains(file)) {
                File workFile = new File("./" + file);
                String sha = Utils.sha1(Utils.readContents(workFile));
                if (!sha.equals(headFiles.get(file))) {
                    System.out.println(file + " (modified)");
                }
            }
            if (!Arrays.asList(workingFiles).contains(file)
                    && !Arrays.asList(removeddFiles).contains(file)) {
                System.out.println(file + " (deleted)" + branch);
            }
        }
        for (String file: stagedFiles) {
            if (!Arrays.asList(workingFiles).contains(file)
                    && !Arrays.asList(removeddFiles).contains(file)) {
                System.out.println(file + " (deleted)");
            } else if (Arrays.asList(workingFiles).contains(file)) {
                File workFile = new File("./" + file);
                File stageFile = new File("./.gitlet/stage/" + file);
                String sha = Utils.sha1(Utils.readContents(workFile));
                String stagesha = Utils.sha1(Utils.readContents(stageFile));
                if (!sha.equals(stagesha)) {
                    System.out.println(file + " (modified)");
                } 
            }
        }
        System.out.println();

        System.out.println("=== Untracked Files ===");
        for (String file: workingFiles) {
            File inWork = new File("./" + file);
            if (!(headFiles.containsKey(file))
                    && !(Arrays.asList(stagedFiles).contains(file))
                    && !inWork.isDirectory() && !file.equals(".gitignore")
                    && !file.equals("Makefile")) {
                System.out.println(file);
            }
        }
    }

    /** Takes the version of a file as it exists in a certain commit (either
     * head or specified commit ID) and puts it in the working directory,
     * overwriting the version of the file that's already there if there is
     * one. Can also do this for all of the files in a specified branch. */
    public static void checkoutStatement() {
        String fileName;
        if (_args.length == 3) {
            if (!_args[1].equals("--")) {
                System.out.println("Incorrect operands.");
                return;
            }
            fileName = _args[2];
            checkoutFile(fileName);
        } else if (_args.length == 4) {
            if (!_args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                return;
            }
            fileName = _args[3];
            String commitID = _args[1];
            checkoutCommit(commitID, fileName);
        } else if (_args.length == 2) {
            String branchName = _args[1];
            checkoutBranch(branchName);
        }
    }

    /** Checkout given FILENAME. */
    public static void checkoutFile(String fileName) {
        if (!head.getBlobsMap().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
        } else {
            String headFileID = head.getBlobsMap().get(fileName);
            File headFile = new File("./.gitlet/CommitFiles/" + headFileID);
            byte[] headFileContents = Utils.readContents(headFile);
            File currFile = new File("./" + fileName);
            Utils.writeContents(currFile, headFileContents);
        }
    }

    /** Checkout given COMMITID and FILENAME. */
    public static void checkoutCommit(String commitID, String fileName) {
        String[] commits = new File("./.gitlet/log/").list();
        Commit commit = Commit.load(commitID + ".ser");
        if (!(Arrays.asList(commits).contains(commitID + ".ser"))) {
            System.out.println("No commit with that id exists.");
        } else if (!commit.getBlobsMap().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
        } else {
            String file = commit.getBlobsMap().get(fileName);
            File commitFile = new File("./.gitlet/CommitFiles/" + file);
            byte[] commitFileContents = Utils.readContents(commitFile);
            File currFile = new File("./" + fileName);
            Utils.writeContents(currFile, commitFileContents);
        }
    }

    /** Checkout given BRANCHNAME. */
    public static void checkoutBranch(String branchName) {
        if (!tree.getBranches().containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        } else if (branchName == branch) {
            System.out.println("No need to checkout the current branch.");
        } else {
            HashMap<String, String> headFiles = head.getBlobsMap();
            String[] workingFiles = new File("./").list();
            String[] stagedFiles = new File("./.gitlet/stage").list();
            String branchHeadID = tree.getBranches().get(branchName);
            Commit branchHead = Commit.load(branchHeadID + ".ser");
            for (String file: workingFiles) {
                if (!headFiles.containsKey(file)
                        && !Arrays.asList(stagedFiles).contains(file)
                        && branchHead.getBlobsMap().containsKey(file)) {
                    System.out.println("There is an untracked file in "
                            + "the way; delete it or add it first.");
                    return;
                }
            }

            for (String file : workingFiles) {
                File inWork = new File("./" + file);
                if (!inWork.isDirectory() && !file.equals(".gitignore")
                        && !file.equals("Makefile")) {
                    inWork.delete();
                }
            }
            String[] fileNames = new File("./.gitlet/CommitFiles/").list();
            for (String file : fileNames) {
                if (branchHead.getBlobsMap().containsValue(file)) {
                    File comFile = new File("./.gitlet/CommitFiles/" + file);
                    byte[] branchFileContents = Utils.readContents(comFile);
                    File currFile = new File("./"
                            + getKey(branchHead.getBlobsMap(), file));
                    Utils.writeContents(currFile, branchFileContents);
                }

            }
            String[] removedFiles =
                    new File("./.gitlet/RemovedFiles").list();
            for (String r: removedFiles) {
                File c = new File("./.gitlet/RemovedFiles/" + r);
                c.delete();
            }
            for (String f: stagedFiles) {
                File d = new File("./.gitlet/stage/" + f);
                d.delete();
            }
            head = branchHead;
            branch = branchName;
        }
    }

    /** Helper method that returns a file's name given the Hashmap it is
     * contained in and its sha-1 code BLOBS, VALUE. */
    public static String getKey(HashMap<String, String> blobs, String value) {
        for (String key : blobs.keySet()) {
            if (blobs.get(key).equals(value)) {
                return key;
            }
        }
        return null;
    }

    /** Takes the branch name(arg[1]) and the head commit
     * from the file head and places it inside the tree Hashmap.*/
    public static void branchStatement() {
        String name = _args[1];
        if (tree.getBranches().containsKey(name)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        tree.makeBranch(name, head);
    }

    /** Deletes the branch with the given name. This only means to delete the
     * pointer associated with the branch; it does not mean to delete all
     * commits that were created under the branch. */
    public static void rmBranchStatement() {
        String name = _args[1];
        if (!(tree.getBranches().containsKey(name))) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (name.equals(branch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        } else {
            tree.deleteBranch(name);
        }
    }

    /** Resets the head to the given commitID. If the commitID does not exits
     * or there are any currently untracked files a notice will be raised.
     * Otherwise, the head is set to the given commit and the branch is 
      * set to the commit's branch. */
    public static void resetStatement() {
        String commitID = _args[1];
        String[] commits = new File("./.gitlet/log/").list();
        Commit commit = Commit.load(commitID + ".ser");
        if (!(Arrays.asList(commits).contains(commitID + ".ser"))) {
            System.out.println("No commit with that id exists.");
        } else {
            HashMap<String, String> commitFiles = commit.getBlobsMap();
            String[] workingFiles = new File("./").list();
            String[] stagedFiles = new File("./.gitlet/stage").list();
            for (String file: workingFiles) {
                if (!commitFiles.containsKey(file)
                        && !Arrays.asList(stagedFiles).contains(file)
                        && commit.getBlobsMap().containsKey(file)) {
                    System.out.println("There is an untracked file in "
                            + "the way; delete it or add it first.");
                    return;
                }
            }

            for (String file : workingFiles) {
                File inWork = new File("./" + file);
                if (!inWork.isDirectory() && !file.equals(".gitignore")
                        && !file.equals("Makefile")) {
                    inWork.delete();
                }
            }

            String[] fileNames = new File("./.gitlet/CommitFiles/").list();
            for (String file : fileNames) {
                if (commit.getBlobsMap().containsValue(file)) {
                    File comFile = new File("./.gitlet/CommitFiles/" + file);
                    byte[] commitFileContents = Utils.readContents(comFile);
                    File currFile = new File("./"
                            + getKey(commit.getBlobsMap(), file));
                    Utils.writeContents(currFile, commitFileContents);
                }
            }

            String[] removedFiles = new File("./.gitlet/RemovedFiles").list();
            for (String r: removedFiles) {
                File c = new File("./.gitlet/RemovedFiles/" + r);
                c.delete();
            }
            for (String f: stagedFiles) {
                File d = new File("./.gitlet/stage/" + f);
                d.delete();
            }

            head = commit;
            branch = commit.getBranchName();
            tree.makeBranch(branch, head);
        }
    }

    /** Merges files from the given branch into the current branch. */
    public static void mergeStatement() {
        String branchName = _args[1];
        String branchID = tree.getBranches().get(branchName);
        Commit branchHead = Commit.load(branchID + ".ser");
        Commit splitPt = findSplit(head, branchHead);
        noConflict = true;
        String[] stagedFiles = new File("./.gitlet/stage").list();
        if (validMerge(branchID, splitPt, branchHead, stagedFiles)) {
            HashMap<String, String> branchFiles = branchHead.getBlobsMap();
            HashMap<String, String> splitFiles = splitPt.getBlobsMap();
            HashMap<String, String> headFiles = head.getBlobsMap();
            String[] allFileNames = new File("./.gitlet/CommitFiles/").list();
            for (String name : allFileNames) {
                String fileName = "";
                ArrayList<String> seenFiles = new ArrayList<String>();
                if (getKey(branchFiles, name) != null) {
                    fileName = getKey(branchFiles, name);
                } else if (getKey(splitFiles, name) != null) {
                    fileName = getKey(splitFiles, name);
                } else if (getKey(headFiles, name) != null) {
                    fileName = getKey(headFiles, name);
                }
                if (!seenFiles.contains(fileName)) {
                    seenFiles.add(fileName);
                    String branchFileCode = branchFiles.get(fileName);
                    String headFileCode = headFiles.get(fileName);
                    String splitFileCode = splitFiles.get(fileName);
                    Boolean fileInBranch = branchFiles.containsKey(fileName);
                    Boolean fileInHead = headFiles.containsKey(fileName);
                    Boolean fileInSplit = splitFiles.containsKey(fileName);
                    Boolean branchEqualsSplit = compareCodes(branchFileCode, splitFileCode);
                    Boolean headEqualsSplit = compareCodes(headFileCode, splitFileCode);

                    if (fileInBranch && fileInHead && fileInSplit) {
                        mergeHelper(branchEqualsSplit, headEqualsSplit, fileName, branchFiles,
                                branchID, branchFileCode,
                                headFiles, headFileCode);
                    } else if (fileInBranch && !fileInHead && !fileInSplit) {
                        checkoutCommit(branchID, getKey(branchFiles, branchFileCode));
                        addFile(fileName);
                    } else if (!fileInBranch && fileInHead && fileInSplit) {
                        if (headEqualsSplit) {
                            rmFile(fileName);
                        } else {
                            mergeFiles(headFileCode, branchFileCode, fileName);
                        }
                    } else if (!fileInBranch && fileInHead && !fileInSplit) {
                        checkoutCommit(head.getShaCode(), getKey(headFiles, headFileCode));
                        addFile(fileName);
                    } else if (fileInBranch && fileInHead && !fileInSplit) {
                        mergeFiles(headFileCode, branchFileCode, fileName);
                    } else if (fileInBranch && !fileInHead && fileInSplit) {
                        if (branchEqualsSplit) {
                            continue;
                        } else {
                            mergeFiles(headFileCode, branchFileCode, fileName);
                        }
                    }
                }
            }
            printMergeMessage(branchName);
        }
    }


    /** Helper for merging that returns true if two file codes are the same. If
     * the first code is null (branch or head will be passed in) then a new file is made*/
    static void compareCodes(code1, code2) {
        if (code1) {
            return code1.equals(code2)
        } else {
            try {
                File f = new File("./.gitlet/CommitFiles/" + "null");
                f.createNewFile();
                return false;
            } catch (IOException e) {}
        }
    }

    /** Helpter for merge that adds a file FILE after it is checked out. */
    static void addFile(String file) {
        _args = new String[]{"add", file};
        addStatement();
    }

    /** Helpter for merge that removes a file FILE. */
    static void rmFile(String file) {
        _args = new String[]{"rm", file};
        rmStatement();
    }


    /** Helper for merge conflict BRANCHNAME.
     */
    static void printMergeMessage(String branchName) {
        if (noConflict) {
            _args = new String[]{"commit", "Merged " + branch + " with "
                    + branchName + "."};
            commitStatement();
        } else {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Helper for merge HEQS, BEQS, FILENAME, BRANCHFILES, BRANCHID,
     *  BRANCHFILECODE, HEADFILES, HEADFILECODE.
     */
    static void mergeHelper(Boolean bEqS, Boolean hEqS, String fileName,
                              HashMap<String, String> branchFiles,
                              String branchID,
                              String branchFileCode,
                              HashMap<String, String> headFiles,
                              String headFileCode) {
        if (!bEqS && hEqS) {
            checkoutCommit(branchID, getKey(branchFiles, branchFileCode));
            File newFile = new File("./.gitlet/CommitFiles/" + branchFileCode);
            byte[] newContent = Utils.readContents(newFile);
            File newStaged = new File("./.gitlet/stage/" + fileName);
            Utils.writeContents(newStaged, newContent);
        } else if (bEqS && !hEqS) {
            checkoutCommit(head.getShaCode(), getKey(headFiles, headFileCode));
            addFile(fileName);
        } else if (!bEqS && !hEqS && !headFileCode.equals(branchFileCode)) {
            mergeFiles(headFileCode, branchFileCode, fileName);
            noConflict = false;
        }
    }

    /** Helper method that perfoms the checks if a merge will be necessary
     *  BRANCHID, SPLITPT, BRANCHHEAD, STAGEDFILES.
     * @return
     */
    static boolean validMerge(String branchID, Commit splitPt,
                              Commit branchHead, String[] stagedFiles) {
        String[] removedFiles = new File("./.gitlet/RemovedFiles").list();
        String[] workingFiles = new File("./").list();
        HashMap<String, String> headFiles = head.getBlobsMap();
        Object[] objects = headFiles.keySet().toArray();
        String[] strObjects;
        strObjects = Arrays.copyOf(objects, objects.length, String[].class);
        String[] committedFiles = strObjects;
        Boolean valid = false;
        Boolean mod = false;
        Boolean untracked = false;
        for (String file: committedFiles) {
            if (Arrays.asList(workingFiles).contains(file)
                    && !Arrays.asList(stagedFiles).contains(file)) {
                File workFile = new File("./" + file);
                String sha = Utils.sha1(Utils.readContents(workFile));
                if (!sha.equals(headFiles.get(file))) {
                    mod = true;
                }
            }
            if (!Arrays.asList(workingFiles).contains(file)
                    && !Arrays.asList(removedFiles).contains(file)) {
                mod = true;
            }
        }
        for (String file: stagedFiles) {
            if (!Arrays.asList(workingFiles).contains(file)
                    && !Arrays.asList(removedFiles).contains(file)) {
                mod = true;
            } else if (Arrays.asList(workingFiles).contains(file)) {
                File workFile = new File("./" + file);
                File stageFile = new File("./.gitlet/stage/" + file);
                String sha = Utils.sha1(Utils.readContents(workFile));
                String stagesha = Utils.sha1(Utils.readContents(stageFile));
                if (!sha.equals(stagesha)) {
                    mod = true;
                } 
            }
        }
        for (String file: workingFiles) {
            File inWork = new File("./" + file);
            if (!(headFiles.containsKey(file))
                    && !(Arrays.asList(stagedFiles).contains(file))
                    && !inWork.isDirectory() && !file.equals(".gitignore")
                    && !file.equals("Makefile")) {
                untracked = true;
            }
        }
        if (untracked) {
            System.out.println("There is an untracked file in "
                            + "the way; delete it or add it first.");
        } else if (stagedFiles.length != 0 || removedFiles.length != 0) {
            System.out.println("You have uncommitted changes.");
        } else if (branchID == null) {
            System.out.println("A branch with that name does not exist.");
        } else if (branchID.equals(head.getShaCode())) {
            System.out.println("Cannot merge a branch with itself.");
        } else if (splitPt.getShaCode().equals(branchHead.getShaCode())) {
            System.out.println("Given branch is an ancestor of the "
                    + "current branch.");
        } else if (splitPt.getShaCode().equals(head.getShaCode())) {
            head = branchHead;
            System.out.println("Current branch fast-forwarded.");
        } else {
            valid = true;
        }
        return valid;
    }


    /** Helper method for case 7 of the merge method (merge conflict).
     * HEADFILECODE, BRANCHFILECODE, FILENAME.
     */
    static void mergeFiles(String headFileCode,
                           String branchFileCode, String fileName) {
        try {
            File mergedFile = new File("./" + fileName);
            mergedFile.delete();
            File headFile = new File("./.gitlet/CommitFiles/" + headFileCode);
            FileWriter writer1 = new FileWriter(mergedFile, true);
            if (headFile.length() == 0) {
                writer1.write("<<<<<<< HEAD");
            } else {
                writer1.write("<<<<<<< HEAD" + "\n");
            }
            writer1.close();
            byte[] headFileContents = Utils.readContents(headFile);
            Utils.addContents(mergedFile, headFileContents);

            File branchFile = new File("./.gitlet/CommitFiles/"
                    + branchFileCode);
            FileWriter writer2 = new FileWriter(mergedFile, true);
            if (branchFile.length() == 0) {
                writer2.write("\n" + "=======");
            } else {
                writer2.write("\n" + "=======" + "\n");
            }
            writer2.close();
            
            byte[] branchFileContents = Utils.readContents(branchFile);
            Utils.addContents(mergedFile, branchFileContents);

            FileWriter writer3 = new FileWriter(mergedFile, true);
            writer3.write("\n" + ">>>>>>>");
            writer3.close();
            noConflict = false;
        } catch (IOException e) {
            return;
        }
    }

    /** Helper method that returns the split point SPLITPT between two branches
     * BRANCH1 and BRANCH2. Returns null if there is no split point (no common
     * ancestor). */
    static Commit findSplit(Commit branch1, Commit branch2) {
        Commit copy1 = branch1;
        while (copy1 != null) {
            Commit splitPt = branch2;
            while (splitPt != null) {
                if (splitPt.getShaCode().equals(copy1.getShaCode())) {
                    return splitPt;
                }
                splitPt = splitPt.getParent();
            }
            copy1 = copy1.getParent();
        }
        return null;
    }

    /** Puts the head, tree, and branch in bytes. */
    static void serialize() throws IOException {
        File heads = new File("./.gitlet/HEAD.ser");
        FileOutputStream headIn = new FileOutputStream(heads);
        ObjectOutputStream headOut = new ObjectOutputStream(headIn);
        headOut.writeObject(head);

        File trees = new File("./.gitlet/tree.ser");
        FileOutputStream treeIn = new FileOutputStream(trees);
        ObjectOutputStream treeOut = new ObjectOutputStream(treeIn);
        treeOut.writeObject(tree);

        File branches = new File("./.gitlet/branch.ser");
        FileOutputStream branchIn = new FileOutputStream(branches);
        ObjectOutputStream branchOut = new ObjectOutputStream(branchIn);
        branchOut.writeObject(branch);
    }

    /** Turns the bytes of the head, tree, and branch into objects  . */
    static void deserialize() throws IOException, ClassNotFoundException {
        File heads = new File("./.gitlet/HEAD.ser");
        FileInputStream headIn = new FileInputStream(heads);
        ObjectInputStream headOut = new ObjectInputStream(headIn);
        head = (Commit) headOut.readObject();

        File trees = new File("./.gitlet/tree.ser");
        FileInputStream treeIn = new FileInputStream(trees);
        ObjectInputStream treeOut = new ObjectInputStream(treeIn);
        tree = (Tree) treeOut.readObject();

        File branches = new File("./.gitlet/branch.ser");
        FileInputStream branchIn = new FileInputStream(branches);
        ObjectInputStream branchOut = new ObjectInputStream(branchIn);
        branch = (String) branchOut.readObject();
    }

    /** The arguments passed into the main method. */
    private static String[] _args;

    /** Saved head. */
    private static Commit head;

    /** Saved tree. */
    private static Tree tree;

    /** Current branch. */
    private static String branch;

    /** A Global variable for the functions involved in merging. */
    private static boolean noConflict;
}
