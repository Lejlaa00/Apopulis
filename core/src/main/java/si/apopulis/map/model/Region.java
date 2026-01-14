package si.apopulis.map.model;

public class Region {
    public String id;
    public float[] vertices;
    public float minX, maxX, minY, maxY;
    public Region(String id, float[] vertices) {
        this.id = id;
        this.vertices = vertices;

        minX = maxX = vertices[0];
        minY = maxY = vertices[1];

        for (int i = 2; i < vertices.length; i += 2) {
            minX = Math.min(minX, vertices[i]);
            maxX = Math.max(maxX, vertices[i]);
            minY = Math.min(minY, vertices[i + 1]);
            maxY = Math.max(maxY, vertices[i + 1]);
        }
    }
}

