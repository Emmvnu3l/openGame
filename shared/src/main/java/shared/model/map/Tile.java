package shared.model.map;

public class Tile {
    private int id;
    private boolean isWalkable;
    private String name;

    public Tile(int id, String name, boolean isWalkable) {
        this.id = id;
        this.name = name;
        this.isWalkable = isWalkable;
    }

    public boolean isWalkable() { return isWalkable; }
    public int getId() { return id; }
    public String getName() { return name; }
}
