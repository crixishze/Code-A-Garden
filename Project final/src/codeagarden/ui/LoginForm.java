package codeagarden.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * The "player name + password" window used by NEW GAME and CONTINUE, in the game's wooden style
 * (UIFactory and Theme). A problem such as a short password is shown in red inside the window,
 * which stays open until the input is fine or the player presses Cancel.
 */
final class LoginForm {

    static final int MAX_NAME_LENGTH = 50;       // same as PLAYER.player_name VARCHAR(50)
    static final int MIN_PASSWORD_LENGTH = 4;

    private static final Dimension FIELD_SIZE = new Dimension(360, 34);

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

    /**
     * Shows the form until it is filled in correctly. okText is the main button ("Start" or "Log In").
     * Returns null if the player presses Cancel or closes the window.
     */
    static LoginForm ask(Window owner, String title, String hint, String okText) {
        JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
        LoginForm[] result = {null};   // an array so the button actions below can change it

        JTextField nameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();   // shows dots instead of the letters
        nameField.setFont(Theme.PIXEL.deriveFont(16f));
        passwordField.setFont(Theme.PIXEL.deriveFont(16f));
        nameField.setPreferredSize(FIELD_SIZE);
        passwordField.setPreferredSize(FIELD_SIZE);
        JLabel problem = UIFactory.createLabel(" ", 14f, Theme.BAD);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.setOpaque(false);
        form.add(UIFactory.createLabel(hint, 14f, Theme.PARCHMENT));
        form.add(UIFactory.createLabel("Player name:", 16f, Theme.PARCHMENT));
        form.add(nameField);
        form.add(UIFactory.createLabel("Password:", 16f, Theme.PARCHMENT));
        form.add(passwordField);
        form.add(problem);

        JButton ok = UIFactory.createButton(okText, 140, 44, 16f);
        JButton cancel = UIFactory.createButton("Cancel", 140, 44, 16f);
        ok.addActionListener(e -> {
            String typedName = nameField.getText().trim();
            char[] typedPassword = passwordField.getPassword();
            String whatIsWrong = findProblem(typedName, typedPassword);
            if (whatIsWrong == null) {
                result[0] = new LoginForm(typedName, typedPassword);
                dialog.dispose();
            } else {
                problem.setText(whatIsWrong);   // stay open and say what to fix
            }
        });
        cancel.addActionListener(e -> dialog.dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        buttons.add(ok);
        buttons.add(cancel);

        JPanel screen = UIFactory.createScreenPanel(new BorderLayout(0, 14), 18, 24, 18, 24);
        screen.add(UIFactory.createTitle(title.toUpperCase()), BorderLayout.NORTH);
        screen.add(form, BorderLayout.CENTER);
        screen.add(buttons, BorderLayout.SOUTH);
        form.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        dialog.setContentPane(screen);
        dialog.getRootPane().setDefaultButton(ok);   // Enter = OK
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);   // modal: waits here until OK (with good input) or Cancel
        return result[0];
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
