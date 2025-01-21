import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

public class ServerClient {
    public static final int GAME_NOT_FOUND = 5;
    public static final int NOT_MY_TURN = 6;
    public static final int INVALID_MOVE = 7;
    public static final int FIELD_TAKEN = 8;
    public static Set<Integer> MOVE_BAD_STATUS = Set.of(GAME_NOT_FOUND, NOT_MY_TURN, INVALID_MOVE, FIELD_TAKEN);

    public static final long TIMEOUT = 6500;
    public static final long ZOMBIE_TIMEOUT = 20000;
    public static final String GAME_STATUS_DRAW = "DRAW";
    public static final String GAME_STATUS_OPP_END = "OPP_DISCONNECTED";

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private GameController controller;

    private long lastPing;

    private boolean needConnectionMessage = true;

    public ServerClient(String serverAddress, int port, GameController controller) throws IOException {
        try {
            socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            this.controller = controller;

            this.lastPing = System.currentTimeMillis();

            new Thread(this::listenToServer).start();
            new Thread(this::monitorConnection).start();
        } catch (IOException e) {
            System.err.println("ERR: Server connect");
            throw e;
        }
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public void sendMove( int toX, int toY) {
        if (out.checkError()) {
            System.err.println("ERR: Connection inactive");
            controller.showErrorMessage("Connection inactive");
            System.exit(0);
        }
        System.out.println("SND: " + toX + ";" + toY);
        out.println("MOVE;" + toX + ";" + toY + "\n");
    }

    public void sendLogin(String name) {
        if (out.checkError()) {
            System.err.println("ERR: Connection inactive");
            controller.showErrorMessage("Connection inactive");
            System.exit(0);
        }
        System.out.println("SNDS: Login");
        out.println("LOGIN;" + name + "\n");
    }

    public void sendOppDiscResponse(String response) {
        if (out.checkError()) {
            System.err.println("ERR: Connection inactive");
            controller.showErrorMessage("Connection inactive");
            System.exit(0);
        }
        System.out.println("SNDS: opponent disconnect response");
        out.println("OPP_DISCONNECTED;" + response + "\n");
    }

    public void sendWantGame() {
        if (out.checkError()) {
            System.err.println("ERR: Connection inactive");
            controller.showErrorMessage("Connection inactive");
            System.exit(0);
            return;
        }
        System.out.println("SNDS: Game request\n");
        out.println("WANT_GAME;\n");
    }

    public void sendLogout() {
        if (out.checkError()) {
            System.err.println("ERR: Connection inactive");
            controller.showErrorMessage("Connection inactive");
            System.exit(0);
        }
        System.out.println("SNDS: Logout\n");
        out.println("LOGOUT;\n");
    }

    public void listenToServer() {
        String response;
        try {
            while ((response = in.readLine()) != null) {
                considerResponse(response);
            }
        } catch (IOException e) {
            System.err.println("ERR: Connection inactive (listenToServer)");
            controller.showErrorMessage("Connection inactive");
            System.exit(0);
        }
    }

    private void monitorConnection() {
        while (true) {
            if (System.currentTimeMillis() - this.lastPing > TIMEOUT && this.needConnectionMessage) {
                System.err.println("ERR: Connection inactive (monitorConnection)");
                this.needConnectionMessage = false;
                controller.showConnectionError();
            }

            if (System.currentTimeMillis() - this.lastPing > ZOMBIE_TIMEOUT) {
                System.err.println("ERR: Connection inactive - zombie timeout (monitorConnection)");
                controller.showErrorMessage("Connection inactive");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void considerResponse(String response) {
        String[] parts = response.split(";");
        switch (parts[0]) {
            case "GAME_STATUS": {
                System.out.println("RCV: GAME_STATUS");
                if (parts[1].equals(GAME_STATUS_DRAW)) {
                    new Thread(() -> controller.showResult("DRAW")).start();
                } else if (parts[1].equals(GAME_STATUS_OPP_END)) {
                    new Thread(() -> controller.showResult("OPPONENT DID NOT WANT TO WAIT FOR YOU")).start();
                } else {
                    new Thread(() -> controller.showResult(parts[1].equals(controller.getModel().getMyPlayer().getName()) ? "Winner winner chicken dinner!" : "Better luck next time...")).start();
                }
                break;
            }
            case "LOGIN": {
                System.out.println("RCV: LOGIN_OK");
                controller.getModel().setMyPlayer(new Player(parts[1]));
                break;
            }
            case "WANT_GAME": {
                System.out.println("RCV: WANT_GAME");
                controller.getModel().getMyPlayer().setPlayerChar(parts[1].charAt(0));
                controller.openWaiting();
                break;
            }
            case "START_GAME": {
                System.out.println("RCV: GAME_STARTED");
                controller.getModel().setOpponentPlayer(parts[1], parts[2].charAt(0));
                controller.setMyTurn(parts[3].charAt(0) == '1');
                //if my turn notify player
                if (controller.getMyTurn()) {
                    controller.notification("Your turn!");
                } else {
                    controller.notification("Waiting for opponent move");
                }
                controller.newGame();
                break;
            }
            case "MOVE": {
                System.out.println("RCV: MOVE");
                int status = Integer.parseInt(parts[1]);
                if (MOVE_BAD_STATUS.contains(status)) {
                    controller.notification("Invalid move, try again");
                    System.out.println("Invalid move: " + status);
                    return;
                }
                controller.setMyTurn(false);
                controller.updateBoard(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), controller.getModel().getMyPlayer());
                controller.notification("Waiting for opponent move");
                controller.updateHeader();
                break;
            }
            case "OPP_MOVE": {
                System.out.println("RCV: OPP_MOVE");
                int toX = Integer.parseInt(parts[1]);
                int toY = Integer.parseInt(parts[2]);
                controller.setMyTurn(true);
                controller.updateBoard(toX, toY, controller.getModel().getOpponentPlayer());
                controller.notification("Your turn!");
                controller.updateHeader();
                break;
            }
            case "PING": {
                System.out.println("RCV: PING");

                this.lastPing = System.currentTimeMillis();
                this.needConnectionMessage = true;

                out.println("PONG;\n");
                break;
            }
            case "OPP_DISCONNECTED": {
                System.out.println("RCV: OPP_DISCONNECTED");
                controller.setMyTurn(false);
                controller.repaintBoard();
                new Thread(() -> controller.showOpponentDisconnected()).start();
                break;
            }
            case "RECONNECT": {
                System.out.println("RCV: RECONNECT");
                controller.setMyTurn(parts[2].equals(controller.getModel().getMyPlayer().getName()));
                controller.getModel().updateBoard(parts[1]);
                controller.getModel().setOpponentPlayer(parts[3], parts[4].charAt(0));
                controller.repaintBoard();
                controller.updateHeader();
                break;
            }
            default: {
                System.out.println("Invalid server msg -> close connection" + response);
                controller.showErrorMessage("Invalid server message");
                System.exit(0);
            }
        }
    }
}