package si.apopulis.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import si.apopulis.map.assets.AssetDescriptors;
import si.apopulis.map.assets.RegionNames;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewsDetailScreen implements Screen {

    private final AssetManager assetManager;
    private final NewsItem newsItem;
    private final Screen previousScreen;
    private Stage stage;
    private Table rootTable;

    public NewsDetailScreen(AssetManager assetManager, NewsItem newsItem, Screen previousScreen) {
        this.assetManager = assetManager;
        this.newsItem = newsItem;
        this.previousScreen = previousScreen;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        TextureAtlas uiAtlas = assetManager.get(AssetDescriptors.UI_ATLAS);
        BitmapFont uiFont = assetManager.get(AssetDescriptors.UI_FONT);

        // Create root table that fills the screen
        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.setBackground(createScreenBackground());

        // Header with back button
        Table header = createHeader(uiAtlas);
        rootTable.add(header).expandX().fillX().pad(20, 20, 0, 20);
        rootTable.row();

        // Content area (scrollable)
        Table contentTable = createContentTable(uiFont);
        ScrollPane scrollPane = new ScrollPane(contentTable);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        rootTable.add(scrollPane).expand().fill().pad(20);
        rootTable.row();

        stage.addActor(rootTable);
    }

    private Table createHeader(TextureAtlas uiAtlas) {
        Table header = new Table();
        header.setBackground(createHeaderBackground());

        // Back button
        ImageButton backButton = new ImageButton(
            new TextureRegionDrawable(uiAtlas.findRegion(RegionNames.BTN_EXIT))
        );
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(() -> {
                    if (previousScreen != null) {
                        ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(previousScreen);
                    }
                });
            }
        });

        header.add(backButton).size(32, 32).pad(12);
        header.add().expandX();
        header.pack();

        return header;
    }

    private Table createContentTable(BitmapFont uiFont) {
        Table content = new Table();
        content.top().left();
        content.pad(30);

        // Maximum content width for comfortable reading
        float maxContentWidth = Math.min(800, Gdx.graphics.getWidth() - 80);

        // Title
        String title = newsItem.getTitle() != null && !newsItem.getTitle().isEmpty()
            ? newsItem.getTitle()
            : "Brez naslova";
        
        BitmapFont titleFont = new BitmapFont(uiFont.getData(), uiFont.getRegions(), false);
        titleFont.getData().setScale(1.4f);
        Label.LabelStyle titleStyle = new Label.LabelStyle(titleFont, new Color(0.1f, 0.1f, 0.1f, 1f));
        Label titleLabel = new Label(title, titleStyle);
        titleLabel.setWrap(true);
        titleLabel.setAlignment(Align.left);
        content.add(titleLabel).width(maxContentWidth).left().padBottom(24);
        content.row();

        // Metadata row (category, date, source)
        Table metadataTable = createMetadataTable(uiFont, maxContentWidth);
        content.add(metadataTable).width(maxContentWidth).left().padBottom(30);
        content.row();

        // Divider line
        Table divider = new Table();
        divider.setBackground(createDividerDrawable());
        content.add(divider).width(maxContentWidth).height(1).padBottom(30);
        content.row();

        // Content/Summary
        String contentText = getContentText();
        if (contentText != null && !contentText.isEmpty()) {
            BitmapFont contentFont = new BitmapFont(uiFont.getData(), uiFont.getRegions(), false);
            contentFont.getData().setScale(1.0f);
            Label.LabelStyle contentStyle = new Label.LabelStyle(contentFont, new Color(0.2f, 0.2f, 0.2f, 1f));
            Label contentLabel = new Label(contentText, contentStyle);
            contentLabel.setWrap(true);
            contentLabel.setAlignment(Align.left);
            content.add(contentLabel).width(maxContentWidth).left();
            content.row();
        }

        // Add padding at bottom
        content.add().expandY().minHeight(40);

        return content;
    }

    private Table createMetadataTable(BitmapFont font, float maxWidth) {
        Table metadata = new Table();
        metadata.left();

        BitmapFont metaFont = new BitmapFont(font.getData(), font.getRegions(), false);
        metaFont.getData().setScale(0.85f);
        Label.LabelStyle metaStyle = new Label.LabelStyle(metaFont, new Color(0.5f, 0.5f, 0.5f, 1f));

        // Category
        if (newsItem.getCategory() != null && newsItem.getCategory().getName() != null) {
            Label categoryLabel = new Label("Kategorija: " + newsItem.getCategory().getName(), metaStyle);
            metadata.add(categoryLabel).left();
            metadata.add().width(20); // Spacer
        }

        // Date
        if (newsItem.getPublishedAt() != null && !newsItem.getPublishedAt().isEmpty()) {
            String formattedDate = formatDate(newsItem.getPublishedAt());
            Label dateLabel = new Label("Datum: " + formattedDate, metaStyle);
            metadata.add(dateLabel).left();
            metadata.add().width(20); // Spacer
        }

        // Source
        if (newsItem.getSource() != null && newsItem.getSource().getName() != null) {
            Label sourceLabel = new Label("Vir: " + newsItem.getSource().getName(), metaStyle);
            metadata.add(sourceLabel).left();
        }

        // Author (if available)
        if (newsItem.getAuthor() != null && !newsItem.getAuthor().isEmpty()) {
            metadata.row().padTop(8);
            Label authorLabel = new Label("Avtor: " + newsItem.getAuthor(), metaStyle);
            metadata.add(authorLabel).left();
        }

        return metadata;
    }

    private String getContentText() {
        if (newsItem.getContent() != null && !newsItem.getContent().isEmpty()) {
            return newsItem.getContent();
        } else if (newsItem.getSummary() != null && !newsItem.getSummary().isEmpty()) {
            return newsItem.getSummary();
        }
        return null;
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "Neznan datum";
        }

        try {
            // Try ISO 8601 format first
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = inputFormat.parse(dateString);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US);
            return outputFormat.format(date);
        } catch (ParseException e) {
            // If parsing fails, return original string
            return dateString;
        }
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createScreenBackground() {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.98f, 0.98f, 0.98f, 1f));
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)
        );
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createHeaderBackground() {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(1f, 1f, 1f, 1f));
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)
        );
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable createDividerDrawable() {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.85f, 0.85f, 0.85f, 1f));
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture texture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(texture)
        );
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.98f, 0.98f, 0.98f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
    }
}
