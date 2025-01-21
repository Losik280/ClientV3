import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

/**
 * Class for graphical user interface of Reversi game.
 */
public class GameView extends JFrame {
    public static final int PLAYER_NAME_LENGTH = 20;
    public static final int GAME_BOARD_SIZE = 4;

    /**
     * Array of buttons for the game board.
     */
    private final JButton[][] buttons = new JButton[GAME_BOARD_SIZE][GAME_BOARD_SIZE];

    /**
     * The controller for the game.
     */
    private GameController controller;

    private final JLabel statusLabel;


    /**
     * The text field for the player's name (login)
     */
    private JPanel loginPanel;

    /**
     * JPanel for waiting for the opponent
     */
    private JPanel waitingPanel;

    /**
     * JPanel for the game
     */
    private JPanel gamePanel;

    /**
     * JPanel for the header
     */
    private JPanel headerPanel;

    private JTextField nameField, serverField, portField;

    public Position fromPos;



    /**
     * Constructor of the ReversiView class.
     */
    public GameView(GameController controller) {
        this.controller = controller;
        setTitle("Reversi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(800, 800);

        statusLabel = new JLabel("Welcome to Reversi", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 15));
        statusLabel.setBackground(Color.gray);

        add(statusLabel, BorderLayout.SOUTH);

        setVisible(true);
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            if (controller.getNetworkClient() != null) {
                if (!wantClose()) {
                    return;
                }
            }
        }
        super.processWindowEvent(e);
    }

    /**
     * Initializes the game board.
     */
    public void initializeBoard() {
        if (waitingPanel != null) {
            remove(waitingPanel);
//            waitingPanel = null;
        }
        if (loginPanel != null) {
            remove(loginPanel);
//            loginPanel = null;
        }

        updateHeader();

        gamePanel = new JPanel();
        // show game board
        gamePanel.setLayout(new GridLayout(GAME_BOARD_SIZE, GAME_BOARD_SIZE));
        for (int i = 0; i < GAME_BOARD_SIZE; i++) {
            for (int j = 0; j < GAME_BOARD_SIZE; j++) {
                buttons[i][j] = new JButton();  // all buttons are empty at the beginning
                buttons[i][j].setFont(new Font("Arial", Font.PLAIN, 60));
                final int x = j;
                final int y = i;

                // Listener for the button
                buttons[i][j].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {

                            controller.sendPlayerMove(x, y);
                    }
                });
                gamePanel.add(buttons[i][j]);  // Add the button to the frame
            }
        }


        add(gamePanel, BorderLayout.CENTER);
        setVisible(true);  // Show the game board
    }

    /**
     * Resets the game board.
     */
    public void resetBoard() {
        for (int i = 0; i < buttons.length; i++) {
            for (int j = 0; j < buttons[i].length; j++) {
                buttons[i][j].setText(" ");
                buttons[i][j].setEnabled(true);
            }
        }
        repaint();
    }

    /**
     * Sets the controller for the game.
     *
     * @param controller The controller for the game.
     */
    public void setController(GameController controller) {
        this.controller = controller;
    }

    /**
     * Shows the login form.
     */
    public void showLoginPanel() {
        loginPanel = new JPanel(new BorderLayout());
//        loginPanel = new JPanel();

        // JPanel for login form
        JPanel logForm = new JPanel();
        int width = (int) (getWidth() * 0.25);
        int height = (int) (getHeight() * 0.15);//0.33);

        // set layout for login form
        logForm.setLayout(new GridLayout(8, 1, 10, 20));
        logForm.setBorder(BorderFactory.createEmptyBorder(height, width, height, width));

        // Form fields
        logForm.add(new JLabel("Name:"), BorderLayout.SOUTH);
        nameField = new JTextField("Player"+ (int)(Math.random()*1000));
        logForm.add(nameField,BorderLayout.NORTH);
        logForm.add(new JLabel());

        logForm.add(new JLabel("Server IP:"), BorderLayout.SOUTH);
        serverField = new JTextField("172.17.38.255");
        logForm.add(serverField,BorderLayout.NORTH);
        logForm.add(new JLabel());


        logForm.add(new JLabel("Port:"), BorderLayout.SOUTH);
        portField = new JTextField("10000");
        logForm.add(portField, BorderLayout.NORTH);

        // JPanel for button
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)); // Centrovaný layout pro tlačítko
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 40, 0));

        JButton connectButton = new JButton("Connect");

        connectButton.addActionListener(e -> {
            nameField.setText(nameField.getText().trim());
            portField.setText(portField.getText().trim());
            serverField.setText(serverField.getText().trim());
            if (validateLogin()) {
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        try {
                            controller.setNetworkClient(new NetworkClient(serverField.getText(), Integer.parseInt(portField.getText()), controller));
                            controller.sendLogin(nameField.getText());
                            controller.requestGameStart();
                        } catch (Exception ex) {
                            SwingUtilities.invokeLater(() -> showInfoMessage("Error: Connection failed"));
                        }
                        return null;
                    }
                }.execute();
            }
        });

        buttonPanel.add(connectButton);

        logForm.setBackground(Color.gray);
        buttonPanel.setBackground(Color.gray);
        loginPanel.add(logForm, BorderLayout.CENTER);
        loginPanel.add(buttonPanel, BorderLayout.SOUTH);
        loginPanel.setBackground(Color.gray);
        setBackground(Color.gray);

        add(loginPanel, BorderLayout.CENTER);
//        revalidate();
//        repaint();
        setVisible(true);
    }

    /**
     * Shows the waiting panel.
     */
    public void showWaitingPanel() {
        if (loginPanel != null) {
            remove(loginPanel);
        }
        if (gamePanel != null) {
            remove(gamePanel);
        }
        if (headerPanel != null) {
            remove(headerPanel);
        }

        waitingPanel = new JPanel(new BorderLayout());
        waitingPanel.setBackground(Color.gray);
        waitingPanel.add(new JLabel("<html><span style='font-size:15px'>Stay tuned, waiting for someone else to start playing.</span></html>", SwingConstants.CENTER), BorderLayout.CENTER);
        add(waitingPanel, BorderLayout.CENTER);
        waitingPanel.repaint();

        setVisible(true);
    }

    /**
     * Shows the result of the game.
     *
     * @param result The result of the game.
     */
    public void showGameResult(String result) {
        int response = JOptionPane.showOptionDialog(gamePanel, result, "GAME RESULT", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[]{"Play Again", "Quit"}, "Play Again");
        if (response == 0) {
            controller.requestGameStart();
        } else {
            controller.sendLogout();
            System.exit(0);
        }

        gamePanel.revalidate();
        gamePanel.repaint();
    }

    /**
     * Shows the Option dialog for message that the opponent has disconnected.
     */
    public void showOpponentDisconnected() {
        int response = JOptionPane.showOptionDialog(gamePanel, "Do you want to wait for opponent?", "Opponent disconnected", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[]{"Yes", "No"}, "Yes");
        if (response == 0) {
            controller.notifyOpponentDisconnection("WAIT");
        } else {
            controller.notifyOpponentDisconnection("NOT_WAIT");
        }
    }

    /**
     * Shows the Option dialog in case that user want to leave from existing connection
     */
    public boolean wantClose() {
        int response = JOptionPane.showOptionDialog(gamePanel, "Do you really want to leave?", "", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[]{"Yes", "No"}, "No");
        if (response == 0) {
            controller.sendLogout();
            return true;
        }
        return false;
    }

    /**
     * Shows the Option dialog in case of connection error.
     */
    public void showConnectionError() {
        int response = JOptionPane.showOptionDialog(gamePanel, "Connection lost, trying to reconnect...", "Connection failure", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[]{"OK"}, "OK");
    }

    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        // close application
        System.exit(0);
    }

    public void showInfoMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Updates the game board with the player's move according to the model.
     *
     * @param model       The model of the game.
     * @param isClickable True if the button is clickable, false otherwise.
     */
    public void updateBoard(GameModel model, boolean isClickable) {
        char[][] board = model.getBoard();
        for (int i = 0; i < buttons.length; i++) {
            for (int j = 0; j < buttons[i].length; j++) {
                char cell = board[i][j];
                if (cell == 'R') {
                    buttons[i][j].setBackground(new Color(186,0,0));
                } else if (cell == 'B') {
                    buttons[i][j].setBackground(new Color(0,0,220));
                } else {
                    buttons[i][j].setBackground(Color.gray); // Reset to default color
                }
                buttons[i][j].setEnabled(isClickable);  // Enable the button if it is empty
                System.out.print(" " + (board[i][j] == ' ' ? "N" : board[i][j]));
            }
            System.out.println();
        }
        gamePanel.revalidate();
        gamePanel.repaint();
    }

    public void updateHeader() {
        if (headerPanel != null) {
            remove(headerPanel);
            headerPanel = null;
        }

        headerPanel = new JPanel(new GridLayout(1, 4));

        JLabel header1 = new JLabel("You:", SwingConstants.RIGHT);
        header1.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(header1);

        // show header (names of the players)
        JLabel player1 = new JLabel(controller.getModel().getMyPlayer().getName().trim(), SwingConstants.LEFT);
        player1.setFont(new Font("Arial", Font.PLAIN, 20));
        headerPanel.add(player1);





        JLabel header2 = new JLabel("Opponent:", SwingConstants.RIGHT);
        header2.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(header2);

        JLabel player2 = new JLabel(controller.getModel().getOpponentPlayer().getName().trim(), SwingConstants.LEFT);
        player2.setFont(new Font("Arial", Font.PLAIN, 20));
        headerPanel.add(player2);


        //get client colour
        char playerChar = controller.getModel().getMyPlayer().getPlayerChar();
        if (playerChar == 'R') {
            player1.setForeground(Color.RED);
            player2.setForeground(Color.BLUE);
        } else {
            player1.setForeground(Color.BLUE);
            player2.setForeground(Color.RED);
        }

        add(headerPanel, BorderLayout.NORTH);
        headerPanel.revalidate();
        headerPanel.repaint();
        setVisible(true);
    }

    /**
     * Handles the login of the player.
     */
    private boolean validateLogin() {
        String name = nameField.getText();
        String serverAddress = serverField.getText();
        String port = portField.getText();

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

        while (nameField.getText().length() < PLAYER_NAME_LENGTH) {
            nameField.setText(nameField.getText() + " ");
        }
        return true;
    }

    public void updateLabel(String text) {
        statusLabel.setBackground(Color.lightGray);
        statusLabel.setText(text);
    }
}