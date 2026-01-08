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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
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
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import si.apopulis.map.assets.AssetDescriptors;
import si.apopulis.map.assets.RegionNames;
import java.util.List;
import com.badlogic.gdx.math.Vector2;
import java.util.Random;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

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
    private float panelHeight;
    private boolean isPanelPinned = false;

    private float originalZoom;
    private static final float PANEL_ZOOM_OUT = 0.4f;

    private Table bottomRightTable;
    private float zoomButtonsBaseX;

    private String selectedCategory = "Splosno";

    // News markers
    private Array<ProvinceNewsMarker> newsMarkers;
    private float timeSinceLastFetch = 0;
    private static final float FETCH_INTERVAL = 60f; // 60 seconds
    private boolean isFetchingNews = false;
    private Array<NewsItem> displayedNews = new Array<>();

    // News items for side panel
    private Array<NewsItem> newsItems;
    private Table newsContentTable;
    private Table newsWrapperTable;
    private ScrollPane newsScrollPane;
    private boolean isFetchingNewsItems = false;

    // News dots inside selected region
    private ObjectMap<String, Vector2> newsDotCache = new ObjectMap<>();
    private Array<Vector2> selectedNewsDots = new Array<>();

    // Pin
    private List<Marker> markers;
    private SpriteBatch batch;
    private TextureRegion pinRegion;
    private TextureRegion ppjPinRegion;

    private static final float PIN_BASE_W = 14f;
    private static final float PIN_BASE_H = 18f;

    public MapScreen(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.newsMarkers = new Array<>();
        this.newsItems = new Array<>();
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        String geoJson = Gdx.files.internal("markers.geojson").readString();
        markers = GeoJsonMarkerLoader.loadMarkers(geoJson);

        System.out.println("Loaded markers: " + markers.size());

        TextureAtlas uiAtlas = assetManager.get(AssetDescriptors.UI_ATLAS);
        pinRegion = uiAtlas.findRegion(RegionNames.IC_PIN);

        ppjPinRegion = uiAtlas.findRegion(RegionNames.IC_PIN_CITY);


        if (pinRegion == null) {
            System.err.println("ERROR: ic_pin not found in atlas!");
        }

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

        fetchNewsMarkers();

        fetchNewsItems(selectedCategory);
    }

    private void fetchNewsItems(String category) {
        if (isFetchingNewsItems) {
            return;
        }

        isFetchingNewsItems = true;
        System.out.println("Fetching news items for category: " + category);

        NewsApiClient.fetchNewsByCategory(category, 50, new NewsApiClient.NewsItemsCallback() {
            @Override
            public void onSuccess(Array<NewsItem> items) {
                newsItems = items;
                isFetchingNewsItems = false;
                System.out.println("Successfully fetched " + items.size + " news items");

                if (selectedRegion != null) {
                    displayedNews = filterNewsForRegion(selectedRegion);
                } else {
                    displayedNews.clear();
                }
                updateNewsCards();
                rebuildNewsDotsForSelectedRegion();
            }

            @Override
            public void onFailure(Throwable error) {
                isFetchingNewsItems = false;
                System.err.println("Failed to fetch news items: " + error.getMessage());
            }
        });
    }

    private void updateNewsCards() {
        if (newsContentTable == null || assetManager == null) {
            return;
        }

        try {
            newsContentTable.clear();

            Array<NewsItem> sourceNews =
                (selectedRegion != null && displayedNews != null)
                    ? displayedNews
                    : newsItems;

            Array<NewsItem> filteredNews = new Array<>();
            for (NewsItem item : sourceNews) {
                if (selectedCategory.equals("Splosno") ||
                    (item.getCategory() != null && item.getCategory().getName() != null &&
                        item.getCategory().getName().equals(selectedCategory))) {
                    filteredNews.add(item);
                }
            }

            if (filteredNews.size == 0) {
                Label.LabelStyle emptyStyle = new Label.LabelStyle(
                    assetManager.get(AssetDescriptors.UI_FONT),
                    new Color(0.5f, 0.5f, 0.5f, 1f)
                );
                Label emptyLabel = new Label("Ni novic za izbrano kategorijo", emptyStyle);
                emptyLabel.setAlignment(Align.center);

                // Add spacer at top to push content to center
                newsContentTable.add().expandY();
                newsContentTable.row();

                // Add centered label
                newsContentTable.add(emptyLabel)
                    .expandX()
                    .center();
                newsContentTable.row();

                // Add spacer at bottom to fill remaining vertical space
                newsContentTable.add().expandY();
                return;
            }

            BitmapFont uiFont = assetManager.get(AssetDescriptors.UI_FONT);
            Label.LabelStyle titleStyle = new Label.LabelStyle(uiFont, new Color(0.15f, 0.15f, 0.15f, 1f));

            BitmapFont descFont = new BitmapFont(uiFont.getData(), uiFont.getRegions(), false);
            descFont.getData().setScale(0.82f);
            Label.LabelStyle descStyle = new Label.LabelStyle(descFont, new Color(0.45f, 0.45f, 0.45f, 1f));

            float cardWidth = panelWidth - 24;

            for (int i = 0; i < filteredNews.size && i < 10; i++) {
                NewsItem item = filteredNews.get(i);
                if (item != null) {
                    addNewsCard(newsContentTable, item, titleStyle, descStyle, cardWidth);
                }
            }

            // Add spacer row at the end to fill remaining vertical space
            newsContentTable.row();
            newsContentTable.add().expandY();
        } catch (Exception e) {
            System.err.println("Error updating news cards: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void fetchNewsMarkers() {
        if (isFetchingNews) {
            return;
        }

        isFetchingNews = true;
        System.out.println("Fetching province news stats from API...");

        NewsApiClient.fetchProvinceNewsStats(24, new NewsApiClient.ProvinceNewsCallback() {
            @Override
            public void onSuccess(Array<ProvinceNewsMarker> markers) {
                newsMarkers = markers;
                isFetchingNews = false;
                System.out.println("Successfully fetched " + markers.size + " province news markers");
                for (ProvinceNewsMarker marker : markers) {
                    System.out.println("  - " + marker);
                }
            }

            @Override
            public void onFailure(Throwable error) {
                isFetchingNews = false;
                System.err.println("Failed to fetch province news stats: " + error.getMessage());
            }
        });
    }

    private boolean isNewsInsideRegion(NewsItem item, Region region) {
        if (item == null || region == null) return false;
        if (item.getLocation() == null) return false;

        double lat = item.getLocation().getLatitude();
        double lon = item.getLocation().getLongitude();

        if (lat == 0.0 && lon == 0.0) return false;

        float[] map = GeoJsonRegionLoader.latLonToMapCoords((float)lat, (float)lon);
        float x = map[0];
        float y = map[1];

        if (x < region.minX || x > region.maxX || y < region.minY || y > region.maxY) {
            return false;
        }

        return Intersector.isPointInPolygon(region.vertices, 0, region.vertices.length, x, y);
    }

    private Array<NewsItem> filterNewsForRegion(Region region) {
        Array<NewsItem> result = new Array<>();
        if (region == null) return result;

        for (NewsItem item : newsItems) {
            if (item == null) continue;

            if (isNewsInsideRegion(item, region)) {
                result.add(item);
            }
        }

        System.out.println("FILTER DEBUG: region=" + region.id + " -> " + result.size + " items");
        return result;
    }

    private void rebuildNewsDotsForSelectedRegion() {
        selectedNewsDots.clear();

        if (selectedRegion == null || displayedNews == null || displayedNews.size == 0) {
            return;
        }

        for (NewsItem item : displayedNews) {
            if (item == null) continue;

            String cacheKey = item.getId() + "_" + selectedRegion.id;

            Vector2 pos = newsDotCache.get(cacheKey);
            if (pos == null) {
                pos = generateDeterministicPointInRegion(selectedRegion, cacheKey);
                newsDotCache.put(cacheKey, pos);
            }

            selectedNewsDots.add(pos);
        }

        System.out.println("DOTS DEBUG: built " + selectedNewsDots.size + " dots for region=" + selectedRegion.id);
    }

    private Vector2 generateDeterministicPointInRegion(Region region, String seedKey) {
        Random rng = new Random(seedKey.hashCode());

        float minX = region.minX;
        float maxX = region.maxX;
        float minY = region.minY;
        float maxY = region.maxY;

        for (int i = 0; i < 400; i++) {
            float x = minX + rng.nextFloat() * (maxX - minX);
            float y = minY + rng.nextFloat() * (maxY - minY);

            if (Intersector.isPointInPolygon(region.vertices, 0, region.vertices.length, x, y)) {
                return new Vector2(x, y);
            }
        }

        // fallback - centar bounding boxa
        return new Vector2((minX + maxX) * 0.5f, (minY + maxY) * 0.5f);
    }

    private void drawSelectedRegionNewsPins() {
        if (batch == null) return;
        if (selectedRegion == null || selectedNewsDots.size == 0) return;
        if (pinRegion == null) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float w = PIN_BASE_W ;
        float h = PIN_BASE_H ;

        for (Vector2 p : selectedNewsDots) {
            float drawX = p.x - w * 0.5f;
            float drawY = p.y;

            batch.draw(pinRegion, drawX, drawY, w, h);
        }

        batch.end();
    }

    private void drawPPJMarkers() {
        if (markers == null || markers.isEmpty()) return;
        if (batch == null) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (Marker m : markers) {

            float[] map = GeoJsonRegionLoader.latLonToMapCoords(m.lat, m.lon);
            float x = map[0];
            float y = map[1];

            boolean isPPJ = "region".equals(m.level);

            TextureRegion region = isPPJ ? ppjPinRegion : pinRegion;

            float w = isPPJ ? PIN_BASE_W * 1.3f : PIN_BASE_W;
            float h = isPPJ ? PIN_BASE_H * 1.3f : PIN_BASE_H;

            float drawX = x - w * 0.5f;
            float drawY = y;

            batch.draw(region, drawX, drawY, w, h);
        }

        batch.end();
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
        float screenHeight = Gdx.graphics.getHeight();
        panelWidth = screenWidth * 0.33f;
        panelHeight = screenHeight;


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
        categories.addAll("Splosno", "Biznis", "Gospodarstvo", "Kultura",
            "Lifestyle", "Politika", "Tehnologija", "Vreme");

        SelectBox.SelectBoxStyle selectBoxStyle = createSelectBoxStyle(uiFont);
        SelectBox<String> categorySelectBox = new SelectBox<>(selectBoxStyle);
        categorySelectBox.setItems(categories);
        categorySelectBox.setSelected("Splosno");

        categorySelectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                selectedCategory = categorySelectBox.getSelected();
                System.out.println("Selected category: " + selectedCategory);
                fetchNewsItems(selectedCategory);
            }
        });

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
        content.pad(0, 12, 12, 12);
        content.defaults().expandX().fillX();

        newsContentTable = content;
        addNewsCards(content, uiFont);

        // Wrap content in a table that fills ScrollPane viewport
        // This ensures the content table always fills the full height
        Table wrapperTable = new Table();
        wrapperTable.top().left();
        wrapperTable.add(content).expand().fill().top().left();
        // Add expandable row to ensure wrapper fills viewport height
        wrapperTable.row();
        wrapperTable.add().expandY();
        newsWrapperTable = wrapperTable;

        // Set wrapper size to fill viewport (header is ~60px)
        float headerHeight = 60f;
        wrapperTable.setSize(panelWidth, panelHeight - headerHeight);

        // Create ScrollPane style with visible scrollbars
        ScrollPane.ScrollPaneStyle scrollPaneStyle = createNewsScrollPaneStyle();

        ScrollPane scrollPane = new ScrollPane(wrapperTable, scrollPaneStyle);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setScrollbarsVisible(true);
        scrollPane.setScrollBarPositions(false, true); // horizontal: left, vertical: right
        scrollPane.setOverscroll(false, false); // Disable overscroll

        // 🔴 OVO JE KLJUČ
        scrollPane.setForceScroll(false, true); // force vertical logic
        scrollPane.setScrollY(0);               // uvijek počni od vrha
        scrollPane.layout();
        newsScrollPane = scrollPane;

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
        sidePanel.setSize(panelWidth, panelHeight);
        sidePanel.setPosition(Gdx.graphics.getWidth(), 0);
        sidePanel.setTransform(true);
    }

    private void addNewsCards(Table container, BitmapFont font) {
        Label.LabelStyle titleStyle = new Label.LabelStyle(font, new Color(0.15f, 0.15f, 0.15f, 1f));

        BitmapFont descFont = new BitmapFont(font.getData(), font.getRegions(), false);
        descFont.getData().setScale(0.82f);
        Label.LabelStyle descStyle = new Label.LabelStyle(descFont, new Color(0.45f, 0.45f, 0.45f, 1f));

        float cardWidth = panelWidth - 24;

        Label loadingLabel = new Label("Nalaganje novic...", descStyle);
        container.add(loadingLabel).padTop(20);
    }

    private void addNewsCard(Table container, NewsItem item, Label.LabelStyle titleStyle, Label.LabelStyle descStyle, float cardWidth) {
        Table cardWrapper = new Table();
        cardWrapper.left();

        Table accentLine = new Table();
        accentLine.setBackground(createAccentLineBackground());
        cardWrapper.add(accentLine).width(3).minHeight(60).fillY();

        Table card = new Table();
        card.pad(10, 12, 10, 12);
        card.setBackground(createCardBackground());

        String title = item.getTitle() != null && !item.getTitle().isEmpty()
            ? item.getTitle()
            : "Brez naslova";
        Label titleLabel = new Label(title, titleStyle);
        titleLabel.setWrap(true);
        card.add(titleLabel).width(cardWidth - 40).left().top();
        card.row().padTop(6);

        // Summary/Description
        String description = item.getSummary() != null && !item.getSummary().isEmpty()
            ? item.getSummary()
            : (item.getContent() != null && !item.getContent().isEmpty()
            ? item.getContent().substring(0, Math.min(150, item.getContent().length())) + "..."
            : "Ni opisa");
        Label descLabel = new Label(description, descStyle);
        descLabel.setWrap(true);
        card.add(descLabel).width(cardWidth - 40).left().top();

        cardWrapper.add(card).expand().fill();

        container.add(cardWrapper).width(cardWidth).padBottom(8).left();
        container.row();
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createPanelBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.98f, 0.98f, 0.98f, 1f));
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
        pixmap.fillRectangle(0, 0, size, 1);
        pixmap.fillRectangle(0, 0, 1, size);
        pixmap.setColor(new Color(0.95f, 0.95f, 0.95f, 1f));
        pixmap.fillRectangle(size - 1, 0, 1, size);
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
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.6f, 0.45f, 0.75f, 1f));
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
        style.fontColor = new Color(0.15f, 0.15f, 0.15f, 1f);
        style.background = createDropdownBackgroundOpen();
        style.backgroundOpen = createDropdownBackground();
        style.scrollStyle = createScrollPaneStyle();
        style.listStyle = createListStyle(font);
        return style;
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createDropdownBackground() {
        int width = 200;
        int height = 38;
        int leftPadding = 12;
        int rightPadding = 30;
        int arrowSize = 7;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        pixmap.setColor(new Color(1f, 1f, 1f, 1f));
        pixmap.fill();

        pixmap.setColor(new Color(0.9f, 0.9f, 0.9f, 1f));
        pixmap.drawRectangle(0, 0, width, height);

        pixmap.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));
        int arrowX = width - rightPadding / 2;
        int arrowY = height / 2;

        int topY = arrowY - arrowSize / 2;
        int bottomY = arrowY + arrowSize / 2;

        for (int y = topY; y <= bottomY; y++) {
            if (y < 0 || y >= height) continue;

            int distFromTop = y - topY;

            int halfWidth = (distFromTop * arrowSize) / arrowSize;

            int leftX = arrowX - halfWidth;
            int rightX = arrowX + halfWidth;

            for (int x = leftX; x <= rightX; x++) {
                if (x >= 0 && x < width) {
                    pixmap.drawPixel(x, y);
                }
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        NinePatch ninePatch = new NinePatch(texture, leftPadding, rightPadding, 0, 0);
        return new NinePatchDrawable(ninePatch);
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createDropdownBackgroundOpen() {
        int width = 200;
        int height = 38;
        int leftPadding = 12;
        int rightPadding = 30;
        int arrowSize = 7;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        pixmap.setColor(new Color(1f, 1f, 1f, 1f));
        pixmap.fill();

        pixmap.setColor(new Color(0.9f, 0.9f, 0.9f, 1f));
        pixmap.drawRectangle(0, 0, width, height);

        pixmap.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));
        int arrowX = width - rightPadding / 2;
        int arrowY = height / 2;

        int topY = arrowY - arrowSize / 2;
        int bottomY = arrowY + arrowSize / 2;

        for (int y = topY; y <= bottomY; y++) {
            if (y < 0 || y >= height) continue;

            int distFromBottom = bottomY - y;

            int halfWidth = (distFromBottom * arrowSize) / arrowSize;

            int leftX = arrowX - halfWidth;
            int rightX = arrowX + halfWidth;

            for (int x = leftX; x <= rightX; x++) {
                if (x >= 0 && x < width) {
                    pixmap.drawPixel(x, y);
                }
            }
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        NinePatch ninePatch = new NinePatch(texture, leftPadding, rightPadding, 0, 0);
        return new NinePatchDrawable(ninePatch);
    }

    private ScrollPane.ScrollPaneStyle createScrollPaneStyle() {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        style.background = createDropdownListBackground();
        return style;
    }

    private ScrollPane.ScrollPaneStyle createNewsScrollPaneStyle() {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        style.vScroll = createScrollBarDrawable();
        style.vScrollKnob = createScrollBarKnobDrawable();
        style.hScroll = createScrollBarDrawable();
        style.hScrollKnob = createScrollBarKnobDrawable();
        return style;
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createScrollBarDrawable() {
        int width = 8;
        int height = 8;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.7f, 0.7f, 0.7f, 0.5f));
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createScrollBarKnobDrawable() {
        int width = 8;
        int height = 8;
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.5f, 0.5f, 0.5f, 0.8f));
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
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

        pixmap.drawLine(0, height - 1, width, height - 1);
        pixmap.drawLine(width - 1, 0, width - 1, height);
        pixmap.drawLine(0, 0, width, 0);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        NinePatch ninePatch = new NinePatch(
            texture,
            4,
            0,
            0,
            0
        );

        return new NinePatchDrawable(ninePatch);
    }


    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createDropdownSelectionBackground() {
        int width = 200;
        int height = 30;

        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);

        pixmap.setColor(new Color(0.6f, 0.45f, 0.75f, 1f));
        pixmap.fill();

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        NinePatch ninePatch = new NinePatch(
            texture,
            4,
            0,
            0,
            0
        );

        return new NinePatchDrawable(ninePatch);
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
        // Update fetch timer
        timeSinceLastFetch += delta;
        if (timeSinceLastFetch >= FETCH_INTERVAL) {
            timeSinceLastFetch = 0;
            fetchNewsMarkers();
            // Also refresh news items when refreshing markers
            fetchNewsItems(selectedCategory);
        }

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

            displayedNews = filterNewsForRegion(selectedRegion);
            rebuildNewsDotsForSelectedRegion();
            updateNewsCards();

            // auto-open panel
            if (!isPanelOpen) {
                toggleSidePanel();
            }
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

        // Draw borders
        drawBorders();

        // Draw news markers
        drawSelectedRegionNewsPins();

        drawPPJMarkers();

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
            panelHeight = height;
            sidePanel.setSize(panelWidth, height);
            float targetX = isPanelOpen ? width - panelWidth : width;
            sidePanel.setPosition(targetX, 0);
        }

        // Update wrapper table size to fill ScrollPane viewport
        // Header is approximately 60px (36px selectbox + 24px padding)
        if (newsWrapperTable != null && newsScrollPane != null) {
            float headerHeight = 60f;
            newsWrapperTable.setSize(panelWidth, panelHeight - headerHeight);
            newsWrapperTable.invalidate();
            newsScrollPane.invalidate();
        }

        // Invalidate layout to ensure expandable rows recalculate
        if (newsContentTable != null) {
            newsContentTable.invalidate();
        }

    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        if (batch != null) batch.dispose();
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
