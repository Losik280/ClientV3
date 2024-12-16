public class Player {
    private String name;
    char playerChar;

    public Player (String name) {
        this.name = name;
    }

    public Player (String name, char playerChar) {
        this.name = name;
        this.playerChar = playerChar;
    }

    public String getName() {
        return this.name;
    }

    public char getPlayerChar() {
        return this.playerChar;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPlayerChar(char playerChar) {
        this.playerChar = playerChar;
    }

}
