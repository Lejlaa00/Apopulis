package si.apopulis.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.utils.ShortArray;

import java.util.List;

public class MapScreen implements Screen {

    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;

    private static final float WORLD_WIDTH = 800;
    private static final float WORLD_HEIGHT = 600;

    private List<Region> regions;
    private EarClippingTriangulator triangulator = new EarClippingTriangulator();


    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();

        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.update();

        regions = GeoJsonRegionLoader.loadAllRegions();
        System.out.println("Loaded regions: " + regions.size());
    }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Regions
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.75f, 0.6f, 0.85f, 1f));

        for (Region region : regions) {
            ShortArray triangles = triangulator.computeTriangles(region.vertices);

            for (int i = 0; i < triangles.size; i += 3) {
                int i1 = triangles.get(i) * 2;
                int i2 = triangles.get(i + 1) * 2;
                int i3 = triangles.get(i + 2) * 2;

                shapeRenderer.triangle(
                    region.vertices[i1],     region.vertices[i1 + 1],
                    region.vertices[i2],     region.vertices[i2 + 1],
                    region.vertices[i3],     region.vertices[i3 + 1]
                );
            }
        }
        shapeRenderer.end();

        // Borders
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);

        for (Region region : regions) {
            shapeRenderer.polygon(region.vertices);
        }

        shapeRenderer.end();
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
