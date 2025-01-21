/**
 * Reversi game controller based on MVC design pattern.
 */
public class GameController implements Runnable {
    /**
     * The core model of the game.
     */
    private final GameModel model;

    /**
     * User interface for the game.
     */
    private final GameView view;

    /**
     * Communication client for server interaction.
     */
    private NetworkClient networkClient;

    private boolean isActivePlayer = false;

    public Thread ctrlThread;

    /**
     * Initialize controller components.
     */
    public GameController() {
        this.model = new GameModel(isActivePlayer);
        this.view = new GameView(this);
        view.setController(this);
        ctrlThread = new Thread(this);
        ctrlThread.start();
    }

    public void setMyTurn(boolean turn) {
        this.isActivePlayer = turn;
    }

    public boolean isMyTurn() {
        return this.isActivePlayer;
    }

    public NetworkClient getNetworkClient() {
        return this.networkClient;
    }

    /**
     * Retrieves the game model.
     *
     * @return current game model.
     */
    public GameModel getModel() {
        return this.model;
    }

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }



    /**
     * Initiates a new game session.
     */
    public void startNewGame() {
        view.initializeBoard();
        model.resetBoard(isActivePlayer);
        view.updateBoard(model, isActivePlayer);
    }

    public void sendPlayerMove(int destX, int destY) {
        networkClient.sendMove(destX, destY);
    }

    public void notifyOpponentDisconnection(String response) {
        networkClient.sendOppDiscResponse(response);
    }

    public void sendLogin(String playerName) {
        networkClient.requestLogin(playerName);
    }

    public void sendLogout() {
        networkClient.sendLogout();
    }

    public void requestGameStart() {
        networkClient.requestNewGame();
    }

    public void displayWaitingScreen() {
        view.displayWaitingScreen();
    }

    public void displayLoginScreen() {
        view.displayLoginScreen();
        while (model.getLocalPlayer() == null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    public void displayResult(String result) {
        view.displayGameResultDialog(result);
    }

    public void notifyDisconnection() {
        view.displayOpponentLeft();
    }

    public void notifyConnectionIssue() {
        view.notifyConnectionError();
    }

    /**
     * Updates the board state with the latest move.
     *
     * @param player The current player making the move.
     */
    public void refreshGameBoard(int xCoord, int yCoord, Player player) {
        model.placeStoneAndUpdate(xCoord, yCoord, player.getPlayerChar());
        view.updateBoard(model, isActivePlayer);
    }

    public void refreshGameView() {
        view.updateBoard(model, isActivePlayer);
    }

    public void refreshHeader() {
        view.refreshHeaderInfo();
    }

    public void displayError(String errorMessage) {
        view.displayErrorDialog(errorMessage);
    }

    @Override
    public void run() {
        displayLoginScreen();
    }

    public void displayNotification(String message) {
        view.setStatusMessage(message);
    }
}
