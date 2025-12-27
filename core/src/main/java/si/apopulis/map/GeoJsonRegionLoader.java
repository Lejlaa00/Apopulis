package si.apopulis.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

public class GeoJsonRegionLoader {

    public static List<Region> loadAllRegions() {

        List<Region> regions = new ArrayList<>();

        FileHandle file = Gdx.files.internal("sr_regions.geojson");
        JsonValue root = new JsonReader().parse(file);

        JsonValue features = root.get("features");

        // Global bounding box
        float minLon = Float.MAX_VALUE;
        float maxLon = Float.MIN_VALUE;
        float minLat = Float.MAX_VALUE;
        float maxLat = Float.MIN_VALUE;

        for (JsonValue feature : features) {
            JsonValue coords = feature.get("geometry").get("coordinates").get(0);
            for (JsonValue p : coords) {
                float lon = p.getFloat(0);
                float lat = p.getFloat(1);

                minLon = Math.min(minLon, lon);
                maxLon = Math.max(maxLon, lon);
                minLat = Math.min(minLat, lat);
                maxLat = Math.max(maxLat, lat);
            }
        }

        float width = maxLon - minLon;
        float height = maxLat - minLat;
        float scale = 700f / Math.max(width, height);

        float centerLon = (minLon + maxLon) / 2f;
        float centerLat = (minLat + maxLat) / 2f;

        // Regions
        for (JsonValue feature : features) {

            String id = feature.get("properties").getString("SR_ID");
            JsonValue coords = feature.get("geometry").get("coordinates").get(0);

            List<Float> verts = new ArrayList<>();

            for (JsonValue p : coords) {
                float lon = p.getFloat(0);
                float lat = p.getFloat(1);

                verts.add((lon - centerLon) * scale + 400);
                verts.add((lat - centerLat) * scale + 300);
            }

            float[] arr = new float[verts.size()];
            for (int i = 0; i < verts.size(); i++) {
                arr[i] = verts.get(i);
            }

            regions.add(new Region(id, arr));
        }

        return regions;
    }


}
