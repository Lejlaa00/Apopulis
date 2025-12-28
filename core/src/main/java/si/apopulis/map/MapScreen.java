package si.apopulis.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.utils.ShortArray;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import si.apopulis.map.assets.AssetDescriptors;
import si.apopulis.map.assets.RegionNames;


import java.util.List;

public class MapScreen implements Screen {

    private final AssetManager assetManager;

    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera;
    private Viewport viewport;

    private static final float WORLD_WIDTH = 800;
    private static final float WORLD_HEIGHT = 600;

    private List<Region> regions;
    private Region hoveredRegion = null;
    private Region selectedRegion = null;

    private EarClippingTriangulator triangulator = new EarClippingTriangulator();

    // Map bounds for camera constraints
    private float mapMinX = Float.MAX_VALUE;
    private float mapMaxX = Float.MIN_VALUE;
    private float mapMinY = Float.MAX_VALUE;
    private float mapMaxY = Float.MIN_VALUE;

    // Panning state
    private boolean isPanning = false;
    private float lastScreenX = 0;
    private float lastScreenY = 0;
    private static final float DRAG_THRESHOLD = 5.0f;

    // Zoom constants
    private static final float MIN_ZOOM = 0.1f;
    private static final float MAX_ZOOM = 3.0f;
    private static final float ZOOM_SPEED = 0.1f;
    private static final float DEFAULT_ZOOM = 1.0f;

    // UI components
    private Stage uiStage;

    private ImageButton zoomInButton;
    private ImageButton zoomOutButton;
    private ImageButton hamburgerButton;
    private Container<Table> sidePanel;
    private boolean isPanelOpen = false;
    private float panelWidth;
    private float effectiveWorldWidth = WORLD_WIDTH;
    private boolean isPanelPinned = false;

    private float originalZoom;
    private static final float PANEL_ZOOM_OUT = 0.4f;

    private Table bottomRightTable;
    private float zoomButtonsBaseX;


    public MapScreen(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();

        camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
        camera.zoom = DEFAULT_ZOOM;
        camera.update();

        regions = GeoJsonRegionLoader.loadAllRegions();
        System.out.println("Loaded regions: " + regions.size());

        calculateMapBounds();

        // Clamp initial camera position to map bounds
        clampCameraToMap();

        setupUI();

        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(uiStage);
        inputMultiplexer.addProcessor(new MapInputProcessor());
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    /**
     * Calculates the overall bounding box of the map from all regions.
     * This is used to constrain camera movement so the map always stays visible.
     */
    private void calculateMapBounds() {
        mapMinX = Float.MAX_VALUE;
        mapMaxX = Float.MIN_VALUE;
        mapMinY = Float.MAX_VALUE;
        mapMaxY = Float.MIN_VALUE;

        for (Region region : regions) {
            mapMinX = Math.min(mapMinX, region.minX);
            mapMaxX = Math.max(mapMaxX, region.maxX);
            mapMinY = Math.min(mapMinY, region.minY);
            mapMaxY = Math.max(mapMaxY, region.maxY);
        }

        System.out.println("Map bounds: X[" + mapMinX + ", " + mapMaxX + "] Y[" + mapMinY + ", " + mapMaxY + "]");
    }

    /**
     * Clamps the camera position so that the visible area always intersects the map.
     *
     * How it works:
     * 1. Calculate the visible area based on viewport size and current zoom level
     * 2. The visible area extends from (camera.x - visibleWidth/2) to (camera.x + visibleWidth/2)
     * 3. Ensure the visible area overlaps with the map bounds
     * 4. When zoomed out (high zoom value > 1.0), the visible area is larger, so constraints are tighter
     * 5. When zoomed in (low zoom value < 1.0), the visible area is smaller, so more panning freedom is allowed
     *
     * Note: In libGDX, zoom > 1.0 means zoomed out (larger visible area), zoom < 1.0 means zoomed in (smaller visible area)
     */
    private void clampCameraToMap() {
        // Calculate the visible area dimensions based on zoom
        // Higher zoom (> 1.0) = larger visible area (zoomed out)
        // Lower zoom (< 1.0) = smaller visible area (zoomed in)

        if (isPanelPinned) {
            camera.update();
            return;
        }

        float effectiveWidth = isPanelPinned
            ? WORLD_WIDTH - panelWidth
            : WORLD_WIDTH;

        float visibleWidth = effectiveWidth * camera.zoom;
        float visibleHeight = WORLD_HEIGHT * camera.zoom;

        // Calculate half-dimensions for easier calculations
        float halfVisibleWidth = visibleWidth * 0.5f;
        float halfVisibleHeight = visibleHeight * 0.5f;

        // Calculate the map dimensions
        float mapWidth = mapMaxX - mapMinX;
        float mapHeight = mapMaxY - mapMinY;

        // If the visible area is larger than the map, center the camera on the map
        if (visibleWidth >= mapWidth) {
            camera.position.x = (mapMinX + mapMaxX) * 0.5f;
        } else {
            // Constrain camera X position so visible area stays within map bounds
            // Left edge constraint: camera.x - halfVisibleWidth >= mapMinX
            // Right edge constraint: camera.x + halfVisibleWidth <= mapMaxX
            float minCameraX = mapMinX + halfVisibleWidth;
            float maxCameraX = mapMaxX - halfVisibleWidth;
            camera.position.x = Math.max(minCameraX, Math.min(maxCameraX, camera.position.x));
        }

        // Same logic for Y axis
        if (visibleHeight >= mapHeight) {
            camera.position.y = (mapMinY + mapMaxY) * 0.5f;
        } else {
            // Bottom edge constraint: camera.y - halfVisibleHeight >= mapMinY
            // Top edge constraint: camera.y + halfVisibleHeight <= mapMaxY
            float minCameraY = mapMinY + halfVisibleHeight;
            float maxCameraY = mapMaxY - halfVisibleHeight;
            camera.position.y = Math.max(minCameraY, Math.min(maxCameraY, camera.position.y));
        }

        camera.update();
    }

    private void setupUI() {
        uiStage = new Stage(new ScreenViewport());

        // Get assets from AssetManager
        TextureAtlas uiAtlas = assetManager.get(AssetDescriptors.UI_ATLAS);
        BitmapFont uiFont = assetManager.get(AssetDescriptors.UI_FONT);

        // Calculate panel width (30-35% of screen width)
        float screenWidth = Gdx.graphics.getWidth();
        panelWidth = screenWidth * 0.33f;

        // Setup zoom buttons
        setupZoomButtons(uiAtlas);

        // Setup hamburger button
        setupHamburgerButton(uiAtlas);

        // Setup side panel
        setupSidePanel(uiFont);

        // Main UI table
        Table mainTable = new Table();
        mainTable.setFillParent(true);

        // Top-right corner for hamburger button
        Table topRightTable = new Table();
        topRightTable.setFillParent(true);
        topRightTable.top().right();
        topRightTable.pad(20);
        topRightTable.add(hamburgerButton).size(32, 32);

        // Bottom-right corner for zoom buttons
        bottomRightTable = new Table();
        bottomRightTable.setFillParent(true);
        bottomRightTable.bottom().right();
        bottomRightTable.pad(20);
        bottomRightTable.add(zoomInButton).size(32, 32).padBottom(10);
        bottomRightTable.row();
        bottomRightTable.add(zoomOutButton).size(32, 32);

        bottomRightTable.right();

        uiStage.addActor(topRightTable);
        uiStage.addActor(bottomRightTable);
        uiStage.addActor(sidePanel);

        zoomButtonsBaseX = bottomRightTable.getX();
    }

    private void setupZoomButtons(TextureAtlas uiAtlas) {
        zoomInButton = new ImageButton(
            new TextureRegionDrawable(uiAtlas.findRegion(RegionNames.BTN_PLUS))
        );
        zoomOutButton = new ImageButton(
            new TextureRegionDrawable(uiAtlas.findRegion(RegionNames.BTN_MINUS))
        );

        zoomInButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                zoomIn();
            }
        });

        zoomOutButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                zoomOut();
            }
        });
    }

    private void setupHamburgerButton(TextureAtlas uiAtlas) {
        hamburgerButton = new ImageButton(
            new TextureRegionDrawable(uiAtlas.findRegion(RegionNames.BTN_HAMBURGER))
        );

        hamburgerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleSidePanel();
            }
        });
    }

    private void setupSidePanel(BitmapFont uiFont) {
        TextureAtlas uiAtlas = assetManager.get(AssetDescriptors.UI_ATLAS);

        // =========================
        // HEADER (EXIT BUTTON)
        // =========================
        ImageButton exitButton = new ImageButton(
            new TextureRegionDrawable(uiAtlas.findRegion(RegionNames.BTN_EXIT))
        );

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleSidePanel();
            }
        });

        Table header = new Table();
        header.top().right();
        header.add(exitButton).size(24, 24);

        // =========================
        // CONTENT (NEWS CARDS)
        // =========================
        Table content = new Table();
        content.top().left();
        content.pad(16);

        addNewsCards(content, uiFont);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        // =========================
        // PANEL ROOT
        // =========================
        Table panelTable = new Table();
        panelTable.setBackground(createPanelBackground());
        panelTable.top();

        panelTable.add(header)
            .expandX()
            .fillX()
            .padBottom(10);
        panelTable.row();

        panelTable.add(scrollPane)
            .expand()
            .fill();

        // =========================
        // CONTAINER (SLIDE PANEL)
        // =========================
        sidePanel = new Container<>(panelTable);
        sidePanel.setSize(panelWidth, Gdx.graphics.getHeight());
        sidePanel.setPosition(Gdx.graphics.getWidth(), 0); // off-screen
        sidePanel.setTransform(true);
    }

    private void addNewsCards(Table container, BitmapFont font) {
        // Mock news data
        String[] newsTitles = {
            "Novice o regionalnem razvoju",
            "Aktualne spremembe v zakonodaji",
            "Nova infrastrukturna nalozba",
            "Sodelovanje med regijami",
            "Okoljske pobude v regiji"
        };

        String[] newsDescriptions = {
            "Pregled najnovejših dogodkov in razvojnih projektov v regiji.",
            "Pomembne spremembe zakonodaje, ki vplivajo na lokalno skupnost.",
            "Predstavitev nove infrastrukturne naložbe, ki bo izboljsala povezanost.",
            "Pregled projektov sodelovanja med razlicnimi regijami drzave.",
            "Okoljske pobude in trajnostni razvojni projekti v regiji."
        };

        Label.LabelStyle titleStyle = new Label.LabelStyle(font, new Color(0.2f, 0.2f, 0.2f, 1f));
        Label.LabelStyle descStyle = new Label.LabelStyle(font, new Color(0.5f, 0.5f, 0.5f, 1f));

        for (int i = 0; i < newsTitles.length; i++) {
            // Create card container with border effect
            Table card = new Table();
            card.pad(15);
            card.setBackground(createCardBackground());

            // Title
            Label titleLabel = new Label(newsTitles[i], titleStyle);
            titleLabel.setWrap(true);
            card.add(titleLabel).width(panelWidth - 50).left().top();
            card.row().padTop(8);

            // Description
            Label descLabel = new Label(newsDescriptions[i], descStyle);
            descLabel.setWrap(true);
            card.add(descLabel).width(panelWidth - 50).left().top();

            // Add card to container with spacing and subtle separator
            container.add(card).width(panelWidth - 20).padBottom(12).left();
            container.row();

            // Add subtle separator line (except after last card)
            if (i < newsTitles.length - 1) {
                Table separator = new Table();
                separator.setBackground(createSeparatorBackground());
                container.add(separator).width(panelWidth - 20).height(1).padBottom(3).left();
                container.row();
            }
        }
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createPanelBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.98f, 0.98f, 0.98f, 1f)); // Light background
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)
        );
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createCardBackground() {
        // Create a white background
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)
        );
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createSeparatorBackground() {
        // Create a subtle separator line
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.85f, 0.85f, 0.85f, 1f));
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)
        );
    }

    private void toggleSidePanel() {
        isPanelOpen = !isPanelOpen;

        float panelTargetX = isPanelOpen
            ? Gdx.graphics.getWidth() - panelWidth
            : Gdx.graphics.getWidth();

        sidePanel.clearActions();
        sidePanel.addAction(Actions.moveTo(panelTargetX, 0, 0.3f));

        // =========================
        // ZOOM BUTTONS – FIXED LOGIC
        // =========================
        float buttonsTargetX = isPanelOpen
            ? zoomButtonsBaseX - panelWidth
            : zoomButtonsBaseX;

        bottomRightTable.clearActions();
        bottomRightTable.addAction(
            Actions.moveTo(buttonsTargetX, bottomRightTable.getY(), 0.3f)
        );

        adjustCameraForPanel(isPanelOpen);
    }



    private void adjustCameraForPanel(boolean open) {

        float pixelShift = panelWidth * 0.47f;     // koliko panela zauzima ekran
        float worldShift = pixelShift * camera.zoom; // PRETVARANJE U WORLD SPACE

        if (open) {
            originalZoom = camera.zoom;

            // kamera ide DESNO → mapa ide LIJEVO
            camera.position.x += pixelShift;

            // dodatni zoom out
            camera.zoom = Math.min(MAX_ZOOM, camera.zoom + PANEL_ZOOM_OUT);

            isPanelPinned = true;
        } else {
            camera.position.x -= worldShift;
            camera.zoom = originalZoom;
            isPanelPinned = false;
        }

        camera.update();
        clampCameraToMap();
    }


    private void zoomIn() {
        float newZoom = camera.zoom - ZOOM_SPEED;
        setZoom(newZoom);
    }

    private void zoomOut() {
        float newZoom = camera.zoom + ZOOM_SPEED;
        setZoom(newZoom);
    }

    private void setZoom(float zoom) {
        camera.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
        camera.update();
        clampCameraToMap();
    }


    private void zoomAtPoint(int screenX, int screenY, float zoomDelta) {
        // Convert screen coordinates to world coordinates before zoom
        Vector3 worldPosBefore = new Vector3(screenX, screenY, 0);
        viewport.unproject(worldPosBefore);

        // Apply zoom change
        float newZoom = camera.zoom + zoomDelta;
        camera.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        camera.update();

        // Convert screen coordinates to world coordinates after zoom
        Vector3 worldPosAfter = new Vector3(screenX, screenY, 0);
        viewport.unproject(worldPosAfter);

        // Adjust camera position to keep the point under cursor fixed
        camera.position.add(worldPosBefore.x - worldPosAfter.x, worldPosBefore.y - worldPosAfter.y, 0);
        camera.update();

        // Clamp camera to ensure map stays visible after zoom
        clampCameraToMap();
    }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        uiStage.act(delta);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        Vector3 mousePos = new Vector3(
            Gdx.input.getX(),
            Gdx.input.getY(),
            0
        );
        camera.unproject(mousePos);

        updateHover(mousePos);

        if (Gdx.input.justTouched() && hoveredRegion != null) {
            selectedRegion = hoveredRegion;
            System.out.println("Clicked region: " + selectedRegion.id);
        }

        Color regionColor = new Color(0.75f, 0.6f, 0.85f, 1f);
        Color hoverColor  = new Color(0.6f, 0.45f, 0.75f, 1f);

        // Draw all regions
        drawRegions(regionColor);

        // Draw hovered region
        if (hoveredRegion != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(hoverColor);
            drawRegion(hoveredRegion, hoveredRegion.vertices);
            shapeRenderer.end();
        }

        // Draw borders
        drawBorders();

        // Draw UI on top
        uiStage.draw();
    }

    private void updateHover(Vector3 mousePos) {
        hoveredRegion = null;

        for (Region region : regions) {

            if (mousePos.x < region.minX || mousePos.x > region.maxX ||
                mousePos.y < region.minY || mousePos.y > region.maxY) {
                continue;
            }

            if (Intersector.isPointInPolygon(
                region.vertices, 0, region.vertices.length,
                mousePos.x, mousePos.y)) {

                hoveredRegion = region;
                break;
            }
        }
    }

    private void drawRegions(Color color) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(color);

        for (Region region : regions) {
            drawRegion(region, region.vertices);
        }

        shapeRenderer.end();
    }

    private void drawRegion(Region region, float[] verts) {
        ShortArray triangles = triangulator.computeTriangles(verts);

        for (int i = 0; i < triangles.size; i += 3) {
            int i1 = triangles.get(i) * 2;
            int i2 = triangles.get(i + 1) * 2;
            int i3 = triangles.get(i + 2) * 2;

            shapeRenderer.triangle(
                verts[i1], verts[i1 + 1],
                verts[i2], verts[i2 + 1],
                verts[i3], verts[i3 + 1]
            );
        }
    }

    private void drawBorders() {
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
        uiStage.getViewport().update(width, height, true);

        // Update panel width and position on resize
        if (sidePanel != null) {
            panelWidth = width * 0.33f;
            sidePanel.setSize(panelWidth, height);
            float targetX = isPanelOpen ? width - panelWidth : width;
            sidePanel.setPosition(targetX, 0);
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        uiStage.dispose();
        // Note: Assets managed by AssetManager should not be disposed here
        // They will be disposed when AssetManager is disposed in ApopulisMap
    }

    private class MapInputProcessor extends InputAdapter {

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            lastScreenX = screenX;
            lastScreenY = screenY;
            isPanning = false;
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            float deltaScreenX = screenX - lastScreenX;
            float deltaScreenY = screenY - lastScreenY;
            float distance = (float) Math.sqrt(deltaScreenX * deltaScreenX + deltaScreenY * deltaScreenY);

            if (distance > DRAG_THRESHOLD || isPanning) {
                Vector3 lastWorldPos = new Vector3(lastScreenX, lastScreenY, 0);
                Vector3 currentWorldPos = new Vector3(screenX, screenY, 0);

                viewport.unproject(lastWorldPos);
                viewport.unproject(currentWorldPos);

                float deltaWorldX = lastWorldPos.x - currentWorldPos.x;
                float deltaWorldY = lastWorldPos.y - currentWorldPos.y;

                camera.position.add(deltaWorldX, deltaWorldY, 0);
                camera.update();

                // Clamp camera to ensure map stays visible after panning
                clampCameraToMap();

                isPanning = true;
            }

            lastScreenX = screenX;
            lastScreenY = screenY;

            return isPanning;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            isPanning = false;
            return false;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            float zoomDelta = amountY * ZOOM_SPEED;
            zoomAtPoint(Gdx.input.getX(), Gdx.input.getY(), zoomDelta);
            return true;
        }
    }
}
