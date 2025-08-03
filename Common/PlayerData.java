package Common;



import java.io.Serializable;

public class PlayerData implements Serializable {
    private String name;
    private long id;
    private int points;
    private boolean eliminated;
    private boolean hasPicked;

    public PlayerData(String name, long id, int points, boolean eliminated, boolean hasPicked) {
        this.name = name;
        this.id = id;
        this.points = points;
        this.eliminated = eliminated;
        this.hasPicked = hasPicked;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public int getPoints() {
        return points;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public boolean hasPicked() {
        return hasPicked;
    }
}
