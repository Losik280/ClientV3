/**
 * This class represents the model of the Reversi game.
 */
public class GameModel {
    /**
     * The game board.
     */
    private final char[][] board;

    /**
     * The player on this PC.
     */
    private Player myPlayer;

    /**
     * The opponent player.
     */
    private Player opponentPlayer;

    /**
     * Status of the game.
     */
    private boolean gameOver;

    /**
     * Constructor of the ReversiModel class.
     */
    public GameModel(boolean myTurn) {
        this.board = new char[Constants.GAME_BOARD_SIZE][Constants.GAME_BOARD_SIZE];
        //resetBoard(myTurn);
    }

    /**
     * Resets the game board.
     */
    public void resetBoard(boolean myTurn) {
        char playerChar = this.myPlayer.getPlayerChar();
        char opponentChar = this.opponentPlayer.getPlayerChar();

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = ' ';
            }
        }
        // set up default values in the center of the board
        if (!myTurn){
            //switch player and opponent char
            char temp = playerChar;
            playerChar = opponentChar;
            opponentChar = temp;
        }

        int mid = board.length / 2;
        board[mid - 1][mid - 1] = playerChar;
        board[mid - 1][mid] = opponentChar;
        board[mid][mid - 1] = opponentChar;
        board[mid][mid] = playerChar;

        gameOver = false;
    }

    /**
     * Updates the game board with the response from the server.
     * @param response The response from the server. (e.g. "XOXOXOXO O X")
     */
    public void updateBoard(String response) {
        for (int i = 0; i < Constants.GAME_BOARD_SIZE; i++) {
            for (int j = 0; j < Constants.GAME_BOARD_SIZE; j++) {
                board[i][j] = response.charAt(i * Constants.GAME_BOARD_SIZE + j);
            }
        }


    }


    public void updateBoard(int toX, int toY, char playerChar) {
        // Directions: {dx, dy} pairs for horizontal, vertical, and diagonal moves
        int[][] directions = {
                {0, 1}, {1, 0}, {0, -1}, {-1, 0},  // Horizontal and vertical
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1} // Diagonal directions
        };

        // Identify opponent's character
        char opponentChar = (playerChar == 'R') ? 'B' : 'R';

        // Place the player's stone on the board
        board[toY][toX] = playerChar;

        // Iterate over all directions
        for (int[] direction : directions) {
            int dx = direction[0];
            int dy = direction[1];
            int x = toX + dx;
            int y = toY + dy;
            boolean validDirection = false;

            // Check if this direction contains opponent's stones followed by the player's stone
            while (isInBounds(x, y) && board[y][x] == opponentChar) {
                x += dx;
                y += dy;
            }

            // Validate the direction: Must end with player's stone after opponent's stones
            if (isInBounds(x, y) && board[y][x] == playerChar) {
                validDirection = true;
            }

            // If direction is valid, flip opponent's stones
            if (validDirection) {
                x = toX + dx;
                y = toY + dy;

                while (isInBounds(x, y) && board[y][x] == opponentChar) {
                    board[y][x] = playerChar;
                    x += dx;
                    y += dy;
                }
            }
        }
    }

    /**
     * Helper method to check if a position is within the bounds of the board.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if the position is within bounds, false otherwise.
     */
    private boolean isInBounds(int x, int y) {
        return x >= 0 && x < board[0].length && y >= 0 && y < board.length;
    }



    /**
     * Sets the opponent player.
     * @param opponentName The opponent player.
     */
    public void setOpponentPlayer(String opponentName, char playerChar) {
        this.opponentPlayer = new Player(opponentName, playerChar);
    }

    /**
     * Sets the player on this PC.
     * @param player The player on this PC.
     */
    public void setMyPlayer(Player player) {
        this.myPlayer = player;
    }

    /**
     * Returns the player on this PC.
     * @return The player on this PC.
     */
    public Player getMyPlayer() {
        return myPlayer;
    }

    /**
     * Returns the opponent player.
     * @return The opponent player.
     */
    public Player getOpponentPlayer() {
        return opponentPlayer;
    }

    /**
     * Returns the game board.
     * @return The game board.
     */
    public char[][] getBoard() {
        return board;
    }
}