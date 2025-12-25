package server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.model.packets.ChatPacket;
import shared.model.packets.LoginPacket;
import shared.model.packets.MapPacket;
import shared.model.packets.MovePacket;
import shared.model.packets.PositionPacket;
import server.world.GameMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private ObjectMapper mapper = new ObjectMapper(); // Jackson
    
    // Estado del Jugador
    public String username;
    public int x = 100; // Posición inicial
    public int y = 100;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }
    
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void run() {
        try {
            // Configurar canales de comunicación
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println("[HANDLER] Cliente conectado. Esperando paquetes...");

            // Loop de escucha
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // System.out.println("[RAW RECIBIDO] " + inputLine);

                // Procesar JSON
                try {
                    JsonNode node = mapper.readTree(inputLine);
                    if (node.has("type")) {
                        String type = node.get("type").asText();

                        if ("LOGIN".equals(type)) {
                            LoginPacket login = mapper.treeToValue(node, LoginPacket.class);
                            this.username = login.username;
                            System.out.println(">>> ¡JUGADOR LOGUEADO!: " + login.username);
                            
                            // 1. Enviar MI posición a TODOS (Broadcast)
                            GameServer.broadcast(new PositionPacket(username, x, y));
                            
                            // 2. Enviar a MÍ el MAPA
                            MapPacket mapPacket = new MapPacket(
                                GameServer.gameMap.getWidth(),
                                GameServer.gameMap.getHeight(),
                                GameServer.gameMap.getTileIds()
                            );
                            this.sendMessage(mapper.writeValueAsString(mapPacket));
                            
                            // 3. Enviar a MÍ la lista de TODOS los que ya están conectados
                            for (ClientHandler other : GameServer.getClients()) {
                                if (other != this && other.username != null) {
                                    PositionPacket existingPlayerPos = new PositionPacket(other.username, other.x, other.y);
                                    String json = mapper.writeValueAsString(existingPlayerPos);
                                    this.sendMessage(json);
                                }
                            }
                            
                        } else if ("MOVE".equals(type)) {
                            MovePacket move = mapper.treeToValue(node, MovePacket.class);
                            processMovement(move.direction);
                        } else if ("CHAT".equals(type)) {
                            ChatPacket chat = mapper.treeToValue(node, ChatPacket.class);
                            chat.username = this.username; 
                            
                            System.out.println("DEBUG CHAT: Tipo=" + chat.chatType + " Target=" + chat.target + " Msg=" + chat.message);

                            if ("PRIVATE".equals(chat.chatType) && chat.target != null) {
                                // --- CHAT PRIVADO ---
                                System.out.println("[CHAT PRIVADO] " + chat.username + " -> " + chat.target + ": " + chat.message);
                                
                                boolean found = false;
                                for (ClientHandler other : GameServer.getClients()) {
                                    // Normalizar comparación de strings (trim)
                                    if (other.username != null && other.username.trim().equals(chat.target.trim())) {
                                        other.sendMessage(mapper.writeValueAsString(chat));
                                        found = true;
                                        break;
                                    }
                                }
                                
                                if (!found) {
                                    System.out.println("ADVERTENCIA: Destinatario " + chat.target + " no encontrado.");
                                }
                                
                                // 2. Enviar copia al remitente
                                this.sendMessage(mapper.writeValueAsString(chat));
                                
                            } else {
                                // --- CHAT GLOBAL (PROXIMIDAD) ---
                                System.out.println("[CHAT GLOBAL] " + chat.username + ": " + chat.message);
                                int range = 500; // Rango de visión del chat
                                
                                for (ClientHandler other : GameServer.getClients()) {
                                    if (other.username == null) continue;
                                    
                                    // Calcular distancia
                                    double dist = Math.hypot(other.x - this.x, other.y - this.y);
                                    
                                    // Enviar solo si está cerca
                                    if (dist <= range) {
                                        String json = mapper.writeValueAsString(chat);
                                        other.sendMessage(json);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando paquete: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("[HANDLER] Error en la conexión con el cliente: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private void processMovement(int direction) {
        int speed = 10;
        int newX = x;
        int newY = y;
        
        switch (direction) {
            case 0: newY -= speed; break; // UP
            case 1: newX += speed; break; // RIGHT
            case 2: newY += speed; break; // DOWN
            case 3: newX -= speed; break; // LEFT
        }
        
        // Validar colisión con el mapa
        if (GameServer.gameMap.isWalkable(newX, newY)) {
            x = newX;
            y = newY;
            // System.out.println("JUGADOR " + username + " MOVIDO A (" + x + "," + y + ")");
            
            // Avisar a TODOS que me moví
            PositionPacket pos = new PositionPacket(username, x, y);
            GameServer.broadcast(pos);
        } else {
            // Opcional: Avisar al cliente que chocó (o simplemente no moverlo)
            // System.out.println("JUGADOR " + username + " CHOCÓ CON MURO EN (" + newX + "," + newY + ")");
        }
    }

    private void closeConnection() {
        GameServer.removeClient(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("[HANDLER] Conexión cerrada.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}