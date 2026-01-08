package si.apopulis.map;

import com.badlogic.gdx.graphics.Color;

public class Marker {
    public final float lat;
    public final float lon;
    public final Color color;
    public final String type;
    public final String name;
    public final String level;

    public Marker(
        float lat,
        float lon,
        Color color,
        String type,
        String name,
        String level
    ) {
        this.lat = lat;
        this.lon = lon;
        this.color = color;
        this.type = type;
        this.name = name;
        this.level = level;
    }
}
