package codeagarden;

import codeagarden.ui.MainMenu;
import javax.swing.SwingUtilities;

/** The only entry point of the game. It just opens the main menu. */
public class Main {

    /** Starts the program: builds the main menu on the Swing (Event Dispatch) thread. */
    public static void main(String[] args) {
        // Swing must be built on the Event Dispatch Thread (the UI thread).
        SwingUtilities.invokeLater(MainMenu::new);
    }
}
