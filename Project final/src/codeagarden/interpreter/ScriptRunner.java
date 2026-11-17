package codeagarden.interpreter;

import java.util.List;
import javax.swing.SwingUtilities;

/**
 * DESIGN PATTERN: Facade.
 * One simple class in front of the script language (Lexer, Parser, Interpreter).
 * The code panel only calls start(code), togglePause() and stop(); it never touches
 * the Lexer, Parser or Interpreter directly. Those three files belong to the interpreter
 * and did not change when the game moved the code editor into the main window.
 *
 * The player's code runs on its own background thread, so the game window keeps
 * responding while the drone works. Each command waits COMMAND_DELAY_MILLIS so the
 * player can watch the drone move.
 */
public final class ScriptRunner {

    /** What the code panel wants to hear about. Always called on the Swing thread. */
    public interface Listener {
        /** A short message for the status line, e.g. "Running" or "plant(Carrot)". */
        void onStatus(String message);

        /** The script started, paused, resumed or stopped: redraw the Run and Pause buttons. */
        void onStateChanged();
    }

    private static final int COMMAND_DELAY_MILLIS = 300;

    private final DroneController drone;
    private final Listener listener;
    private volatile Interpreter interpreter;   // null = no script running

    /** drone carries out the commands; listener is told what is happening. */
    public ScriptRunner(DroneController drone, Listener listener) {
        this.drone = drone;
        this.listener = listener;
    }

    /** True while a script is running (or paused). */
    public boolean isRunning() {
        return interpreter != null;
    }

    /** True while a running script is paused. */
    public boolean isPaused() {
        Interpreter running = interpreter;
        return running != null && running.paused;
    }

    /**
     * Reads the code and starts it on its own thread. Returns false (and runs nothing) if a
     * script is already running or the code could not be read.
     */
    public boolean start(String code) {
        if (interpreter != null) {
            return false;
        }
        List<Stmt> program;
        try {
            program = new Parser(Lexer.lex(code)).parse();   // text -> words -> statements
        } catch (RuntimeException e) {
            tell("Couldn't read the code. Check the brackets ( ) { } and the ; at line ends.");
            return false;
        }
        Interpreter running = new Interpreter(this::runCommand);
        interpreter = running;
        tellStateChanged();
        tell("Running");
        Thread thread = new Thread(() -> runOnThisThread(running, program), "player-script");
        thread.setDaemon(true);   // never keeps the program alive after the window closes
        thread.start();
        return true;
    }

    /** Pauses a running script, or resumes a paused one. */
    public void togglePause() {
        Interpreter running = interpreter;
        if (running == null) {
            return;
        }
        running.paused = !running.paused;
        tellStateChanged();
        tell(running.paused ? "Paused" : "Running");
    }

    /** Stops the running script (it finishes its current command first). */
    public void stop() {
        Interpreter running = interpreter;
        if (running != null) {
            running.stopped = true;
        }
    }

    // The body of the background thread: runs the whole program, then reports how it ended.
    private void runOnThisThread(Interpreter running, List<Stmt> program) {
        String ending;
        try {
            running.run(program);
            ending = running.stopped ? "Stopped" : "Finished";
        } catch (RuntimeException e) {
            ending = "The code stopped with an error. Check the spelling of names and numbers.";
        }
        interpreter = null;
        tellStateChanged();
        tell(ending);
    }

    // Called by the Interpreter for every command, e.g. plant(Carrot).
    private Object runCommand(String name, List<Object> args) {
        tell(name + "(" + joinArguments(args) + ")");
        Interpreter.sleep(COMMAND_DELAY_MILLIS);   // slow enough to watch the drone
        return drone.run(name, args);
    }

    // Turns [North] into "North" and [1, 2] into "1, 2" for the status line.
    private static String joinArguments(List<Object> args) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(args.get(i));
        }
        return text.toString();
    }

    // The listener lives in the Swing world, so messages are handed to the Swing thread.
    private void tell(String message) {
        SwingUtilities.invokeLater(() -> listener.onStatus(message));
    }

    // Tells the code panel (on the Swing thread) to redraw its Run and Pause buttons.
    private void tellStateChanged() {
        SwingUtilities.invokeLater(listener::onStateChanged);
    }
}
