package server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.model.packets.LoginPacket;
import shared.model.packets.MovePacket;
import shared.model.packets.PositionPacket;

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
                            
                            // 2. Enviar a MÍ la lista de TODOS los que ya están conectados
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
        switch (direction) {
            case 0: y -= speed; break; // UP
            case 1: x += speed; break; // RIGHT
            case 2: y += speed; break; // DOWN
            case 3: x -= speed; break; // LEFT
        }
        System.out.println("JUGADOR " + username + " MOVIDO A (" + x + "," + y + ")");
        
        // Avisar a TODOS que me moví
        PositionPacket pos = new PositionPacket(username, x, y);
        GameServer.broadcast(pos);
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