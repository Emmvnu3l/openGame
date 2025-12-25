package server.world;

import shared.model.map.Tile;

public class GameMap {
    private int width;
    private int height;
    private Tile[][] tiles;
    private int tileSize = 50; // Tamaño de cada tile en pixeles

    // IDs básicos
    public static final int TILE_GRASS = 0;
    public static final int TILE_WATER = 1;
    public static final int TILE_WALL = 2;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        generateTestMap();
    }

    private void generateTestMap() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Por defecto pasto
                tiles[x][y] = new Tile(TILE_GRASS, "Grass", true);

                // Bordes de agua (creando una isla)
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    tiles[x][y] = new Tile(TILE_WATER, "Water", false);
                }

                // Un muro de prueba en el medio
                if (x == 10 && y > 5 && y < 15) {
                    tiles[x][y] = new Tile(TILE_WALL, "Wall", false);
                }
            }
        }
    }

    public boolean isWalkable(int pixelX, int pixelY) {
        // Convertir coordenadas de pixeles a tiles
        int tileX = pixelX / tileSize;
        int tileY = pixelY / tileSize;

        if (tileX < 0 || tileX >= width || tileY < 0 || tileY >= height) {
            return false; // Fuera del mapa
        }
        return tiles[tileX][tileY].isWalkable();
    }
    
    public int getTileSize() { return tileSize; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public int[][] getTileIds() {
        int[][] ids = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ids[x][y] = tiles[x][y].getId();
            }
        }
        return ids;
    }
}
