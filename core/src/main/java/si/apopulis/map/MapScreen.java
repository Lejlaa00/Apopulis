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
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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
    private boolean isPanelPinned = false;

    private float originalZoom;
    private static final float PANEL_ZOOM_OUT = 0.4f;

    private Table bottomRightTable;
    private float zoomButtonsBaseX;

    private String selectedCategory = "Splošno";

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

        clampCameraToMap();

        setupUI();

        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(uiStage);
        inputMultiplexer.addProcessor(new MapInputProcessor());
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

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

    private void clampCameraToMap() {
        if (isPanelPinned) {
            camera.update();
            return;
        }

        float effectiveWidth = isPanelPinned
            ? WORLD_WIDTH - panelWidth
            : WORLD_WIDTH;

        float visibleWidth = effectiveWidth * camera.zoom;
        float visibleHeight = WORLD_HEIGHT * camera.zoom;

        float halfVisibleWidth = visibleWidth * 0.5f;
        float halfVisibleHeight = visibleHeight * 0.5f;

        float mapWidth = mapMaxX - mapMinX;
        float mapHeight = mapMaxY - mapMinY;

        if (visibleWidth >= mapWidth) {
            camera.position.x = (mapMinX + mapMaxX) * 0.5f;
        } else {
            float minCameraX = mapMinX + halfVisibleWidth;
            float maxCameraX = mapMaxX - halfVisibleWidth;
            camera.position.x = Math.max(minCameraX, Math.min(maxCameraX, camera.position.x));
        }

        if (visibleHeight >= mapHeight) {
            camera.position.y = (mapMinY + mapMaxY) * 0.5f;
        } else {
            float minCameraY = mapMinY + halfVisibleHeight;
            float maxCameraY = mapMaxY - halfVisibleHeight;
            camera.position.y = Math.max(minCameraY, Math.min(maxCameraY, camera.position.y));
        }

        camera.update();
    }

    private void setupUI() {
        uiStage = new Stage(new ScreenViewport());

        TextureAtlas uiAtlas = assetManager.get(AssetDescriptors.UI_ATLAS);
        BitmapFont uiFont = assetManager.get(AssetDescriptors.UI_FONT);

        float screenWidth = Gdx.graphics.getWidth();
        panelWidth = screenWidth * 0.33f;

        setupZoomButtons(uiAtlas);

        setupHamburgerButton(uiAtlas);

        setupSidePanel(uiFont);

        Table mainTable = new Table();
        mainTable.setFillParent(true);

        Table topRightTable = new Table();
        topRightTable.setFillParent(true);
        topRightTable.top().right();
        topRightTable.pad(20);
        topRightTable.add(hamburgerButton).size(32, 32);

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

        Array<String> categories = new Array<>();
        categories.addAll("Splošno", "Biznis", "Gospodarstvo", "Kultura",
            "Lifestyle", "Politika", "Tehnologija", "Vreme");

        SelectBox.SelectBoxStyle selectBoxStyle = createSelectBoxStyle(uiFont);
        SelectBox<String> categorySelectBox = new SelectBox<>(selectBoxStyle);
        categorySelectBox.setItems(categories);
        categorySelectBox.setSelected("Splošno");

        categorySelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                selectedCategory = categorySelectBox.getSelected();
                System.out.println("Selected category: " + selectedCategory);
            }
        });

        // Exit button
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
        header.top().left();
        header.pad(12);
        header.add(categorySelectBox).left().height(36).minWidth(150);
        header.add().expandX();
        header.add(exitButton).size(24, 24).right();

        Table content = new Table();
        content.top().left();
        content.pad(12, 12, 12, 12);

        addNewsCards(content, uiFont);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

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

        sidePanel = new Container<>(panelTable);
        sidePanel.setSize(panelWidth, Gdx.graphics.getHeight());
        sidePanel.setPosition(Gdx.graphics.getWidth(), 0);
        sidePanel.setTransform(true);
    }

    private void addNewsCards(Table container, BitmapFont font) {
        String[] newsTitles = {
            "Novice o regionalnem razvoju",
            "Aktualne spremembe v zakonodaji",
            "Nova infrastrukturna nalozba",
            "Sodelovanje med regijami",
            "Okoljske pobude v regiji"
        };

        String[] newsDescriptions = {
            "Pregled najnovejsih dogodkov in razvojnih projektov v regiji.",
            "Pomembne spremembe zakonodaje, ki vplivajo na lokalno skupnost.",
            "Predstavitev nove infrastrukturne nalozbe, ki bo izboljsala povezanost.",
            "Pregled projektov sodelovanja med razlicnimi regijami drzave.",
            "Okoljske pobude in trajnostni razvojni projekti v regiji."
        };

        Label.LabelStyle titleStyle = new Label.LabelStyle(font, new Color(0.15f, 0.15f, 0.15f, 1f));

        BitmapFont descFont = new BitmapFont(font.getData(), font.getRegions(), false);
        descFont.getData().setScale(0.82f);
        Label.LabelStyle descStyle = new Label.LabelStyle(descFont, new Color(0.45f, 0.45f, 0.45f, 1f));

        float cardWidth = panelWidth - 24;

        for (int i = 0; i < newsTitles.length; i++) {
            Table cardWrapper = new Table();
            cardWrapper.left();

            Table accentLine = new Table();
            accentLine.setBackground(createAccentLineBackground());
            cardWrapper.add(accentLine).width(3).minHeight(60).fillY();

            Table card = new Table();
            card.pad(10, 12, 10, 12);
            card.setBackground(createCardBackground());

            Label titleLabel = new Label(newsTitles[i], titleStyle);
            titleLabel.setWrap(true);
            card.row().padTop(6);

            Label descLabel = new Label(newsDescriptions[i], descStyle);
            descLabel.setWrap(true);
            card.add(descLabel).width(cardWidth - 40).left().top();

            cardWrapper.add(card).expand().fill();

            container.add(cardWrapper).width(cardWidth).padBottom(8).left();
            container.row();
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
        int size = 100;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        pixmap.setColor(new Color(1f, 1f, 1f, 1f));
        pixmap.fill();

        pixmap.setColor(new Color(0.92f, 0.92f, 0.92f, 1f));
        // Top border
        pixmap.fillRectangle(0, 0, size, 1);
        // Left border
        pixmap.fillRectangle(0, 0, 1, size);
        // Right border (softer)
        pixmap.setColor(new Color(0.95f, 0.95f, 0.95f, 1f));
        pixmap.fillRectangle(size - 1, 0, 1, size);
        // Bottom border (softer, simulating shadow)
        pixmap.setColor(new Color(0.94f, 0.94f, 0.94f, 1f));
        pixmap.fillRectangle(0, size - 1, size, 1);

        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        texture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)
        );
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createAccentLineBackground() {
        // Create a thin colored accent line (left side of card)
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        // Use a subtle accent color (soft blue/purple to match the map theme)
        pixmap.setColor(new Color(0.6f, 0.45f, 0.75f, 1f)); // Matches hover color from map
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)
        );
    }

    private SelectBox.SelectBoxStyle createSelectBoxStyle(BitmapFont font) {
        SelectBox.SelectBoxStyle style = new SelectBox.SelectBoxStyle();
        style.font = font;
        style.fontColor = new Color(0.15f, 0.15f, 0.15f, 1f); // Dark text
        style.background = createDropdownBackground();
        style.scrollStyle = createScrollPaneStyle();
        style.listStyle = createListStyle(font);
        return style;
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createDropdownBackground() {
        int width = 200;
        int height = 38;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        pixmap.setColor(new Color(1f, 1f, 1f, 1f));
        pixmap.fill();

        pixmap.setColor(new Color(0.9f, 0.9f, 0.9f, 1f));
        pixmap.drawRectangle(0, 0, width, height);

        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        texture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        pixmap.dispose();

        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)
        );
    }

    private ScrollPane.ScrollPaneStyle createScrollPaneStyle() {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        style.background = createDropdownListBackground();
        return style;
    }

    private com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle createListStyle(BitmapFont font) {
        com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle style = new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle();
        style.font = font;
        style.fontColorSelected = new Color(1f, 1f, 1f, 1f);
        style.fontColorUnselected = new Color(0.15f, 0.15f, 0.15f, 1f);
        style.selection = createDropdownSelectionBackground();
        style.background = createDropdownListBackground();
        return style;
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createDropdownListBackground() {
        int width = 200;
        int height = 200;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        pixmap.setColor(new Color(1f, 1f, 1f, 1f));
        pixmap.fill();

        pixmap.setColor(new Color(0.9f, 0.9f, 0.9f, 1f));
        pixmap.drawRectangle(0, 0, width, height);

        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        texture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        pixmap.dispose();

        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)
        );
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createDropdownSelectionBackground() {
        int width = 200;
        int height = 30;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        pixmap.setColor(new Color(0.6f, 0.45f, 0.75f, 1f));
        pixmap.fill();

        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        texture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
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

        float pixelShift = panelWidth * 0.47f;
        float worldShift = pixelShift * camera.zoom;

        if (open) {
            originalZoom = camera.zoom;

            camera.position.x += pixelShift;

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
        Vector3 worldPosBefore = new Vector3(screenX, screenY, 0);
        viewport.unproject(worldPosBefore);

        float newZoom = camera.zoom + zoomDelta;
        camera.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        camera.update();

        Vector3 worldPosAfter = new Vector3(screenX, screenY, 0);
        viewport.unproject(worldPosAfter);

        camera.position.add(worldPosBefore.x - worldPosAfter.x, worldPosBefore.y - worldPosAfter.y, 0);
        camera.update();

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

        drawRegions(regionColor);

        if (hoveredRegion != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(hoverColor);
            drawRegion(hoveredRegion, hoveredRegion.vertices);
            shapeRenderer.end();
        }

        drawBorders();

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
