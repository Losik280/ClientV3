/**
 * Controller for Reversi game (MVC pattern).
 */
public class GameController implements Runnable {
    /**
     * The model of the game.
     */
    private final GameModel model;

    /**
     * Class for Graphical User Interface.
     */
    private final GameView view;

    /**
     * The client for communication with the server.
     */
    private ServerClient serverClient;

    private boolean myTurn = false;

    public Thread controllerThread;

    /**
     * Class constructor
     */
    public GameController() {
        this.model = new GameModel(myTurn);
        this.view = new GameView(this);
        view.setController(this);
        controllerThread = new Thread(this);
        controllerThread.start();
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
    }

    public boolean getMyTurn() {
        return myTurn;
    }

    public ServerClient getServerClient() {
        return serverClient;
    }

    /**
     * Getter for the model.
     *
     * @return The model of the game.
     */
    public GameModel getModel() {
        return model;
    }

    public void setServerClient(ServerClient serverClient) {
        this.serverClient = serverClient;
    }

    public void openWaiting() {
        view.showWaitingPanel();
    }

    public void openLogin() {
        view.showLoginPanel();
        while (model.getMyPlayer() == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Starts a new game.
     */
    public void newGame() {
        view.initializeBoard();
        model.resetBoard(myTurn);
        view.updateBoard(model, myTurn);
    }

    public void sendPlayerMove( int toX, int toY) {
        serverClient.sendMove( toX, toY);
    }

    public void sendOppDiscResponse(String response) {
        serverClient.sendOppDiscResponse(response);
    }

    public void sendLogin(String playerName) {
        serverClient.sendLogin(playerName);
    }

    public void sendLogout() {
        serverClient.sendLogout();
    }

    public void sendWantGame() {
        serverClient.sendWantGame();
    }

    public void showResult(String result) {
        view.showGameResult(result);
    }

    public void showOpponentDisconnected() {
        view.showOpponentDisconnected();
    }

    public void showConnectionError() {
        view.showConnectionError();
    }

    /**
     * Updates the game board with the player's move.
     *
     * @param player The player's name.
     */
    public void updateBoard(int to_x, int to_y, Player player) {
        model.updateBoard(to_x, to_y, player.getPlayerChar());
        view.updateBoard(model, myTurn);
    }

    public void repaintBoard() {
        view.updateBoard(model, myTurn);
    }

    public void updateHeader() {
        view.updateHeader();
    }

    public void showErrorMessage(String message) {
        view.showErrorMessage(message);
    }

    @Override
    public void run() {
        openLogin();
    }

    public void notification(String s) {
        view.updateLabel(s);
    }
}