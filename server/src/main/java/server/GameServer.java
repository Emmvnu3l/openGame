package server;

import shared.NetworkConstants;
import server.world.GameMap;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GameServer {
    private ServerSocket serverSocket;
    private boolean isRunning;
    
    // Lista de clientes conectados (Thread-Safe)
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static ObjectMapper mapper = new ObjectMapper();
    
    // Mapa del Juego
    public static GameMap gameMap;

    public void start() {
        // Inicializar Mapa (ej. 20x20 tiles)
        gameMap = new GameMap(20, 20); 
        
        try {
            serverSocket = new ServerSocket(NetworkConstants.PORT);
            isRunning = true;
            System.out.println("[SERVER] Servidor iniciado en puerto " + NetworkConstants.PORT);

            while (isRunning) {
                System.out.println("[SERVER] Esperando conexiones...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] ¡Nuevo cliente conectado! IP: " + clientSocket.getInetAddress());

                // Iniciar un hilo separado para este cliente
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler); // Agregar a la lista
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error: " + e.getMessage());
        }
    }
    
    public static void broadcast(Object packet) {
        broadcast(packet, null);
    }

    public static void broadcast(Object packet, ClientHandler excludeClient) {
        try {
            String json = mapper.writeValueAsString(packet);
            for (ClientHandler client : clients) {
                if (client != excludeClient) {
                    client.sendMessage(json);
                }
            }
        } catch (Exception e) {
            System.err.println("[SERVER] Error broadcasting: " + e.getMessage());
        }
    }
    
    // Método para obtener la lista de clientes (copia segura)
    public static List<ClientHandler> getClients() {
        return clients;
    }
    
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("[SERVER] Cliente removido.");
    }
}