import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles all network-related operations for the Reversi game,
 * including sending messages to the server and receiving responses.
 */
public class NetworkClient {

    /**
     * Error code indicating no game was found.
     */
    public static final int ERR_GAME_NOT_FOUND = 5;

    /**
     * Error code indicating it's not the player's turn.
     */
    public static final int ERR_NOT_MY_TURN = 6;

    /**
     * Error code indicating an invalid move occurred.
     */
    public static final int ERR_INVALID_MOVE = 7;

    /**
     * Error code indicating the field is already taken.
     */
    public static final int ERR_FIELD_OCCUPIED = 8;

    /**
     * A set of status codes representing bad move states.
     */
    public static final Set<Integer> UNACCEPTABLE_MOVE_CODES = Set.of(
            ERR_GAME_NOT_FOUND,
            ERR_NOT_MY_TURN,
            ERR_INVALID_MOVE,
            ERR_FIELD_OCCUPIED
    );

    /**
     * Timeout for detecting inactive connections in milliseconds.
     */
    public static final long CONNECTION_TIMEOUT = 6500;

    /**
     * Extended timeout marking the client as a "zombie" after no activity.
     */
    public static final long CONNECTION_ZOMBIE_TIMEOUT = 20000;

    /**
     * A server status indicating the game ended in a draw.
     */
    public static final String STATUS_MSG_DRAW = "DRAW";

    /**
     * A server status indicating the opponent ended the game.
     */
    public static final String STATUS_MSG_OPPONENT_LEFT = "OPP_DISCONNECTED";

    /**
     * The client socket used for communication.
     */
    private Socket networkSocket;

    /**
     * A reader for receiving data from the server.
     */
    private BufferedReader readerStream;

    /**
     * A writer for sending data to the server.
     */
    private PrintWriter writerStream;

    /**
     * The main game controller, responsible for handling game logic.
     */
    private GameController mainController;

    /**
     * Tracks the timestamp of the last received ping message.
     */
    private long timestampLastPing;

    /**
     * A flag indicating whether a connection message is needed.
     */
    private boolean pendingConnectionMessage = true;

    /**
     * Constructs a new NetworkClient, attempts a connection, and starts two background threads:
     * one for listening to the server and one for monitoring connection health.
     *
     * @param serverAddress The server IP address.
     * @param port          The server port.
     * @param controller    The main game controller.
     * @throws IOException If an I/O error occurs when opening the socket.
     */
    public NetworkClient(String serverAddress, int port, GameController controller) throws IOException {
        try {
            networkSocket = new Socket(serverAddress, port);
            readerStream = new BufferedReader(new InputStreamReader(networkSocket.getInputStream()));
            writerStream = new PrintWriter(networkSocket.getOutputStream(), true);

            this.mainController = controller;
            this.timestampLastPing = System.currentTimeMillis();

            new Thread(this::listenToServer).start();
            new Thread(this::monitorConnectionHealth).start();
        } catch (IOException e) {
            System.err.println("ERR: Server connect");
            throw e;
        }
    }

    /**
     * Sends a move to the server, specifying the coordinates for the move.
     *
     * @param toX The x-coordinate.
     * @param toY The y-coordinate.
     */
    public void sendMove(int toX, int toY) {
        if (writerStream.checkError()) {
            System.err.println("ERR: Connection inactive");
            mainController.displayError("Connection inactive");
            System.exit(0);
        }
        System.out.println("SND: " + toX + ";" + toY);
        writerStream.println("MOVE;" + toX + ";" + toY + "\n");
    }

    /**
     * Sends a login request (username) to the server.
     *
     * @param name The name of the player.
     */
    public void requestLogin(String name) {
        if (writerStream.checkError()) {
            System.err.println("ERR: Connection inactive");
            mainController.displayError("Connection inactive");
            System.exit(0);
        }
        System.out.println("SNDS: Login");
        writerStream.println("LOGIN;" + name + "\n");
    }

    /**
     * Sends a response to the server about whether this player wants to wait for the disconnected opponent or not.
     *
     * @param response The player's choice, e.g. "WAIT" or "NOT_WAIT".
     */
    public void sendOppDiscResponse(String response) {
        if (writerStream.checkError()) {
            System.err.println("ERR: Connection inactive");
            mainController.displayError("Connection inactive");
            System.exit(0);
        }
        System.out.println("SNDS: opponent disconnect response");
        writerStream.println("WAIT_REPLY;" + response + "\n");
    }

    /**
     * Requests a new game from the server.
     */
    public void requestNewGame() {
        if (writerStream.checkError()) {
            System.err.println("ERR: Connection inactive");
            mainController.displayError("Connection inactive");
            System.exit(0);
            return;
        }
        System.out.println("SNDS: Game request\n");
        writerStream.println("JOIN_GAME;\n");
    }

    /**
     * Sends a logout message to the server, requesting termination of the session.
     */
    public void sendLogout() {
        if (writerStream.checkError()) {
            System.err.println("ERR: Connection inactive");
            mainController.displayError("Connection inactive");
            System.exit(0);
        }
        System.out.println("SNDS: Logout\n");
        writerStream.println("LOGOUT;\n");
    }

    /**
     * Continuously listens to the server's messages and processes them.
     */
    public void listenToServer() {
        String serverMsg;
        try {
            while ((serverMsg = readerStream.readLine()) != null) {
                interpretServerMessage(serverMsg);
            }
        } catch (IOException e) {
            System.err.println("ERR: Connection inactive (listenToServer)");
            mainController.displayError("Connection inactive");
            System.exit(0);
        }
    }

    /**
     * Monitors the connection at periodic intervals to detect timeouts and
     * trigger notifications or errors in the GameController.
     */
    private void monitorConnectionHealth() {
        while (true) {
            long now = System.currentTimeMillis();
            if (now - this.timestampLastPing > CONNECTION_TIMEOUT && this.pendingConnectionMessage) {
                System.err.println("ERR: Connection inactive (monitorConnection)");
                this.pendingConnectionMessage = false;
                mainController.notifyConnectionIssue();
            }

            if (now - this.timestampLastPing > CONNECTION_ZOMBIE_TIMEOUT) {
                System.err.println("ERR: Connection inactive - zombie timeout (monitorConnection)");
                mainController.displayError("Connection inactive");
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Parses and reacts to a single line of text received from the server.
     *
     * @param response A line of text from the server, containing instructions or status codes.
     */
    private void interpretServerMessage(String response) {
        Map<String, Runnable> commandHandlers = new HashMap<>();
        String[] parts = response.split(";");

        commandHandlers.put("GAME_STATUS", () -> {
            System.out.println("RCV: GAME_STATUS");
            if (parts[1].equals(STATUS_MSG_DRAW)) {
                new Thread(() -> mainController.displayResult("DRAW")).start();
            } else if (parts[1].equals(STATUS_MSG_OPPONENT_LEFT)) {
                new Thread(() -> mainController.displayResult("OPPONENT DID NOT WANT TO WAIT FOR YOU")).start();
            } else {
                new Thread(() -> mainController.displayResult(
                        parts[1].equals(mainController.getModel().getLocalPlayer().getName())
                                ? "Winner winner chicken dinner!"
                                : "Better luck next time..."
                )).start();
            }
        });

        commandHandlers.put("LOGIN", () -> {
            System.out.println("RCV: LOGIN_OK");
            mainController.getModel().setLocalPlayer(new Player(parts[1]));
        });

        commandHandlers.put("JOIN_GAME", () -> {
            System.out.println("RCV: JOIN_GAME");
            mainController.getModel().getLocalPlayer().setPlayerChar(parts[1].charAt(0));
            mainController.displayWaitingScreen();
        });

        commandHandlers.put("START_GAME", () -> {
            System.out.println("RCV: GAME_STARTED");
            mainController.getModel().setRemotePlayer(parts[1], parts[2].charAt(0));
            mainController.setMyTurn(parts[3].charAt(0) == '1');
            if (mainController.isMyTurn()) {
                mainController.displayNotification("Your turn!");
            } else {
                mainController.displayNotification("Waiting for opponent move");
            }
            mainController.startNewGame();
        });

        commandHandlers.put("MOVE", () -> {
            System.out.println("RCV: MOVE");
            int status = Integer.parseInt(parts[1]);
            if (UNACCEPTABLE_MOVE_CODES.contains(status)) {
                mainController.displayNotification("Invalid move, try again");
                System.out.println("Invalid move: " + status);
                return;
            }
            mainController.setMyTurn(false);
            mainController.refreshGameBoard(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                    mainController.getModel().getLocalPlayer());
            mainController.displayNotification("Waiting for opponent move");
            mainController.refreshHeader();
        });

        commandHandlers.put("OPP_MOVE", () -> {
            System.out.println("RCV: OPP_MOVE");
            int toX = Integer.parseInt(parts[1]);
            int toY = Integer.parseInt(parts[2]);
            mainController.setMyTurn(true);
            mainController.refreshGameBoard(toX, toY, mainController.getModel().getRemotePlayer());
            mainController.displayNotification("Your turn!");
            mainController.refreshHeader();
        });

        commandHandlers.put("PING", () -> {
            System.out.println("RCV: PING");
            this.timestampLastPing = System.currentTimeMillis();
            this.pendingConnectionMessage = true;
            writerStream.println("PONG;\n");
        });

        commandHandlers.put("OPP_DISCONNECTED", () -> {
            System.out.println("RCV: OPP_DISCONNECTED");
            mainController.setMyTurn(false);
            mainController.refreshGameView();
            new Thread(() -> mainController.notifyDisconnection()).start();
        });

        commandHandlers.put("RECONNECT", () -> {
            System.out.println("RCV: RECONNECT");
            mainController.setMyTurn(parts[2].equals(mainController.getModel().getLocalPlayer().getName()));
            mainController.getModel().updateBoard(parts[1]);
            mainController.getModel().setRemotePlayer(parts[3], parts[4].charAt(0));
            mainController.refreshGameView();
            mainController.refreshHeader();
        });

        // Process the command using the map
        Runnable commandHandler = commandHandlers.get(parts[0]);
        if (commandHandler != null) {
            commandHandler.run();
        } else {
            System.out.println("Invalid server msg -> close connection" + response);
            mainController.displayError("Invalid server message");
            System.exit(0);
        }
    }

}
