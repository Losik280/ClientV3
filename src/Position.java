

/**
 * Instances of the Position class represent a container that carries the x and y position on the chessboard.
 */
public class Position {
    private int x;
    private int y;

    /**
     * Constructor for the container
     * @param x x position on the chessboard
     * @param y y position on the chessboard
     */
    public Position(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    /**
     * Setter for the x position
     * @param x x position on the chessboard
     */
    private void setX(int x) {
        //if (x < 0 || x > 7) throw new IllegalArgumentException("Invalid x coordinate");
        this.x = x;
    }

    /**
     * Setter for the y position
     * @param y y position on the chessboard
     */
    private void setY(int y) {
        //if (y < 0 || y > 7) throw new IllegalArgumentException("Invalid y coordinate");
        this.y = y;
    }

    /**
     * Getter for the x position on the chessboard
     * @return x position on the chessboard
     */
    public int getX() {
        return x;
    }

    /**
     * Getter for the y position on the chessboard
     * @return y position on the chessboard
     */
    public int getY() {
        return y;
    }
}