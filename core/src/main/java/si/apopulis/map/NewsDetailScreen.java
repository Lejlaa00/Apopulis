package si.apopulis.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewsDetailScreen implements Screen {

    private final AssetManager assetManager;
    private final NewsItem newsItem;
    private final Screen previousScreen;

    private Stage stage;

    public NewsDetailScreen(AssetManager assetManager, NewsItem newsItem, Screen previousScreen) {
        this.assetManager = assetManager;
        this.newsItem = newsItem;
        this.previousScreen = previousScreen;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        BitmapFont uiFont = assetManager.get(AssetDescriptors.UI_FONT);
        TextureAtlas uiAtlas = assetManager.get(AssetDescriptors.UI_ATLAS);

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(bg(new Color(0.10f, 0.10f, 0.11f, 1f)));
        stage.addActor(root);

        Table header = new Table();
        header.setBackground(bg(new Color(0.12f, 0.12f, 0.13f, 1f)));
        header.pad(16);

        ImageButton back = new ImageButton(
            new TextureRegionDrawable(uiAtlas.findRegion(RegionNames.BTN_X))
        );
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

                if (previousScreen instanceof MapScreen) {
                    ((MapScreen) previousScreen).resetSidePanelUI();
                }

                ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener())
                    .setScreen(previousScreen);
            }
        });


        header.add(back)
            .size(20, 20)
            .left();

        header.add().expandX();

        root.add(header).expandX().fillX().padBottom(16);
        root.row();

        // CONTENT CARD
        float maxWidth = Math.min(900, Gdx.graphics.getWidth() - 64);
        float paddingLeft = 20f;
        float paddingRight = 48f;
        float paddingTop = 32f;
        float paddingBottom = 32f;

        Table content = new Table();
        content.top().left();
        content.padLeft(paddingLeft);
        content.padRight(paddingRight);
        content.padTop(paddingTop);
        content.padBottom(paddingBottom);

        BitmapFont titleFont = new BitmapFont(uiFont.getData(), uiFont.getRegions(), false);
        titleFont.getData().setScale(1.35f);
        Label title = new Label(
            safe(newsItem.getTitle(), "Brez naslova"),
            new Label.LabelStyle(titleFont, new Color(0.95f, 0.95f, 0.95f, 1f))
        );
        title.setWrap(true);
        content.add(title).width(maxWidth).left().padBottom(20);
        content.row();

        if (newsItem.getImageUrl() != null && !newsItem.getImageUrl().isEmpty()) {
            addHeroImage(content, maxWidth, newsItem.getImageUrl());
        }

        BitmapFont metaFont = new BitmapFont(Gdx.files.internal("fonts/arial-32.fnt"));
        metaFont.getData().setScale(0.55f);

        Color metaColor = new Color(0.6f, 0.6f, 0.6f, 1f);
        Label.LabelStyle metaStyle = new Label.LabelStyle(metaFont, metaColor);

        Table meta = new Table();
        meta.left();

        if (newsItem.getCategory() != null) {
            meta.add(new Label("Kategorija: " + newsItem.getCategory().getName(), metaStyle))
                .left()
                .padBottom(4);
            meta.row();
        }

        if (newsItem.getPublishedAt() != null) {
            meta.add(new Label("Objavljeno: " + formatDate(newsItem.getPublishedAt()), metaStyle))
                .left()
                .padBottom(4);
            meta.row();
        }

        if (newsItem.getAuthor() != null && !newsItem.getAuthor().isEmpty()) {
            meta.add(new Label("Avtor: " + newsItem.getAuthor(), metaStyle))
                .left()
                .padBottom(4);
            meta.row();
        }

        if (newsItem.getSource() != null) {
            meta.add(new Label("Vir: " + newsItem.getSource().getName(), metaStyle))
                .left();
        }

        content.add(meta).left().padBottom(20);
        content.row();

        Table divider = new Table();
        divider.setBackground(bg(new Color(0.25f, 0.25f, 0.26f, 1f)));

        content.add(divider)
            .width(maxWidth)
            .height(1)
            .padBottom(24);
        content.row();


        BitmapFont bodyFont = new BitmapFont(Gdx.files.internal("fonts/arial-32.fnt"));
        bodyFont.getData().setScale(0.65f);

        Label body = new Label(
            getBodyText(),
            new Label.LabelStyle(bodyFont, new Color(0.82f, 0.82f, 0.82f, 1f))
        );

        body.setWrap(true);
        body.setAlignment(Align.left);

        content.add(body)
            .width(maxWidth)
            .left();

        content.row();

        content.add().expandY().minHeight(40);

        Table contentWrapper = new Table();
        contentWrapper.top().left();
        float wrapperWidth = maxWidth + paddingLeft + paddingRight;

        contentWrapper.add(content)
            .width(wrapperWidth)
            .top()
            .left();
        contentWrapper.row();
        contentWrapper.add().expandY();

        ScrollPane scroll = new ScrollPane(contentWrapper);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        scroll.setClamp(true);
        scroll.setFillParent(false);

        Table card = new Table();
        card.setBackground(bg(new Color(0.15f, 0.15f, 0.16f, 1f)));
        card.add(scroll).expand().fill();
        card.pad(8);

        root.add(card)
            .expand()
            .fill()
            .padBottom(24)
            .padLeft(100)
            .padRight(100);
    }

    private String getBodyText() {
        if (newsItem.getContent() != null && !newsItem.getContent().isEmpty())
            return newsItem.getContent();
        if (newsItem.getSummary() != null)
            return newsItem.getSummary();
        return "";
    }

    private String formatDate(String raw) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(raw);
            return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(d);
        } catch (ParseException e) {
            return raw;
        }
    }

    private String safe(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private void addHeroImage(Table content, float maxWidth, String imageUrl) {

        Pixmap ph = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        ph.setColor(new Color(0.2f, 0.2f, 0.2f, 1f));
        ph.fill();
        Texture placeholder = new Texture(ph);
        ph.dispose();

        Image hero = new Image(placeholder);
        hero.setScaling(Scaling.fit);

        content.add(hero)
            .width(maxWidth)
            .height(260)
            .padBottom(24);
        content.row();

        HttpRequestBuilder builder = new HttpRequestBuilder();
        Net.HttpRequest request = builder.newRequest()
            .method(Net.HttpMethods.GET)
            .url(imageUrl)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {

            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                byte[] bytes = httpResponse.getResult();

                Gdx.app.postRunnable(() -> {
                    try {
                        Pixmap pixmap = new Pixmap(bytes, 0, bytes.length);
                        Texture tex = new Texture(pixmap);
                        pixmap.dispose();

                        hero.setDrawable(
                            new TextureRegionDrawable(new TextureRegion(tex))
                        );
                    } catch (Exception e) {
                        Gdx.app.error("IMAGE", "Failed to decode image", e);
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.error("IMAGE", "Failed to load image", t);
            }

            @Override
            public void cancelled() {}
        });
    }


    private TextureRegionDrawable bg(Color c) {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return new TextureRegionDrawable(new TextureRegion(t));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.10f, 0.10f, 0.11f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
