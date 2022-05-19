package gitlet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Zwea Htet
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            if (args.length == 0) {
                throw Utils.error("Please enter a command.");
            }
            switch (args[0]) {
            case "init":
                Commands.init(args);
                break;
            case "add":
                Commands.add(args);
                break;
            case "commit":
                Commands.commit(args);
                break;
            case "log":
                Commands.log(args);
                break;
            case "global-log":
                Commands.globalLog(args);
                break;
            case "checkout":
                Commands.checkout(args);
                break;
            case "rm":
                Commands.remove(args);
                break;
            case "find":
                Commands.find(args);
                break;
            case "status":
                Commands.status(args);
                break;
            case "branch":
                Commands.branch(args);
                break;
            case "rm-branch":
                Commands.removeBranch(args);
                break;
            case "reset":
                Commands.reset(args);
                break;
            case "merge":
                Commands.merge(args);
                break;
            default:
                throw Utils.error("No command with that name exists.");
            }
        } catch (Exception err) {
            exitWithError(err.getMessage());
        }
        System.exit(0);
    }

    /**
     * Prints out MESSAGE and exits with error code 0.
     * Note:
     *     The functionality for erroring/exit codes is different within Gitlet
     *     so DO NOT use this as a reference.
     *     Refer to the spec for more information.
     * @param message message to print
     */
    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }
}
