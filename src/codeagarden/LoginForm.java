package codeagarden;

import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** The small "player name + password" form used by NEW GAME and CONTINUE. */
final class LoginForm {

    static final int MAX_NAME_LENGTH = 50;       // same as PLAYER.player_name VARCHAR(50)
    static final int MIN_PASSWORD_LENGTH = 4;

    private final String name;
    private final char[] password;

    private LoginForm(String name, char[] password) {
        this.name = name;
        this.password = password;
    }

    String getName() {
        return name;
    }

    char[] getPassword() {
        return password;
    }

    /** Shows the form until it is filled in correctly. Returns null if the player presses Cancel. */
    static LoginForm ask(Component parent, String title, String hint) {
        JTextField nameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);   // shows dots instead of the letters
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel(hint));
        form.add(new JLabel("Player name:"));
        form.add(nameField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);

        while (true) {
            int answer = JOptionPane.showConfirmDialog(parent, form, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return null;
            }
            String typedName = nameField.getText().trim();
            char[] typedPassword = passwordField.getPassword();
            String problem = findProblem(typedName, typedPassword);
            if (problem == null) {
                return new LoginForm(typedName, typedPassword);
            }
            JOptionPane.showMessageDialog(parent, problem, title, JOptionPane.WARNING_MESSAGE);
        }
    }

    // Returns what is wrong with the typed values, or null if they are fine.
    private static String findProblem(String name, char[] password) {
        if (name.isEmpty()) {
            return "Please type a player name.";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return "The player name can be at most " + MAX_NAME_LENGTH + " characters.";
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return "The password needs at least " + MIN_PASSWORD_LENGTH + " characters.";
        }
        return null;
    }
}
