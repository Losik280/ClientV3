import java.util.Arrays;

/**
 * Model for managing the state of a Reversi game.
 */
public class GameModel {
    /**
     * The playing field.
     */
    private final char[][] gameBoard;

    /**
     * Player using this computer.
     */
    private Player localPlayer;

    /**
     * The remote player.
     */
    private Player remotePlayer;

    /**
     * Indicates if the game is finished.
     */
    private boolean isGameOver;

    /**
     * Initializes the game model with starting conditions.
     */
    public GameModel(boolean isFirstPlayer) {
        this.gameBoard = new char[GameView.GAME_BOARD_SIZE][GameView.GAME_BOARD_SIZE];
    }


    /**
     * Sets the remote player with the given name and token.
     *
     * @param name The name of the remote player.
     * @param token The character token representing the remote player.
     */
    public void setRemotePlayer(String name, char token) {
        this.remotePlayer = new Player(name, token);
    }

    /**
     * Sets the local player.
     *
     * @param player The local player object.
     */
    public void setLocalPlayer(Player player) {
        this.localPlayer = player;
    }

    /**
     * Gets the local player.
     *
     * @return The local player object.
     */
    public Player getLocalPlayer() {
        return localPlayer;
    }

    /**
     * Gets the remote player.
     *
     * @return The remote player object.
     */
    public Player getRemotePlayer() {
        return remotePlayer;
    }

    /**
     * Gets the game board.
     *
     * @return A 2D array representing the game board.
     */
    public char[][] getGameBoard() {
        return gameBoard;
    }

    /**
     * Sets the board to its initial state.
     */
    public void resetBoard(boolean isFirstPlayer) {
        char localChar = this.localPlayer.getPlayerChar();
        char remoteChar = this.remotePlayer.getPlayerChar();

        for (char[] row : gameBoard) {
            Arrays.fill(row, ' ');
        }

        int center = gameBoard.length / 2;
        gameBoard[center - 1][center - 1] = isFirstPlayer ? localChar : remoteChar;
        gameBoard[center - 1][center] = isFirstPlayer ? remoteChar : localChar;
        gameBoard[center][center - 1] = isFirstPlayer ? remoteChar : localChar;
        gameBoard[center][center] = isFirstPlayer ? localChar : remoteChar;

        isGameOver = false;
    }

    /**
     * Incorporates the server's response into the game state.
     * @param serverResponse The response from the server. (e.g. "XOXOXOXO O X")
     */
    public void updateBoard(String serverResponse) {
        for (int i = 0; i < GameView.GAME_BOARD_SIZE; i++) {
            for (int j = 0; j < GameView.GAME_BOARD_SIZE; j++) {
                gameBoard[i][j] = serverResponse.charAt(i * GameView.GAME_BOARD_SIZE + j);
            }
        }
    }

    /**
     * Updates the game board with a new move and flips the opponent's pieces as necessary.
     *
     * @param targetX The x-coordinate of the move.
     * @param targetY The y-coordinate of the move.
     * @param localChar The character representing the local player's pieces.
     */
    public void updateBoard(int targetX, int targetY, char localChar) {
        // Define possible movement directions
        int[][] moves = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        char opposingChar = (localChar == 'R') ? 'B' : 'R';
        gameBoard[targetY][targetX] = localChar;

        // Check and flip pieces in all possible directions
        for (int[] move : moves) {
            boolean canFlip = checkAndFlip(targetX, targetY, move[0], move[1], localChar, opposingChar);
        }
    }

    /**
     * Checks if pieces can be flipped in a given direction and flips them if possible.
     *
     * @param x The starting x-coordinate.
     * @param y The starting y-coordinate.
     * @param dx The x-direction to move.
     * @param dy The y-direction to move.
     * @param playerChar The character representing the player's pieces.
     * @param opponentChar The character representing the opponent's pieces.
     * @return true if pieces were flipped, false otherwise.
     */
    private boolean checkAndFlip(int x, int y, int dx, int dy, char playerChar, char opponentChar) {
        int newX = x + dx;
        int newY = y + dy;
        boolean canFlip = false;

        // Move in the specified direction and check for opponent's pieces
        while (isInBounds(newX, newY) && gameBoard[newY][newX] == opponentChar) {
            newX += dx;
            newY += dy;
        }

        // If a player's piece is found, flip the opponent's pieces
        if (isInBounds(newX, newY) && gameBoard[newY][newX] == playerChar) {
            canFlip = true;
            while (newX != x || newY != y) {
                newX -= dx;
                newY -= dy;
                gameBoard[newY][newX] = playerChar;
            }
        }

        return canFlip;
    }

    /**
     * Checks if the given coordinates are within the bounds of the game board.
     *
     * @param x The x-coordinate to check.
     * @param y The y-coordinate to check.
     * @return true if the coordinates are within bounds, false otherwise.
     */
    private boolean isInBounds(int x, int y) {
        return x >= 0 && x < gameBoard[0].length && y >= 0 && y < gameBoard.length;
    }

    }
