package gitlet;
import java.util.LinkedHashMap;
import java.io.Serializable;

/** Tree class for Gitlet, the mini version-control system.
 *  @author Yonas/Juno
 */
public class Tree implements Serializable {

    /** Creates a new Tree object given a BRANCH, and INITCOMMIT. */
    Tree(String branch, Commit initCommit) {
        branches = new LinkedHashMap<String, String>();
        branches.put(branch, initCommit.getShaCode());
    }

    /** Takes in the name of the new branch BRANCH and adds it to 
     * the branches hashset where key = BRANCH, and val = HEAD. */
    public void makeBranch(String branch, Commit head) {
        branches.put(branch, head.getShaCode());
    }

    /** Deletes BRANCH from branches. */
    public void deleteBranch(String branch) {
        branches.remove(branch);
    }

    /** Accessor method for the branches. */
    public LinkedHashMap<String, String> getBranches() {
        return branches;
    }

    /** HashMap that keeps track of all of the branches and their pointers.
     * The key is the branch name, and the value is a commit's sha1. Will be
     * useful for checking out a branch. */
    private LinkedHashMap<String, String> branches;
}
