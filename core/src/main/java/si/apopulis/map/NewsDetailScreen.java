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
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import si.apopulis.map.assets.AssetDescriptors;
import si.apopulis.map.assets.RegionNames;
import si.apopulis.map.model.CommentItem;
import si.apopulis.map.model.NewsItem;

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
    private Table commentsTable;
    private Label commentsStatusLabel;
    private TextField commentInput;
    private float commentsMaxWidth;
    private Stage stage;
    private ScrollPane scrollPane;

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
        root.setColor(1f, 1f, 1f, 0f);
        stage.addActor(root);
        root.addAction(Actions.fadeIn(0.4f));

        Table header = new Table();
        header.setBackground(bg(new Color(0.12f, 0.12f, 0.13f, 1f)));
        header.pad(16);

        ImageButton back = new ImageButton(
            new TextureRegionDrawable(uiAtlas.findRegion(RegionNames.BTN_X))
        );
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {

                if (newsItem.getId() == null || newsItem.getId().isEmpty()) {
                    commentsStatusLabel.setText("Manjka ID novice.");
                    return;
                }

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
        commentsMaxWidth = maxWidth;
        float paddingLeft = 24f;
        float paddingRight = 24f;
        float paddingTop = 20f;
        float paddingBottom = 20f;

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

        content.add(body).width(maxWidth).left();
        content.row();

        // Comments
        Label commentsTitle = new Label("Komentari", new Label.LabelStyle(uiFont, Color.WHITE));
        content.add(commentsTitle).width(maxWidth).left().padTop(24).padBottom(10);
        content.row();

        Table dividerBeforeComments = new Table();
        dividerBeforeComments.setBackground(bg(new Color(0.25f, 0.25f, 0.26f, 1f)));
        content.add(dividerBeforeComments).width(maxWidth).height(1).padTop(18).padBottom(16);
        content.row();

        // Status
        commentsStatusLabel = new Label("Učitavam...", new Label.LabelStyle(metaFont, metaColor));
        content.add(commentsStatusLabel).width(maxWidth).left().padBottom(8);
        content.row();

        // Comments list
        commentsTable = new Table();
        commentsTable.left().top();
        content.add(commentsTable).width(maxWidth).left();
        content.row();

        // Input and send
        TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle();
        tfStyle.font = metaFont;
        tfStyle.fontColor = new Color(0.9f, 0.9f, 0.9f, 1f);
        tfStyle.background = bg(new Color(0.18f, 0.18f, 0.19f, 1f));
        tfStyle.cursor = bg(Color.WHITE);
        tfStyle.selection = bg(new Color(0.3f, 0.3f, 0.35f, 1f));

        commentInput = new TextField("", tfStyle);
        commentInput.setMessageText("  Napiši komentar...");
        commentInput.setMaxLength(200);
        commentInput.setAlignment(Align.left);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = metaFont;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.up = bg(new Color(0.55f, 0.35f, 0.80f, 1f));
        btnStyle.down = bg(new Color(0.45f, 0.28f, 0.70f, 1f));

        TextButton sendBtn = new TextButton("Pošalji", btnStyle);

        sendBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String txt = commentInput.getText();
                if (txt == null || txt.trim().isEmpty()) {
                    commentsStatusLabel.setText("Komentar ne sme biti prazan.");
                    return;
                }

                commentsStatusLabel.setText("Pošiljam...");

                NewsApiClient.createGuestComment(newsItem.getId(), txt.trim(), new NewsApiClient.CreateCommentCallback() {
                    @Override
                    public void onSuccess(CommentItem created) {
                        commentInput.setText("");
                        loadComments(); // refresh
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        commentsStatusLabel.setText("Napaka pri pošiljanju komentarja.");
                        Gdx.app.error("COMMENTS", "Create failed", error);
                    }
                });
            }
        });

        Table inputRow = new Table();
        inputRow.add(commentInput).width(maxWidth - 140).height(42).padRight(10);
        inputRow.add(sendBtn).width(130).height(42);

        content.add(inputRow).width(maxWidth).left().padTop(14);
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

        scrollPane = new ScrollPane(contentWrapper);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setClamp(true);
        scrollPane.setFillParent(false);

        Table card = new Table();
        card.setBackground(bg(new Color(0.15f, 0.15f, 0.16f, 1f)));
        card.add(scrollPane).expand().fill();
        card.pad(8);
        card.setColor(1f, 1f, 1f, 0f);
        card.setScale(0.96f);

        root.add(card)
            .expand()
            .fill()
            .padBottom(24)
            .padLeft(130)
            .padRight(100);

        card.addAction(Actions.parallel(
            Actions.fadeIn(0.45f),
            Actions.scaleTo(1f, 1f, 0.45f)
        ));

        loadComments();
        stage.setScrollFocus(scrollPane);
        stage.setKeyboardFocus(scrollPane);
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
            public void cancelled() {
            }
        });
    }

    private void loadComments() {

        if (newsItem.getId() == null || newsItem.getId().isEmpty()) {
            commentsStatusLabel.setText("Manjka ID novice.");
            return;
        }
        commentsStatusLabel.setText("Učitavam...");

        NewsApiClient.fetchCommentsForNews(newsItem.getId(), new NewsApiClient.CommentsCallback() {
            @Override
            public void onSuccess(Array<CommentItem> comments) {
                renderComments(comments);
            }

            @Override
            public void onFailure(Throwable error) {
                commentsTable.clear();
                commentsStatusLabel.setText("Napaka pri nalaganju komentarjev.");
                Gdx.app.error("COMMENTS", "Load failed", error);
            }
        });
    }

    private void renderComments(Array<CommentItem> comments) {
        commentsTable.clear();

        if (comments == null || comments.size == 0) {
            commentsStatusLabel.setText("Ni komentarjev.");
            return;
        }

        commentsStatusLabel.setText("");

        BitmapFont cFont = new BitmapFont(Gdx.files.internal("fonts/arial-32.fnt"));
        cFont.getData().setScale(0.6f);

        Label.LabelStyle userStyle = new Label.LabelStyle(cFont, new Color(0.55f, 0.35f, 0.80f, 1f));
        Label.LabelStyle textStyle = new Label.LabelStyle(cFont, new Color(0.82f, 0.82f, 0.82f, 1f));

        for (CommentItem c : comments) {
            String user = (c.getUsername() == null || c.getUsername().isEmpty()) ? "Guest" : c.getUsername();

            Label userLbl = new Label(user, userStyle);
            Label textLbl = new Label(c.getContent() == null ? "" : c.getContent(), textStyle);
            textLbl.setWrap(true);

            Table one = new Table();
            one.left().top();
            one.add(userLbl).left().padBottom(4);
            one.row();
            one.add(textLbl).width(commentsMaxWidth).left().padBottom(12);

            commentsTable.add(one).left().padBottom(8);
            commentsTable.row();
        }
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

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
