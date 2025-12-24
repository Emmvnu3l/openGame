# OpenTibia Clone (Java MMORPG)

Proyecto de desarrollo de un MMORPG 2D estilo Tibia desde cero, utilizando Java puro y JavaFX, enfocado en una arquitectura Cliente-Servidor robusta y escalable.

## 🚀 Estado Actual: Networking Básico (Fase 1)
Hemos establecido con éxito la comunicación fundamental entre el cliente y el servidor.

### ✅ Logros Completados
- [x] **Estructura Multi-módulo:** Configuración de Gradle con módulos `server`, `client` y `shared`.
- [x] **Módulo Shared:** Definición de constantes de red (Puerto/Host) para evitar duplicidad.
- [x] **Servidor TCP (`GameServer`):**
    - Escucha conexiones en el puerto 5555.
    - Acepta múltiples clientes simultáneamente.
    - Implementación de **Threads** (`ClientHandler`) para manejar a cada jugador de forma independiente.
- [x] **Cliente TCP (`GameClient`):**
    - Se conecta exitosamente al servidor.
    - Recibe y muestra mensajes de bienvenida.
    - Mantiene la sesión "viva" (loop de espera).

---

## 🛠 Arquitectura Técnica

### 1. Server (`/server`)
El cerebro del juego. Es autoritativo y maneja toda la lógica.
- **GameServer:** Punto de entrada que acepta sockets.
- **ClientHandler:** Hilo dedicado por cada jugador conectado. Gestiona la entrada/salida de datos de ese usuario específico.

### 2. Client (`/client`)
La interfaz visual (futuro JavaFX). Actualmente es una consola que actúa como terminal tonta.
- **GameClient:** Gestiona la conexión TCP y escucha eventos del servidor.

### 3. Shared (`/shared`)
Código compartido para asegurar consistencia.
- **NetworkConstants:** Configuración de red (IP, Puerto).
- *(Próximamente: Paquetes de protocolo, Enums, Modelos de datos)*.

---

## 📋 Próximos Pasos (Hoja de Ruta)

### Fase 2: Protocolo de Comunicación (Inmediato)
- [ ] Definir un protocolo de mensajes (ej: JSON o Bytes).
- [ ] Crear clases de Paquetes en `shared` (LoginPacket, MovePacket, ChatPacket).
- [ ] Serializar y Deserializar objetos para enviarlos por la red.

### Fase 3: Game Loop & Estado
- [ ] Implementar el "Tick Loop" en el servidor (60 actualizaciones por segundo).
- [ ] Crear un mapa básico en memoria.
- [ ] Sincronizar la posición de los jugadores.

### Fase 4: Cliente Gráfico (JavaFX)
- [ ] Reemplazar la consola por una ventana gráfica.
- [ ] Renderizar un personaje (cuadrado o sprite).
- [ ] Mover el personaje con el teclado y enviar los comandos al servidor.

---

## 💡 Cómo Ejecutar el Proyecto

1. **Iniciar el Servidor:** Ejecutar `ServerMain`.
2. **Iniciar Clientes:** Ejecutar `ClientMain` (puedes abrir múltiples instancias para simular varios jugadores).