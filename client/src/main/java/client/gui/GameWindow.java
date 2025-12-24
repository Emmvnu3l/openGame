package client.gui;

import client.networking.GameClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GameWindow extends Application {
    private GameClient client;
    private Pane root;
    // Mapa para guardar todos los jugadores: Nombre -> Cuadrado
    private Map<String, Rectangle> players = new HashMap<>();
    private Text positionText;
    private final String MY_NAME = "Jugador_" + new Random().nextInt(1000);

    @Override
    public void start(Stage stage) {
        // Inicializar red
        client = new GameClient();
        
        // Configurar GUI
        root = new Pane();
        root.setPrefSize(800, 600);
        
        // Texto de info
        positionText = new Text(10, 20, "Conectando...");
        root.getChildren().add(positionText);
        
        Scene scene = new Scene(root);
        
        // Manejo de Teclado
        scene.setOnKeyPressed(e -> {
            int direction = -1;
            if (e.getCode() == KeyCode.W || e.getCode() == KeyCode.UP) direction = 0;
            if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT) direction = 1;
            if (e.getCode() == KeyCode.S || e.getCode() == KeyCode.DOWN) direction = 2;
            if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT) direction = 3;
            
            if (direction != -1) {
                client.sendMove(direction);
            }
        });
        
        // Conectar eventos de red a la GUI
        client.setOnPositionUpdate(packet -> {
            Platform.runLater(() -> {
                // Verificar si el jugador ya existe en pantalla
                Rectangle rect = players.get(packet.username);
                
                if (rect == null) {
                    // ¡Es NUEVO! Lo creamos
                    boolean isMe = packet.username.equals(MY_NAME);
                    Color color = isMe ? Color.BLUE : Color.RED;
                    
                    rect = new Rectangle(40, 40, color);
                    rect.setX(packet.x);
                    rect.setY(packet.y);
                    
                    players.put(packet.username, rect); // Guardar en el mapa
                    root.getChildren().add(rect); // Añadir a la ventana
                    System.out.println("Nuevo jugador renderizado: " + packet.username);
                } else {
                    // Ya existe, solo actualizamos posición
                    rect.setX(packet.x);
                    rect.setY(packet.y);
                }
                
                // Actualizar texto si soy yo
                if (packet.username.equals(MY_NAME)) {
                    positionText.setText("Yo: " + packet.username + " | Pos: " + packet.x + "," + packet.y);
                }
            });
        });

        // Iniciar conexión en background
        new Thread(() -> client.connect(MY_NAME)).start();

        stage.setTitle("OpenGame MMORPG - JavaFX Client");
        stage.setScene(scene);
        stage.show();
    }
}
