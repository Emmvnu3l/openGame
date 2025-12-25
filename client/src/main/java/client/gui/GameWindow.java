package client.gui;

import client.networking.GameClient;
import shared.model.packets.MapPacket;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GameWindow extends Application {
    private GameClient client;
    private Pane gameWorld; // Viewport (lo que se ve en pantalla)
    private Group worldContent; // Contenido del mundo (Mapa + Jugadores)
    private Group mapLayer;     // Capa 1: Mapa (Fondo)
    private Group entityLayer;  // Capa 2: Entidades (Jugadores)
    private VBox uiRoot;    // Raíz de la UI (layout vertical)
    
    // Mapa para guardar todos los jugadores
    private Map<String, PlayerSprite> players = new HashMap<>();
    private Text positionText;
    
    // --- CHAT TIBIA STYLE ---
    private TabPane chatTabs;
    private ListView<Text> localChatList;
    private ListView<Text> serverLogList;
    private TextField chatInput;
    
    // Estado
    private final String MY_NAME = "Jugador_" + new Random().nextInt(1000);
    private String privateChatTarget = null; 

    // Clase interna para visualización del jugador
    private class PlayerSprite extends Group {
        private Rectangle body;
        private Text nameTag;
        private Text chatBubble;
        private FadeTransition chatFade;
        private String name;

        public PlayerSprite(String name, Color color) {
            this.name = name;
            
            // Cuerpo
            body = new Rectangle(40, 40, color);
            
            // Nombre
            nameTag = new Text(name);
            nameTag.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            nameTag.setFill(Color.BLACK);
            double nameWidth = nameTag.getLayoutBounds().getWidth();
            nameTag.setX((40 - nameWidth) / 2);
            nameTag.setY(-5); 

            // Burbuja Chat
            chatBubble = new Text();
            chatBubble.setFont(Font.font("Arial", 14));
            chatBubble.setFill(Color.DARKBLUE);
            chatBubble.setStroke(Color.WHITE);
            chatBubble.setStrokeWidth(0.5);
            chatBubble.setVisible(false);

            this.getChildren().addAll(body, nameTag, chatBubble);
            
            // Menu Contextual
            if (!name.equals(MY_NAME)) {
                this.setOnContextMenuRequested(e -> {
                    ContextMenu menu = new ContextMenu();
                    MenuItem itemPrivate = new MenuItem("Mensaje Privado a " + name);
                    itemPrivate.setOnAction(ev -> {
                        startPrivateChat(name);
                    });
                    menu.getItems().add(itemPrivate);
                    menu.show(this, e.getScreenX(), e.getScreenY());
                });
            }
        }

        public void showChat(String msg, Color color) {
            chatBubble.setText(msg);
            chatBubble.setFill(color);
            chatBubble.setVisible(true);
            
            double chatWidth = chatBubble.getLayoutBounds().getWidth();
            chatBubble.setX((40 - chatWidth) / 2);
            chatBubble.setY(-25);

            if (chatFade != null) chatFade.stop();
            chatBubble.setOpacity(1.0);
            chatFade = new FadeTransition(Duration.seconds(4), chatBubble);
            chatFade.setFromValue(1.0);
            chatFade.setToValue(0.0);
            chatFade.setDelay(Duration.seconds(3));
            chatFade.setOnFinished(e -> chatBubble.setVisible(false));
            chatFade.play();
        }
    }

    private void startPrivateChat(String target) {
        privateChatTarget = target;
        chatInput.setPromptText("Privado a " + target + ": (ESC para cancelar)");
        chatInput.setStyle("-fx-background-color: #ffcccc;"); // Color rojizo para indicar privado
        chatInput.requestFocus();
    }

    private void endPrivateChat() {
        privateChatTarget = null;
        chatInput.setPromptText("Escribe un mensaje...");
        chatInput.setStyle(""); // Restaurar estilo
    }

    // Método para renderizar el mapa recibido del servidor
    private void renderMap(MapPacket mapData) {
        // Limpiar solo la capa del mapa
        mapLayer.getChildren().clear();

        int tileSize = 50;
        
        for (int x = 0; x < mapData.width; x++) {
            for (int y = 0; y < mapData.height; y++) {
                int tileId = mapData.tileIds[x][y];
                
                Rectangle tile = new Rectangle(tileSize, tileSize);
                tile.setX(x * tileSize);
                tile.setY(y * tileSize);
                tile.setStroke(Color.BLACK);
                tile.setStrokeWidth(0.5);
                
                switch (tileId) {
                    case 0: tile.setFill(Color.LIGHTGREEN); break; // Pasto
                    case 1: tile.setFill(Color.BLUE); break;       // Agua
                    case 2: tile.setFill(Color.GRAY); break;       // Muro
                    default: tile.setFill(Color.BLACK); break;
                }
                
                mapLayer.getChildren().add(tile);
            }
        }
    }

    private void updateCamera(double playerX, double playerY) {
        double viewportW = gameWorld.getPrefWidth();
        double viewportH = gameWorld.getPrefHeight();
        
        // Centrar: ViewportCenter - PlayerPos - PlayerHalfSize
        // PlayerHalfSize es aprox 20 (40/2)
        double newX = (viewportW / 2) - playerX - 20; 
        double newY = (viewportH / 2) - playerY - 20;
        
        worldContent.setTranslateX(newX);
        worldContent.setTranslateY(newY);
    }

    @Override
    public void start(Stage stage) {
        // 1. Inicializar Red
        client = new GameClient();
        
        // 2. Configurar Layout Principal (BorderPane o VBox)
        // Usaremos un VBox: Arriba el Juego (Pane), Abajo el Chat (Tibia Style)
        uiRoot = new VBox();
        uiRoot.setPrefSize(800, 600);
        uiRoot.setStyle("-fx-background-color: black;");

        // --- MUNDO DEL JUEGO ---
        gameWorld = new Pane();
        gameWorld.setPrefSize(800, 450); // 3/4 de la pantalla
        gameWorld.setStyle("-fx-background-color: #222; -fx-border-color: #555; -fx-border-width: 2;");

        // Crear contenedor para el mundo
        worldContent = new Group();
        
        // Inicializar capas
        mapLayer = new Group();
        entityLayer = new Group();
        
        // Añadir capas en orden: Primero Mapa (fondo), luego Entidades (arriba)
        worldContent.getChildren().addAll(mapLayer, entityLayer);
        
        gameWorld.getChildren().add(worldContent);
        
        // Clip para que el mapa no se salga del viewport
        Rectangle clip = new Rectangle(800, 450);
        gameWorld.setClip(clip);

        // Renderizado del Mapa: Esperar paquete del servidor
        // drawTestMap(worldContent); // Eliminado
        
        positionText = new Text(10, 20, "Conectando...");
        gameWorld.getChildren().add(positionText); // HUD fijo (no se mueve con la cámara)

        // --- UI DE CHAT (TIBIA STYLE) ---
        VBox chatContainer = new VBox();
        chatContainer.setPrefSize(800, 150);
        chatContainer.setStyle("-fx-background-color: #333; -fx-padding: 5;");
        VBox.setVgrow(chatContainer, Priority.ALWAYS);

        // Tabs
        chatTabs = new TabPane();
        chatTabs.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
        
        // Tab Local Chat
        Tab tabLocal = new Tab("Local Chat");
        tabLocal.setClosable(false);
        localChatList = new ListView<>();
        localChatList.setStyle("-fx-background-color: #222; -fx-control-inner-background: #222;");
        tabLocal.setContent(localChatList);
        
        // Tab Server Log
        Tab tabServer = new Tab("Server Log");
        tabServer.setClosable(false);
        serverLogList = new ListView<>();
        serverLogList.setStyle("-fx-background-color: #222; -fx-control-inner-background: #222;");
        tabServer.setContent(serverLogList);
        
        chatTabs.getTabs().addAll(tabLocal, tabServer);
        VBox.setVgrow(chatTabs, Priority.ALWAYS);

        // Input
        chatInput = new TextField();
        chatInput.setPromptText("Escribe un mensaje...");
        chatInput.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        chatInput.setFont(Font.font("Arial", 12));

        chatContainer.getChildren().addAll(chatTabs, chatInput);
        
        // Añadir todo al root
        uiRoot.getChildren().addAll(gameWorld, chatContainer);

        // --- LOGICA DE EVENTOS ---
        
        // Enviar Mensaje
        chatInput.setOnAction(e -> {
            String msg = chatInput.getText();
            if (!msg.isEmpty()) {
                if (privateChatTarget != null) {
                    client.sendPrivateChat(privateChatTarget, msg);
                    // Feedback visual local inmediato
                    addMessageToChat("Tú a " + privateChatTarget + ": " + msg, Color.MAGENTA);
                } else {
                    client.sendChat(msg);
                }
                chatInput.clear();
            }
        });

        // Click en juego devuelve foco
        gameWorld.setOnMouseClicked(e -> gameWorld.requestFocus());

        Scene scene = new Scene(uiRoot);
        
        // Teclado Global
        scene.setOnKeyPressed(e -> {
            // Cancelar privado
            if (e.getCode() == KeyCode.ESCAPE) {
                if (privateChatTarget != null) {
                    endPrivateChat();
                }
                gameWorld.requestFocus();
                return;
            }

            // Si estamos en el input, no mover
            if (chatInput.isFocused()) {
                return; 
            }

            // Chat Rápido con ENTER
            if (e.getCode() == KeyCode.ENTER) {
                chatInput.requestFocus();
                return;
            }

            // Movimiento
            int direction = -1;
            if (e.getCode() == KeyCode.W || e.getCode() == KeyCode.UP) direction = 0;
            if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT) direction = 1;
            if (e.getCode() == KeyCode.S || e.getCode() == KeyCode.DOWN) direction = 2;
            if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT) direction = 3;
            
            if (direction != -1) {
                client.sendMove(direction);
            }
        });

        // --- CALLBACKS DE RED ---
        
        client.setOnMapReceived(packet -> {
            Platform.runLater(() -> renderMap(packet));
        });
        
        client.setOnPositionUpdate(packet -> {
            Platform.runLater(() -> {
                PlayerSprite sprite = players.get(packet.username);
                
                if (sprite == null) {
                    boolean isMe = packet.username.equals(MY_NAME);
                    Color color = isMe ? Color.BLUE : Color.RED;
                    
                    sprite = new PlayerSprite(packet.username, color);
                    sprite.setLayoutX(packet.x);
                    sprite.setLayoutY(packet.y);
                    
                    players.put(packet.username, sprite); 
                    entityLayer.getChildren().add(sprite); // Añadir a la capa de entidades
                } else {
                    sprite.setLayoutX(packet.x);
                    sprite.setLayoutY(packet.y);
                }
                
                if (packet.username.equals(MY_NAME)) {
                    positionText.setText("Yo: " + packet.username + " | Pos: " + packet.x + "," + packet.y);
                    // ACTUALIZAR CÁMARA
                    updateCamera(packet.x, packet.y);
                }
            });
        });

        client.setOnChatReceived(packet -> {
            Platform.runLater(() -> {
                System.out.println("CLIENT DEBUG: Msg recibido de " + packet.getUsername());
                
                boolean isPrivate = "PRIVATE".equals(packet.getChatType());
                String sender = packet.getUsername();
                String msg = packet.getMessage();
                
                if (isPrivate) {
                    // Mensaje Privado (Magenta)
                    addMessageToChat("(Privado) " + sender + ": " + msg, Color.MAGENTA);
                } else {
                    // Chat Global (Blanco/Amarillo estilo Tibia)
                    addMessageToChat(sender + ": " + msg, Color.LIGHTYELLOW);
                }

                // Burbuja visual
                PlayerSprite sprite = players.get(sender);
                if (sprite != null) {
                    sprite.showChat(msg, isPrivate ? Color.MAGENTA : Color.DARKBLUE);
                }
            });
        });

        // Hilo de conexión
        new Thread(() -> {
            client.connect(MY_NAME);
            Platform.runLater(() -> addLog("Conectado al servidor como " + MY_NAME));
        }).start();

        stage.setTitle("OpenGame - Tibia Style Chat");
        stage.setScene(scene);
        stage.show();
        
        gameWorld.requestFocus();
    }
    
    // Helpers para UI
    private void addMessageToChat(String text, Color color) {
        Text t = new Text(text);
        t.setFill(color);
        t.setFont(Font.font("Verdana", 12));
        localChatList.getItems().add(t);
        localChatList.scrollTo(localChatList.getItems().size() - 1);
    }
    
    private void addLog(String text) {
        Text t = new Text(text);
        t.setFill(Color.LIGHTGRAY);
        t.setFont(Font.font("Verdana", 10));
        serverLogList.getItems().add(t);
        serverLogList.scrollTo(serverLogList.getItems().size() - 1);
    }
}
