package client.networking;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.NetworkConstants;
import shared.model.packets.ChatPacket;
import shared.model.packets.LoginPacket;
import shared.model.packets.MapPacket;
import shared.model.packets.MovePacket;
import shared.model.packets.PositionPacket;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class GameClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ObjectMapper mapper = new ObjectMapper(); // Jackson
    
    // Callback para actualizar la GUI
    private Consumer<PositionPacket> onPositionUpdate;
    private Consumer<ChatPacket> onChatReceived;
    private Consumer<MapPacket> onMapReceived;
    private String myUsername;

    public void setOnPositionUpdate(Consumer<PositionPacket> callback) {
        this.onPositionUpdate = callback;
    }

    public void setOnChatReceived(Consumer<ChatPacket> callback) {
        this.onChatReceived = callback;
    }

    public void setOnMapReceived(Consumer<MapPacket> callback) {
        this.onMapReceived = callback;
    }

    public void connect(String username) {
        this.myUsername = username;
        try {
            System.out.println("[CLIENT] Conectando a " + NetworkConstants.HOST + ":" + NetworkConstants.PORT);
            socket = new Socket(NetworkConstants.HOST, NetworkConstants.PORT);
            System.out.println("[CLIENT] ¡Conexión exitosa!");

            // Configurar salida
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 1. ENVIAR LOGIN (JSON)
            LoginPacket login = new LoginPacket(username);
            String json = mapper.writeValueAsString(login);
            out.println(json); // Enviar JSON como línea de texto
            System.out.println("[CLIENT] Enviado login: " + json);

            // Hilo para escuchar al servidor
            new Thread(this::listenLoop).start();

        } catch (IOException e) {
            System.err.println("[CLIENT] Error de conexión: " + e.getMessage());
        }
    }
    
    private void listenLoop() {
        try {
            String response;
            while ((response = in.readLine()) != null) {
                // System.out.println("[SERVER RAW]: " + response);
                try {
                     if (!response.startsWith("{")) continue; // Ignorar mensajes de texto plano por ahora
                     
                     JsonNode node = mapper.readTree(response);
                     if (node.has("type")) {
                         String type = node.get("type").asText();
                         
                         if ("POSITION".equals(type)) {
                            PositionPacket pos = mapper.treeToValue(node, PositionPacket.class);
                            if (onPositionUpdate != null) {
                                onPositionUpdate.accept(pos);
                            }
                        } else if ("map_data".equals(type)) {
                            MapPacket map = mapper.treeToValue(node, MapPacket.class);
                            if (onMapReceived != null) {
                                onMapReceived.accept(map);
                            }
                        } else if ("CHAT".equals(type)) {
                             ChatPacket chat = mapper.treeToValue(node, ChatPacket.class);
                             if (onChatReceived != null) {
                                 onChatReceived.accept(chat);
                             }
                         }
                     }
                } catch (Exception e) {
                    System.err.println("Error procesando server msg: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Desconectado.");
        }
    }
    
    public void sendMove(int direction) {
        if (out == null) return;
        try {
            MovePacket move = new MovePacket(myUsername, direction);
            String json = mapper.writeValueAsString(move);
            out.println(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendChat(String message) {
        if (out == null) return;
        try {
            ChatPacket chat = new ChatPacket(myUsername, message);
            String json = mapper.writeValueAsString(chat);
            out.println(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPrivateChat(String target, String message) {
        if (out == null) return;
        try {
            ChatPacket chat = new ChatPacket(myUsername, message, target, "PRIVATE");
            String json = mapper.writeValueAsString(chat);
            out.println(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
