package shared.model.packets;

public class MovePacket extends Packet {
    public int direction; // 0: UP, 1: RIGHT, 2: DOWN, 3: LEFT (Por ejemplo)
    public String username;

    public MovePacket() {
        super("MOVE");
    }

    public MovePacket(String username, int direction) {
        super("MOVE");
        this.username = username;
        this.direction = direction;
    }
}
