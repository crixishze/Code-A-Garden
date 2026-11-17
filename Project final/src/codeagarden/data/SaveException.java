package codeagarden.data;

/**
 * Something went wrong while saving or loading. The message is written for the player to read.
 * isConnectionProblem() tells the menu whether the database itself could not be used (then the
 * game can still be played without saving) or whether it was a normal answer like "wrong password".
 */
public class SaveException extends Exception {

    private final boolean connectionProblem;

    /** A normal answer such as "wrong password" (not a connection problem). */
    public SaveException(String message) {
        super(message);
        this.connectionProblem = false;
    }

    /** A problem with the original technical error kept as the cause. */
    public SaveException(String message, boolean connectionProblem, Throwable cause) {
        super(message, cause);
        this.connectionProblem = connectionProblem;
    }

    /** True if the database could not be used at all (missing, stopped, wrong settings). */
    public boolean isConnectionProblem() {
        return connectionProblem;
    }
}
