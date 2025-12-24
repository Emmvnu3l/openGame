package shared.model.packets;

public class LoginPacket extends Packet {
    public String username;

    public LoginPacket() {
        super("LOGIN"); // Identificador del tipo de mensaje
    }

    public LoginPacket(String username) {
        super("LOGIN");
        this.username = username;
    }
}