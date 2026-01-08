package si.apopulis.map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

public class GeoJsonMarkerLoader {

    public static List<Marker> loadMarkers(String geoJson) {
        List<Marker> markers = new ArrayList<>();

        JsonValue root = new JsonReader().parse(geoJson);
        JsonValue features = root.get("features");
        if (features == null) return markers;

        for (JsonValue feature = features.child; feature != null; feature = feature.next) {
            JsonValue geometry = feature.get("geometry");
            JsonValue properties = feature.get("properties");
            if (geometry == null || properties == null) continue;

            String geomType = geometry.getString("type", "");
            if (!"Point".equals(geomType)) continue;

            JsonValue coords = geometry.get("coordinates");
            if (coords == null || coords.size < 2) continue;

            float lon = coords.getFloat(0);
            float lat = coords.getFloat(1);

            String type = properties.getString("type", "unknown");
            String name = properties.getString("name", null);
            String level = properties.getString("level", "news");


            String colorHex = properties.getString("marker-color", null);
            Color color = parseColor(colorHex);

            markers.add(new Marker(lat, lon, color, type, name, level));
        }

        return markers;
    }

    private static Color parseColor(String hex) {
        if (hex == null) return Color.WHITE;
        hex = hex.replace("#", "");
        return Color.valueOf(hex);
    }
}
