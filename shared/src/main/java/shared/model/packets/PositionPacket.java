package shared.model.packets;

public class PositionPacket extends Packet {
    public String username;
    public int x;
    public int y;

    public PositionPacket() {
        super("POSITION");
    }

    public PositionPacket(String username, int x, int y) {
        super("POSITION");
        this.username = username;
        this.x = x;
        this.y = y;
    }
}
