package shared.model.packets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatPacket extends Packet {
    public String username;
    public String message;
    public String target; 
    public String chatType; 

    public ChatPacket() {
        super("CHAT");
    }

    public ChatPacket(String username, String message) {
        super("CHAT");
        this.username = username;
        this.message = message;
        this.chatType = "GLOBAL";
    }

    public ChatPacket(String username, String message, String target, String chatType) {
        super("CHAT");
        this.username = username;
        this.message = message;
        this.target = target;
        this.chatType = chatType;
    }
    
    // Getters y Setters para asegurar compatibilidad con Jackson
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    
    public String getChatType() { return chatType; }
    public void setChatType(String chatType) { this.chatType = chatType; }
}
