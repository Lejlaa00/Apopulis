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
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
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
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import si.apopulis.map.assets.AssetDescriptors;
import si.apopulis.map.assets.RegionNames;
import si.apopulis.map.model.Marker;
import si.apopulis.map.model.NewsItem;
import si.apopulis.map.model.Region;

import java.util.List;
import com.badlogic.gdx.math.Vector2;
import java.util.Random;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

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

    private float mapMinX = Float.MAX_VALUE;
    private float mapMaxX = Float.MIN_VALUE;
    private float mapMinY = Float.MAX_VALUE;
    private float mapMaxY = Float.MIN_VALUE;

    private boolean isPanning = false;
    private float lastScreenX = 0;
    private float lastScreenY = 0;
    private static final float DRAG_THRESHOLD = 5.0f;

    private boolean uiConsumedTouch = false;

    // Zoom constants
    private static final float MIN_ZOOM = 0.1f;
    private static final float MAX_ZOOM = 3.0f;
    private static final float ZOOM_SPEED = 0.1f;
    private static final float DEFAULT_ZOOM = 1.0f;

    // Smooth camera animation for region selection
    private boolean isAnimatingCamera = false;
    private boolean isRegionZoomed = false;
    private float targetZoom;
    private Vector2 targetPosition = new Vector2();
    private Vector2 startPosition = new Vector2();
    private float startZoom;
    private float animationProgress = 0f;
    private static final float ZOOM_ANIMATION_DURATION = 0.65f;
    private static final float REGION_ZOOM_IN_AMOUNT = 0.28f;

    private boolean isTwoPhaseAnimation = false;
    private float intermediateZoom;
    private Vector2 intermediatePosition = new Vector2();
    private float phase1Progress = 0f;
    private float phase2Progress = 0f;

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
    private Table categoryChipsWrapper;

    private Array<ProvinceNewsMarker> newsMarkers;
    private float timeSinceLastFetch = 0;
    private static final float FETCH_INTERVAL = 60f;
    private boolean isFetchingNews = false;
    private Array<NewsItem> displayedNews = new Array<>();

    private Array<NewsItem> newsItems;
    private Table newsContentTable;
    private Table newsWrapperTable;
    private ScrollPane newsScrollPane;
    private boolean isFetchingNewsItems = false;

    private ObjectMap<String, Vector2> newsDotCache = new ObjectMap<>();
    private Array<NewsPin> selectedNewsPins = new Array<>();

    private List<Marker> markers;
    private SpriteBatch batch;
    private TextureRegion pinRegion;
    private TextureRegion ppjPinRegion;

    private static final float PIN_BASE_W = 14f;
    private static final float PIN_BASE_H = 18f;

    private Table categoryChipsTable;
    private ScrollPane categoryChipsScroll;
    private String activeChipCategory = "Splosno";
    private Label regionTitleLabel;

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
            newsContentTable.top();

            float cardWidth = panelWidth - 24;
            BitmapFont uiFont = assetManager.get(AssetDescriptors.UI_FONT);

            if (selectedRegion != null && regionTitleLabel != null) {
                String regionName = getRegionDisplayName(selectedRegion.id);
                regionTitleLabel.setText(regionName);
                regionTitleLabel.setVisible(true);
            } else if (regionTitleLabel != null) {
                regionTitleLabel.setVisible(false);
            }

            Array<NewsItem> sourceNews = (selectedRegion != null && displayedNews != null && displayedNews.size > 0)
                ? displayedNews
                : newsItems;

            Array<NewsItem> filteredNews = new Array<>();
            for (NewsItem item : sourceNews) {
                if (item == null) continue;

                if (selectedCategory.equals("Splosno") ||
                    (item.getCategory() != null && item.getCategory().getName() != null &&
                        item.getCategory().getName().equals(selectedCategory))) {
                    filteredNews.add(item);
                }
            }

            if (filteredNews.size == 0) {
                BitmapFont descFont = new BitmapFont(uiFont.getData(), uiFont.getRegions(), false);
                descFont.getData().setScale(0.82f);
                Label.LabelStyle emptyStyle = new Label.LabelStyle(
                    descFont,
                    new Color(0.75f, 0.75f, 0.75f, 1f)
                );

                Label emptyLabel = new Label(
                    "Ni novic za izbrano kategorijo",
                    emptyStyle
                );
                emptyLabel.setAlignment(Align.left);
                emptyLabel.setWrap(false);

                newsContentTable.add(emptyLabel)
                    .padTop(20)
                    .left();
                newsContentTable.row();
                newsContentTable.add().expandY();

                if (newsScrollPane != null) {
                    newsScrollPane.layout();
                    newsScrollPane.setScrollY(0);
                }
                return;
            }

            Color softWhite = new Color(0.88f, 0.88f, 0.88f, 1f);
            Color mutedWhite = new Color(0.75f, 0.75f, 0.75f, 1f);

            Label.LabelStyle titleStyle = new Label.LabelStyle(uiFont, softWhite);

            BitmapFont descFont = new BitmapFont(uiFont.getData(), uiFont.getRegions(), false);
            descFont.getData().setScale(0.82f);
            Label.LabelStyle descStyle = new Label.LabelStyle(descFont, mutedWhite);

            for (int i = 0; i < filteredNews.size && i < 10; i++) {
                NewsItem item = filteredNews.get(i);
                if (item != null) {
                    addNewsCard(newsContentTable, item, titleStyle, descStyle, cardWidth);
                }
            }

            newsContentTable.row();
            newsContentTable.add().expandY();

            if (newsScrollPane != null) {
                newsScrollPane.layout();
                newsScrollPane.setScrollY(0);
            }
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
        selectedNewsPins.clear();

        if (selectedRegion == null) {
            return;
        }

        if (displayedNews == null || displayedNews.size == 0) {
            System.out.println("DOTS DEBUG: displayedNews is empty for region=" + selectedRegion.id);
            return;
        }

        Array<NewsItem> filteredNews = new Array<>();
        for (NewsItem item : displayedNews) {
            if (item == null) continue;

            if (selectedCategory.equals("Splosno") ||
                (item.getCategory() != null && item.getCategory().getName() != null &&
                    item.getCategory().getName().equals(selectedCategory))) {
                filteredNews.add(item);
            }
        }

        if (filteredNews.size == 0) {
            System.out.println("DOTS DEBUG: No news items after category filter for region=" + selectedRegion.id + ", category=" + selectedCategory);
            return;
        }

        for (NewsItem item : filteredNews) {
            String cacheKey = item.getId() + "_" + selectedRegion.id;

            Vector2 pos = newsDotCache.get(cacheKey);
            if (pos == null) {
                pos = generateDeterministicPointInRegion(selectedRegion, cacheKey);
                newsDotCache.put(cacheKey, pos);
            }

            selectedNewsPins.add(new NewsPin(pos, item));
        }

        System.out.println("DOTS DEBUG: built " + selectedNewsPins.size + " pins for region=" + selectedRegion.id + ", category=" + selectedCategory + ", from " + filteredNews.size + " filtered items");
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

        return new Vector2((minX + maxX) * 0.5f, (minY + maxY) * 0.5f);
    }

    private void drawSelectedRegionNewsPins() {
        if (batch == null) return;
        if (selectedRegion == null || selectedNewsPins.size == 0) return;
        if (pinRegion == null) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float w = PIN_BASE_W;
        float h = PIN_BASE_H;

        for (NewsPin pin : selectedNewsPins) {
            float drawX = pin.position.x - w * 0.5f;
            float drawY = pin.position.y;

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

        setupCategoryChips(uiFont);
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

        BitmapFont regionTitleFont = new BitmapFont(uiFont.getData(), uiFont.getRegions(), false);
        regionTitleFont.getData().setScale(1.1f);
        Label.LabelStyle regionTitleStyle = new Label.LabelStyle(regionTitleFont, new Color(0.95f, 0.95f, 0.95f, 1f));
        regionTitleLabel = new Label("", regionTitleStyle);
        regionTitleLabel.setAlignment(Align.left);
        regionTitleLabel.setVisible(false);

        header.add(regionTitleLabel).left().padRight(8);
        header.add().expandX();
        header.add(exitButton).size(24, 24).right();

        newsContentTable = new Table();
        newsContentTable.top().left();
        newsContentTable.pad(12);
        newsContentTable.defaults().expandX().fillX();

        Table contentWrapper = new Table();
        contentWrapper.top().left();
        contentWrapper.add(newsContentTable).expand().fill().top().left();
        contentWrapper.row();
        contentWrapper.add().expandY();

        ScrollPane.ScrollPaneStyle scrollPaneStyle = createNewsScrollPaneStyle();
        newsScrollPane = new ScrollPane(contentWrapper, scrollPaneStyle);
        newsScrollPane.setFadeScrollBars(false);
        newsScrollPane.setScrollingDisabled(true, false);
        newsScrollPane.setOverscroll(false, false);
        newsScrollPane.setClamp(true);
        newsScrollPane.setScrollBarPositions(false, true);
        newsScrollPane.setScrollY(0);

        Table panelTable = new Table();
        panelTable.setBackground(createPanelBackground());
        panelTable.setFillParent(false);

        panelTable.add(header)
            .expandX()
            .fillX()
            .height(48);
        panelTable.row();

        panelTable.add(newsScrollPane)
            .expand()
            .fill();

        sidePanel = new Container<>(panelTable);
        sidePanel.setSize(panelWidth, panelHeight);
        sidePanel.setPosition(Gdx.graphics.getWidth(), 0);
        sidePanel.setTransform(true);

        sidePanel.fill();

        if (newsContentTable != null && assetManager != null) {
            BitmapFont loadingFont = assetManager.get(AssetDescriptors.UI_FONT);
            BitmapFont descFont = new BitmapFont(loadingFont.getData(), loadingFont.getRegions(), false);
            descFont.getData().setScale(0.82f);
            Label.LabelStyle descStyle = new Label.LabelStyle(descFont, new Color(0.75f, 0.75f, 0.75f, 1f));

            Label loadingLabel = new Label("Nalaganje novic...", descStyle);
            loadingLabel.setAlignment(Align.left);
            newsContentTable.add(loadingLabel).padTop(20).left();
            newsContentTable.row();
            newsContentTable.add().expandY();
        }
    }

    private void addNewsCard(Table container, NewsItem item, Label.LabelStyle titleStyle, Label.LabelStyle descStyle, float cardWidth) {
        Table cardWrapper = new Table();
        cardWrapper.left();

        Table accentLine = new Table();
        accentLine.setBackground(createAccentLineBackground());
        cardWrapper.add(accentLine).width(3).minHeight(48).fillY();

        Table card = new Table();
        card.pad(8, 12, 8, 12);
        card.setBackground(createCardBackground());

        String rawTitle = item.getTitle();
        String title = limitTitle(rawTitle, 100);

        Label titleLabel = new Label(title, titleStyle);
        titleLabel.setWrap(true);
        titleLabel.setEllipsis(true);
        titleLabel.setFontScale(0.80f);
        card.add(titleLabel).width(cardWidth - 40).left().top();
        card.row().padTop(6);

        String rawText =
            item.getSummary() != null && !item.getSummary().isEmpty()
                ? item.getSummary()
                : item.getContent();

        String description = firstSentenceOrLimit(rawText, 80);

        Label descLabel = new Label(description, descStyle);
        descLabel.setWrap(true);
        descLabel.setFontScale(0.50f);
        card.add(descLabel).width(cardWidth - 40).left().top();

        cardWrapper.add(card).expand().fill();

        com.badlogic.gdx.scenes.scene2d.utils.Drawable normalBg = createCardBackground();
        com.badlogic.gdx.scenes.scene2d.utils.Drawable hoverBg = createCardHoverBackground();

        Container<Table> cardContainer = new Container<>(cardWrapper);
        cardContainer.fill();

        cardContainer.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openNewsDetailScreen(item);
            }
        });

        cardContainer.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                card.clearActions();
                card.setBackground(hoverBg);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                card.clearActions();
                card.setBackground(normalBg);
            }
        });

        container.add(cardContainer).width(cardWidth).padBottom(8).left();
        container.row();
    }

    private String limitTitle(String title, int maxChars) {
        if (title == null) return "Brez naslova";

        title = title.trim();

        if (title.length() <= maxChars) {
            return title;
        }

        return title.substring(0, maxChars).trim() + "...";
    }

    private String firstSentenceOrLimit(String text, int maxChars) {
        if (text == null) return "";

        text = text.trim();

        int dotIndex = text.indexOf(".");
        if (dotIndex > 0 && dotIndex < maxChars) {
            return text.substring(0, dotIndex + 1);
        }

        if (text.length() > maxChars) {
            return text.substring(0, maxChars).trim() + "...";
        }

        return text;
    }

    private String getRegionDisplayName(String regionId) {
        if (regionId == null) return "";
        switch (regionId) {
            case "1": return "Pomurska";
            case "2": return "Podravska";
            case "3": return "Koroška";
            case "4": return "Savinjska";
            case "5": return "Zasavska";
            case "6": return "Posavska";
            case "7": return "Jugovzhodna";
            case "8": return "Osrednjeslovenska";
            case "9": return "Gorenjska";
            case "10": return "Primorsko-notranjska";
            case "11": return "Goriška";
            case "12": return "Obalno-kraška";
            default: return regionId;
        }
    }

    private void openNewsDetailScreen(NewsItem item) {
        com.badlogic.gdx.Game game =
            (com.badlogic.gdx.Game) Gdx.app.getApplicationListener();

        NewsDetailScreen detailScreen =
            new NewsDetailScreen(assetManager, item, this);

        game.setScreen(detailScreen);
    }


    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createPanelBackground() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.12f, 0.12f, 0.13f, 1f));
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

        pixmap.setColor(new Color(0.16f, 0.16f, 0.17f, 1f));
        pixmap.fill();

        pixmap.setColor(new Color(0.25f, 0.25f, 0.26f, 1f));
        pixmap.fillRectangle(0, 0, size, 1);
        pixmap.fillRectangle(0, 0, 1, size);
        pixmap.setColor(new Color(0.25f, 0.25f, 0.26f, 1f));

        pixmap.fillRectangle(size - 1, 0, 1, size);
        pixmap.setColor(new Color(0.25f, 0.25f, 0.26f, 1f));

        pixmap.fillRectangle(0, size - 1, size, 1);

        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        texture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)
        );
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createCardHoverBackground() {
        int size = 100;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        pixmap.setColor(new Color(0.20f, 0.20f, 0.21f, 1f));
        pixmap.fill();

        pixmap.setColor(new Color(0.28f, 0.28f, 0.29f, 1f));
        pixmap.fillRectangle(0, 0, size, 1);
        pixmap.fillRectangle(0, 0, 1, size);
        pixmap.setColor(new Color(0.28f, 0.28f, 0.29f, 1f));

        pixmap.fillRectangle(size - 1, 0, 1, size);
        pixmap.setColor(new Color(0.28f, 0.28f, 0.29f, 1f));

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
    private Container<Label> createCategoryChip(String category, BitmapFont font, TextureAtlas atlas) {

        Label label = new Label(category, new Label.LabelStyle(
            font, new Color(0.88f, 0.88f, 0.88f, 1f)
        ));
        label.setAlignment(Align.center);


        Container<Label> chip = new Container<>(label);
        chip.setTransform(true);
        chip.pad(8, 16, 8, 16);
        chip.minHeight(45);
        chip.maxHeight(45);
        chip.minWidth(100);
        chip.maxWidth(200);

        boolean active = category.equals(activeChipCategory);

        com.badlogic.gdx.scenes.scene2d.utils.Drawable normalBg = active
            ? createActiveCategoryChipDrawable(atlas)
            : createCategoryChipDrawable(atlas);
        com.badlogic.gdx.scenes.scene2d.utils.Drawable hoverBg = active
            ? createActiveCategoryChipHoverDrawable(atlas)
            : createCategoryChipHoverDrawable(atlas);

        chip.setBackground(normalBg);

        chip.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                activeChipCategory = category;
                selectedCategory = category;
                fetchNewsItems(category);
                rebuildCategoryChips();
                if (selectedRegion != null) {
                    rebuildNewsDotsForSelectedRegion();
                }
            }
        });

        chip.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                chip.clearActions();
                chip.setBackground(hoverBg);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                chip.clearActions();
                chip.setBackground(normalBg);
            }
        });

        return chip;
    }

    private Drawable createCategoryChipDrawable(TextureAtlas atlas) {
        return new TextureRegionDrawable(
            atlas.findRegion(RegionNames.BTN_CATEGORY)
        ).tint(new Color(1f, 1f, 1f, 0.3f));
    }

    private Drawable createActiveCategoryChipDrawable(TextureAtlas atlas) {
        return new TextureRegionDrawable(
            atlas.findRegion(RegionNames.BTN_CATEGORY)
        ).tint(new Color(1f, 1f, 1f, 0.6f));
    }

    private Drawable createCategoryChipHoverDrawable(TextureAtlas atlas) {
        return new TextureRegionDrawable(
            atlas.findRegion(RegionNames.BTN_CATEGORY)
        ).tint(new Color(1f, 1f, 1f, 0.45f));
    }

    private Drawable createActiveCategoryChipHoverDrawable(TextureAtlas atlas) {
        return new TextureRegionDrawable(
            atlas.findRegion(RegionNames.BTN_CATEGORY)
        ).tint(new Color(1f, 1f, 1f, 0.75f));
    }

    private void setupCategoryChips(BitmapFont font) {

        categoryChipsTable = new Table();
        categoryChipsTable.left();
        categoryChipsTable.pad(10);

        rebuildCategoryChips();

        categoryChipsScroll = new ScrollPane(categoryChipsTable);
        categoryChipsScroll.setScrollingDisabled(false, true);
        categoryChipsScroll.setFadeScrollBars(false);

        categoryChipsWrapper = new Table();
        categoryChipsWrapper.setFillParent(true);
        categoryChipsWrapper.top();
        categoryChipsWrapper.padTop(20);
        categoryChipsWrapper.padRight(100);
        categoryChipsWrapper.add(categoryChipsScroll)
            .height(48)
            .expandX()
            .fillX()
            .padLeft(16)
            .padRight(16);

        uiStage.addActor(categoryChipsWrapper);

    }

    private void rebuildCategoryChips() {
        categoryChipsTable.clear();

        String[] categories = {
            "Splošno", "Biznis", "Politika",
            "Kultura", "Lifestyle", "Gospodarstvo",
            "Tehnologija", "Vreme"
        };

        BitmapFont font = assetManager.get(AssetDescriptors.UI_FONT);
        TextureAtlas atlas = assetManager.get(AssetDescriptors.UI_ATLAS);


        for (String cat : categories) {
            categoryChipsTable.add(
                createCategoryChip(cat, font, atlas)
            ).padRight(8);
        }
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


        adjustCategoryChipsForPanel(isPanelOpen);

        adjustCameraForPanel(isPanelOpen);

    }
    private void adjustCategoryChipsForPanel(boolean open) {
        if (categoryChipsWrapper == null) return;

        categoryChipsWrapper.clearActions();

        float targetRightPad = open ? panelWidth + 24f : 100f;
        float targetHeight   = open ? 40f : 48f;

        categoryChipsWrapper.addAction(
            Actions.run(() -> {
                categoryChipsWrapper.padRight(targetRightPad);
                categoryChipsScroll.setHeight(targetHeight);
                categoryChipsWrapper.invalidateHierarchy();
            })
        );
    }

    public void resetSidePanelUI() {
        isPanelOpen = false;
        isPanelPinned = false;

        if (sidePanel != null) {
            sidePanel.clearActions();
            sidePanel.setPosition(Gdx.graphics.getWidth(), 0);
        }

        adjustCategoryChipsForPanel(false);

        if (bottomRightTable != null) {
            bottomRightTable.clearActions();
            bottomRightTable.setPosition(zoomButtonsBaseX, bottomRightTable.getY());
        }

        camera.zoom = DEFAULT_ZOOM;
        camera.update();
        clampCameraToMap();

        isRegionZoomed = false;
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

    private Vector2 calculateRegionCentroid(Region region) {
        if (region == null || region.vertices == null || region.vertices.length < 4) {
            return new Vector2(0, 0);
        }

        float sumX = 0f;
        float sumY = 0f;
        int vertexCount = region.vertices.length / 2;

        for (int i = 0; i < region.vertices.length; i += 2) {
            sumX += region.vertices[i];
            sumY += region.vertices[i + 1];
        }

        return new Vector2(sumX / vertexCount, sumY / vertexCount);
    }

    private void smoothZoomToRegion(Region region) {
        if (region == null) return;

        Vector2 centroid = calculateRegionCentroid(region);

        startPosition.set(camera.position.x, camera.position.y);
        startZoom = camera.zoom;

        if (isRegionZoomed) {
            isTwoPhaseAnimation = true;

            intermediateZoom = DEFAULT_ZOOM;
            intermediatePosition.set((mapMinX + mapMaxX) * 0.5f, (mapMinY + mapMaxY) * 0.5f);

            targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, DEFAULT_ZOOM - REGION_ZOOM_IN_AMOUNT));
            targetPosition.set(centroid.x, centroid.y);

            phase1Progress = 0f;
            phase2Progress = 0f;
        } else {
            isTwoPhaseAnimation = false;
            targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, camera.zoom - REGION_ZOOM_IN_AMOUNT));
            targetPosition.set(centroid.x, centroid.y);
        }

        isAnimatingCamera = true;
        animationProgress = 0f;
    }

    private void smoothZoomOutToDefault() {
        startPosition.set(camera.position.x, camera.position.y);
        startZoom = camera.zoom;

        isTwoPhaseAnimation = false;
        targetZoom = DEFAULT_ZOOM;
        targetPosition.set((mapMinX + mapMaxX) * 0.5f, (mapMinY + mapMaxY) * 0.5f);

        isAnimatingCamera = true;
        animationProgress = 0f;
        isRegionZoomed = false;
    }

    private void updateCameraAnimation(float delta) {
        if (!isAnimatingCamera) return;

        if (isTwoPhaseAnimation) {
            if (phase1Progress < 1f) {
                phase1Progress += delta / ZOOM_ANIMATION_DURATION;
                if (phase1Progress >= 1f) {
                    phase1Progress = 1f;
                }

                float t1 = phase1Progress;
                float t1Squared = t1 * t1;
                float t1Cubed = t1Squared * t1;
                t1 = t1 < 0.5f ? 4f * t1Cubed : 1f - (float)Math.pow(-2f * t1 + 2f, 3f) / 2f;

                float currentX = startPosition.x + (intermediatePosition.x - startPosition.x) * t1;
                float currentY = startPosition.y + (intermediatePosition.y - startPosition.y) * t1;
                camera.position.set(currentX, currentY, 0);
                camera.zoom = startZoom + (intermediateZoom - startZoom) * t1;
            }
            else if (phase2Progress < 1f) {
                phase2Progress += delta / ZOOM_ANIMATION_DURATION;
                if (phase2Progress >= 1f) {
                    phase2Progress = 1f;
                    isAnimatingCamera = false;
                    isRegionZoomed = true;
                }

                float t2 = phase2Progress;
                float t2Squared = t2 * t2;
                float t2Cubed = t2Squared * t2;
                t2 = t2 < 0.5f ? 4f * t2Cubed : 1f - (float)Math.pow(-2f * t2 + 2f, 3f) / 2f;

                float currentX = intermediatePosition.x + (targetPosition.x - intermediatePosition.x) * t2;
                float currentY = intermediatePosition.y + (targetPosition.y - intermediatePosition.y) * t2;
                camera.position.set(currentX, currentY, 0);
                camera.zoom = intermediateZoom + (targetZoom - intermediateZoom) * t2;
            }
        } else {
            animationProgress += delta / ZOOM_ANIMATION_DURATION;

            if (animationProgress >= 1f) {
                animationProgress = 1f;
                isAnimatingCamera = false;
                isRegionZoomed = true;
            }

            float t = animationProgress;
            float tSquared = t * t;
            float tCubed = tSquared * t;
            t = t < 0.5f ? 4f * tCubed : 1f - (float)Math.pow(-2f * t + 2f, 3f) / 2f;

            float currentX = startPosition.x + (targetPosition.x - startPosition.x) * t;
            float currentY = startPosition.y + (targetPosition.y - startPosition.y) * t;
            camera.position.set(currentX, currentY, 0);

            camera.zoom = startZoom + (targetZoom - startZoom) * t;
        }

        camera.update();
        clampCameraToMap();
    }


    @Override
    public void render(float delta) {
        timeSinceLastFetch += delta;
        if (timeSinceLastFetch >= FETCH_INTERVAL) {
            timeSinceLastFetch = 0;
            fetchNewsMarkers();
            fetchNewsItems(selectedCategory);
        }

        Gdx.gl.glClearColor(0.118f, 0.118f, 0.133f, 1f);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        uiStage.act(delta);

        updateCameraAnimation(delta);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        Vector3 mousePos = new Vector3(
            Gdx.input.getX(),
            Gdx.input.getY(),
            0
        );
        camera.unproject(mousePos);

        updateHover(mousePos);

        boolean uiHit = false;
        if (Gdx.input.justTouched() && uiStage != null) {
            int touchX = Gdx.input.getX();
            int touchY = Gdx.input.getY();
            float stageY = Gdx.graphics.getHeight() - touchY;
            uiHit = (uiStage.hit(touchX, (int)stageY, true) != null);
        }

        if (Gdx.input.justTouched() && !uiHit) {
            NewsPin clickedPin = findClickedPin(mousePos);
            if (clickedPin != null) {
                openNewsDetailScreen(clickedPin.newsItem);
                return;
            }

            if (hoveredRegion != null) {
                if (hoveredRegion == selectedRegion) {
                    selectedRegion = null;
                    displayedNews.clear();
                    selectedNewsPins.clear();
                    updateNewsCards();

                    smoothZoomOutToDefault();

                    if (isPanelOpen) {
                        toggleSidePanel();
                    }
                } else {
                    selectedRegion = hoveredRegion;
                    System.out.println("Clicked region: " + selectedRegion.id);

                    smoothZoomToRegion(selectedRegion);

                    displayedNews = filterNewsForRegion(selectedRegion);
                    rebuildNewsDotsForSelectedRegion();
                    updateNewsCards();

                    if (!isPanelOpen) {
                        toggleSidePanel();
                    }
                }
            }
        }

        Color regionColor = new Color(0.75f, 0.6f, 0.85f, 1f);
        Color hoverColor  = new Color(0.6f, 0.45f, 0.75f, 1f);

        drawRegions(regionColor, selectedRegion, hoveredRegion);

        if (selectedRegion != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(hoverColor);
            drawRegion(selectedRegion, selectedRegion.vertices);
            shapeRenderer.end();
        }

        if (hoveredRegion != null && hoveredRegion != selectedRegion) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(hoverColor);
            drawRegion(hoveredRegion, hoveredRegion.vertices);
            shapeRenderer.end();
        }

        drawBorders();

        drawSelectedRegionNewsPins();

        drawPPJMarkers();

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

    private void drawRegions(Color color, Region excludeRegion1, Region excludeRegion2) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(color);

        for (Region region : regions) {
            if (region == excludeRegion1 || region == excludeRegion2) {
                continue;
            }
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
            panelHeight = height;
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
        if (batch != null) batch.dispose();
        uiStage.dispose();
    }

    private class MapInputProcessor extends InputAdapter {

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            float stageY = Gdx.graphics.getHeight() - screenY;
            if (uiStage != null && uiStage.hit(screenX, (int)stageY, true) != null) {
                uiConsumedTouch = true;
                return false;
            }

            uiConsumedTouch = false;
            lastScreenX = screenX;
            lastScreenY = screenY;
            isPanning = false;
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (uiConsumedTouch) {
                return false;
            }

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
            boolean wasConsumed = uiConsumedTouch;
            uiConsumedTouch = false;
            isPanning = false;
            return wasConsumed;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            float zoomDelta = amountY * ZOOM_SPEED;
            zoomAtPoint(Gdx.input.getX(), Gdx.input.getY(), zoomDelta);
            return true;
        }
    }

    private static class NewsPin {
        final Vector2 position;
        final NewsItem newsItem;

        NewsPin(Vector2 position, NewsItem newsItem) {
            this.position = position;
            this.newsItem = newsItem;
        }
    }

    private NewsPin findClickedPin(Vector3 worldPos) {
        if (selectedNewsPins.size == 0) return null;

        float clickX = worldPos.x;
        float clickY = worldPos.y;
        float pinHalfWidth = PIN_BASE_W * 0.5f;
        float pinHeight = PIN_BASE_H;
        float clickRadius = Math.max(pinHalfWidth, pinHeight * 0.5f) * 1.5f;

        for (NewsPin pin : selectedNewsPins) {
            float dx = clickX - pin.position.x;
            float dy = clickY - pin.position.y;
            float distanceSq = dx * dx + dy * dy;

            if (distanceSq <= clickRadius * clickRadius) {
                return pin;
            }
        }

        return null;
    }
}
