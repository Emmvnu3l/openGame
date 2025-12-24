package shared.model.packets;

// Esta clase será el padre de todos los mensajes
// Usamos una propiedad "type" para saber qué mensaje es (login, movimiento, etc.)
public class Packet {
    public String type;

    public Packet() {
    }

    public Packet(String type) {
        this.type = type;
    }
}