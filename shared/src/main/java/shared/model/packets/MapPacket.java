package shared.model.packets;

import shared.model.map.Tile;

public class MapPacket {
    public String type = "map_data";
    public int width;
    public int height;
    public int[][] tileIds; // Enviamos solo IDs para ser eficientes

    public MapPacket() {} // Constructor vacío para Jackson

    public MapPacket(int width, int height, int[][] tileIds) {
        this.width = width;
        this.height = height;
        this.tileIds = tileIds;
    }
}
