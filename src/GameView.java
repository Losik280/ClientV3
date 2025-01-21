import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

/**
 * Represents the main graphical user interface for the Reversi game.
 * This class extends JFrame and handles all UI-related operations.
 */
public class GameView extends JFrame {

    /**
     * The maximum number of characters allowed for a player's name.
     */
    public static final int PLAYER_NAME_LENGTH = 20;

    /**
     * The size (rows and columns) of the Reversi game board.
     */
    public static final int GAME_BOARD_SIZE = 4;

    /**
     * A 2D array (matrix) of buttons that comprise the Reversi board.
     */
    private final JButton[][] boardButtons = new JButton[GAME_BOARD_SIZE][GAME_BOARD_SIZE];

    /**
     * The main game controller. It oversees the game logic and network operations.
     */
    private GameController mainController;

    /**
     * A label that displays status messages or tips for the player.
     */
    private final JLabel lblStatus;

    /**
     * A panel displaying the login form.
     */
    private JPanel panelLogin;

    /**
     * A panel displayed while waiting for an opponent to join.
     */
    private JPanel panelWaiting;

    /**
     * A panel displaying the actual Reversi board during the game.
     */
    private JPanel panelGame;

    /**
     * A panel displaying information (e.g., players’ names) in the header.
     */
    private JPanel panelHeader;

    /**
     * A text field for entering the player's name.
     */
    private JTextField fldName;

    /**
     * A text field for entering the server IP address.
     */
    private JTextField fldServer;

    /**
     * A text field for entering the server port number.
     */
    private JTextField fldPort;

    /**
     * Constructs the game view, sets up initial properties, and displays the main window.
     *
     * @param controller The main game controller (logic and network).
     */
    public GameView(GameController controller) {
        this.mainController = controller;
        setTitle("Reversi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(800, 800);

        lblStatus = new JLabel("Welcome to Reversi", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Arial", Font.BOLD, 15));
        lblStatus.setBackground(Color.GRAY);

        add(lblStatus, BorderLayout.SOUTH);
        setVisible(true);
    }

    /**
     * Custom handling of the window closing event to confirm if the user truly wants to exit
     * when a network connection is active.
     *
     * @param e The WindowEvent triggered on closing.
     */
    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            if (mainController.getNetworkClient() != null) {
                if (!confirmCloseRequest()) {
                    return;
                }
            }
        }
        super.processWindowEvent(e);
    }

    /**
     * Initializes and displays the game board by creating a grid of buttons.
     */
    public void initializeBoard() {
        if (panelWaiting != null) {
            remove(panelWaiting);
        }
        if (panelLogin != null) {
            remove(panelLogin);
        }
        refreshHeaderInfo();

        panelGame = new JPanel(new GridLayout(GAME_BOARD_SIZE, GAME_BOARD_SIZE));
        for (int row = 0; row < GAME_BOARD_SIZE; row++) {
            for (int col = 0; col < GAME_BOARD_SIZE; col++) {
                boardButtons[row][col] = new JButton();
                boardButtons[row][col].setFont(new Font("Arial", Font.PLAIN, 60));

                final int x = col;
                final int y = row;
                boardButtons[row][col].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        mainController.sendPlayerMove(x, y);
                    }
                });
                panelGame.add(boardButtons[row][col]);
            }
        }
        add(panelGame, BorderLayout.CENTER);
        setVisible(true);
    }

    /**
     * Assigns a new game controller.
     *
     * @param controller The new game controller to be used by this view.
     */
    public void setController(GameController controller) {
        this.mainController = controller;
    }

    /**
     * Displays a login panel where the user can input credentials (name, server IP, port).
     */
    public void displayLoginScreen() {
        panelLogin = new JPanel(new BorderLayout());
        JPanel loginFormPanel = new JPanel();
        int width = (int) (getWidth() * 0.25);
        int height = (int) (getHeight() * 0.15);

        loginFormPanel.setLayout(new GridLayout(8, 1, 10, 20));
        loginFormPanel.setBorder(BorderFactory.createEmptyBorder(height, width, height, width));

        // Player name
        loginFormPanel.add(new JLabel("Name:"));
        fldName = new JTextField("Player" + (int) (Math.random() * 1000));
        loginFormPanel.add(fldName);
        loginFormPanel.add(new JLabel());

        // Server IP
        loginFormPanel.add(new JLabel("Server IP:"));
        fldServer = new JTextField("172.17.38.255");
        loginFormPanel.add(fldServer);
        loginFormPanel.add(new JLabel());

        // Server port
        loginFormPanel.add(new JLabel("Port:"));
        fldPort = new JTextField("10000");
        loginFormPanel.add(fldPort);

        // Connect button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 40, 0));

        JButton btnConnect = new JButton("Connect");
        btnConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                fldName.setText(fldName.getText().trim());
                fldPort.setText(fldPort.getText().trim());
                fldServer.setText(fldServer.getText().trim());
                if (isLoginValid()) {
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            try {
                                mainController.setNetworkClient(
                                        new NetworkClient(fldServer.getText(),
                                                Integer.parseInt(fldPort.getText()),
                                                mainController)
                                );
                                mainController.sendLogin(fldName.getText());
                                mainController.requestGameStart();
                            } catch (Exception ex) {
                                SwingUtilities.invokeLater(() -> displayInformationDialog("Error: Connection failed"));
                            }
                            return null;
                        }
                    }.execute();
                }
            }
        });
        buttonPanel.add(btnConnect);

        loginFormPanel.setBackground(Color.GRAY);
        buttonPanel.setBackground(Color.GRAY);
        panelLogin.add(loginFormPanel, BorderLayout.CENTER);
        panelLogin.add(buttonPanel, BorderLayout.SOUTH);
        panelLogin.setBackground(Color.GRAY);
        setBackground(Color.GRAY);

        add(panelLogin, BorderLayout.CENTER);
        setVisible(true);
    }

    /**
     * Displays a panel informing the user that the system is waiting for an opponent.
     */
    public void displayWaitingScreen() {
        if (panelLogin != null) {
            remove(panelLogin);
        }
        if (panelGame != null) {
            remove(panelGame);
        }
        if (panelHeader != null) {
            remove(panelHeader);
        }

        panelWaiting = new JPanel(new BorderLayout());
        panelWaiting.setBackground(Color.GRAY);
        panelWaiting.add(new JLabel(
                "<html><span style='font-size:15px'>Stay tuned, waiting for another player...</span></html>",
                SwingConstants.CENTER), BorderLayout.CENTER);
        add(panelWaiting, BorderLayout.CENTER);
        panelWaiting.repaint();
        setVisible(true);
    }

    /**
     * Displays the result of the game in a dialog and offers the user the choice to play again or quit.
     *
     * @param result A string describing the final state of the game.
     */
    public void displayGameResultDialog(String result) {
        int response = JOptionPane.showOptionDialog(
                panelGame,
                result,
                "GAME RESULT",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"Play Again", "Quit"},
                "Play Again"
        );
        if (response == 0) {
            mainController.requestGameStart();
        } else {
            mainController.sendLogout();
            System.exit(0);
        }
        panelGame.revalidate();
        panelGame.repaint();
    }

    /**
     * Shows a dialog indicating the opponent has disconnected, giving options to wait or not.
     */
    public void displayOpponentLeft() {
        int response = JOptionPane.showOptionDialog(
                panelGame,
                "Do you want to wait for an opponent?",
                "Opponent Disconnected",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"Yes", "No"},
                "Yes"
        );
        if (response == 0) {
            mainController.notifyOpponentDisconnection("WAIT");
        } else {
            mainController.notifyOpponentDisconnection("NOT_WAIT");
        }
    }

    /**
     * Asks the user for confirmation to close the application if a network connection is established.
     *
     * @return true if the user wants to close, false otherwise.
     */
    public boolean confirmCloseRequest() {
        int response = JOptionPane.showOptionDialog(
                panelGame,
                "Do you really want to leave?",
                "",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"Yes", "No"},
                "No"
        );
        if (response == 0) {
            mainController.sendLogout();
            return true;
        }
        return false;
    }

    /**
     * Shows a dialog indicating a connection error and attempts to reconnect.
     */
    public void notifyConnectionError() {
        JOptionPane.showOptionDialog(
                panelGame,
                "Connection lost, trying to reconnect...",
                "Connection Failure",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"OK"},
                "OK"
        );
    }

    /**
     * Displays an error dialog with the given message, then closes the application.
     *
     * @param message A short description of the error.
     */
    public void displayErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        System.exit(0);
    }

    /**
     * Displays an informational dialog with the given message.
     *
     * @param message A short informational text.
     */
    public void displayInformationDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Updates the board's colors and clickability according to the current model state.
     *
     * @param model        The main game model providing the current board state.
     * @param isClickable  If true, the board squares are enabled for user interaction.
     */
    public void updateBoard(GameModel model, boolean isClickable) {
        char[][] board = model.getGameBoard();
        for (int row = 0; row < boardButtons.length; row++) {
            for (int col = 0; col < boardButtons[row].length; col++) {
                char cell = board[row][col];
                if (cell == 'R') {
                    boardButtons[row][col].setBackground(new Color(186, 0, 0));
                } else if (cell == 'B') {
                    boardButtons[row][col].setBackground(new Color(0, 0, 220));
                } else {
                    boardButtons[row][col].setBackground(Color.GRAY);
                }
                boardButtons[row][col].setEnabled(isClickable);
            }
        }
        panelGame.revalidate();
        panelGame.repaint();
    }

    /**
     * Updates the header panel to show the names of the local and remote players.
     */
    public void refreshHeaderInfo() {
        if (panelHeader != null) {
            remove(panelHeader);
            panelHeader = null;
        }

        panelHeader = new JPanel(new GridLayout(1, 4));

        JLabel lblYourSide = new JLabel("You:", SwingConstants.RIGHT);
        lblYourSide.setFont(new Font("Arial", Font.BOLD, 20));
        panelHeader.add(lblYourSide);

        JLabel lblLocalName = new JLabel(mainController.getModel().getLocalPlayer().getName().trim(), SwingConstants.LEFT);
        lblLocalName.setFont(new Font("Arial", Font.PLAIN, 20));
        panelHeader.add(lblLocalName);

        JLabel lblOpponentSide = new JLabel("Opponent:", SwingConstants.RIGHT);
        lblOpponentSide.setFont(new Font("Arial", Font.BOLD, 20));
        panelHeader.add(lblOpponentSide);

        JLabel lblRemoteName = new JLabel(mainController.getModel().getRemotePlayer().getName().trim(), SwingConstants.LEFT);
        lblRemoteName.setFont(new Font("Arial", Font.PLAIN, 20));
        panelHeader.add(lblRemoteName);

        char localChar = mainController.getModel().getLocalPlayer().getPlayerChar();
        if (localChar == 'R') {
            lblLocalName.setForeground(Color.RED);
            lblRemoteName.setForeground(Color.BLUE);
        } else {
            lblLocalName.setForeground(Color.BLUE);
            lblRemoteName.setForeground(Color.RED);
        }

        add(panelHeader, BorderLayout.NORTH);
        panelHeader.revalidate();
        panelHeader.repaint();
        setVisible(true);
    }

    /**
     * Validates that the user's login information is correct and well-formed.
     *
     * @return True if valid, false otherwise.
     */
    private boolean isLoginValid() {
        String name = fldName.getText();
        String serverAddress = fldServer.getText();
        String port = fldPort.getText();

        if (name.isEmpty() || serverAddress.isEmpty() || port.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            Integer.parseInt(port);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Port must be a number!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (name.length() > PLAYER_NAME_LENGTH) {
            JOptionPane.showMessageDialog(this, "Name is too long!", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        while (fldName.getText().length() < PLAYER_NAME_LENGTH) {
            fldName.setText(fldName.getText() + " ");
        }
        return true;
    }

    /**
     * Updates the status label at the bottom of the window with the specified text.
     *
     * @param text A message to be displayed to the user.
     */
    public void setStatusMessage(String text) {
        lblStatus.setBackground(Color.LIGHT_GRAY);
        lblStatus.setText(text);
    }
}
