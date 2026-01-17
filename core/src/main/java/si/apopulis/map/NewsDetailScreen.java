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
    private String editingCommentId = null;
    private TextButton sendBtn;
    private String myOwnerKey;
    private TextureRegion editRegion;
    private TextureRegion delRegion;
    private float iconBtnSize = 26f;

    public NewsDetailScreen(AssetManager assetManager, NewsItem newsItem, Screen previousScreen) {
        this.assetManager = assetManager;
        this.newsItem = newsItem;
        this.previousScreen = previousScreen;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        myOwnerKey = NewsApiClient.getLocalOwnerKey();

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

        TextureAtlas atlas = assetManager.get(AssetDescriptors.UI_ATLAS);

        editRegion = atlas.findRegion(RegionNames.BTN_EDIT);
        delRegion  = atlas.findRegion(RegionNames.BTN_DELETE);

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

        BitmapFont metaFont = new BitmapFont(Gdx.files.internal("fonts/font.fnt"));
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


        BitmapFont bodyFont = new BitmapFont(Gdx.files.internal("fonts/font.fnt"));
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
        
        Pixmap bgPixmap = new Pixmap(100, 100, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(new Color(0.18f, 0.18f, 0.19f, 1f));
        bgPixmap.fill();
        Texture bgTexture = new Texture(bgPixmap);
        bgTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        bgPixmap.dispose();
        
        com.badlogic.gdx.graphics.g2d.NinePatch ninePatch = new com.badlogic.gdx.graphics.g2d.NinePatch(bgTexture, 8, 8, 8, 8);
        com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable npDrawable = new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(ninePatch);
        tfStyle.background = npDrawable;
        
        tfStyle.cursor = bg(Color.WHITE);
        tfStyle.selection = bg(new Color(0.3f, 0.3f, 0.35f, 1f));

        commentInput = new TextField("", tfStyle);
        commentInput.setMessageText("Napiši komentar...");
        commentInput.setMaxLength(200);
        commentInput.setAlignment(Align.left);

        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = metaFont;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.up = bg(new Color(0.55f, 0.35f, 0.80f, 1f));
        btnStyle.down = bg(new Color(0.45f, 0.28f, 0.70f, 1f));

        sendBtn = new TextButton("Pošalji", btnStyle);

        sendBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String txt = commentInput.getText();
                if (txt == null || txt.trim().isEmpty()) {
                    commentsStatusLabel.setText("Komentar ne sme biti prazan.");
                    return;
                }

                // Edit mode
                if (editingCommentId != null) {
                    commentsStatusLabel.setText("Shranjujem...");

                    NewsApiClient.updateMyComment(editingCommentId, txt.trim(), new NewsApiClient.UpdateCommentCallback() {
                        @Override
                        public void onSuccess(CommentItem updated) {
                            editingCommentId = null;
                            commentInput.setText("");
                            sendBtn.setText("Pošalji");
                            loadComments();
                        }

                        @Override
                        public void onFailure(Throwable error) {
                            commentsStatusLabel.setText("Napaka pri shranjevanju.");
                            Gdx.app.error("COMMENTS", "Update failed", error);
                        }
                    });

                    return;
                }

                // Create/Normal mode
                commentsStatusLabel.setText("Pošiljam...");

                NewsApiClient.createGuestComment(newsItem.getId(), txt.trim(), new NewsApiClient.CreateCommentCallback() {
                    @Override
                    public void onSuccess(CommentItem created) {
                        commentInput.setText("");
                        loadComments();
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

        BitmapFont cFont = new BitmapFont(Gdx.files.internal("fonts/font.fnt"));
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

            Table topRow = new Table();
            topRow.left();
            topRow.add(userLbl).left().expandX();

            boolean isMine = c.isSimulated()
                && c.getOwnerKey() != null
                && myOwnerKey != null
                && c.getOwnerKey().equals(myOwnerKey);

            if (isMine) {
                TextButton.TextButtonStyle smallBtn = new TextButton.TextButtonStyle();
                smallBtn.font = cFont;
                smallBtn.fontColor = Color.WHITE;
                smallBtn.up = bg(new Color(0.30f, 0.30f, 0.32f, 1f));
                smallBtn.down = bg(new Color(0.22f, 0.22f, 0.24f, 1f));

                ImageButton.ImageButtonStyle editStyle = new ImageButton.ImageButtonStyle();
                editStyle.imageUp = new TextureRegionDrawable(editRegion);
                editStyle.imageDown = new TextureRegionDrawable(editRegion);
                ImageButton editBtn = new ImageButton(editStyle);

                ImageButton.ImageButtonStyle delStyle = new ImageButton.ImageButtonStyle();
                delStyle.imageUp = new TextureRegionDrawable(delRegion);
                delStyle.imageDown = new TextureRegionDrawable(delRegion);
                ImageButton delBtn = new ImageButton(delStyle);

                editBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        editingCommentId = c.getId();
                        commentInput.setText(c.getContent() == null ? "" : c.getContent());
                        sendBtn.setText("Sačuvaj");
                        commentsStatusLabel.setText("Uređivanje komentara...");
                        stage.setKeyboardFocus(commentInput);
                        scrollPane.setScrollY(scrollPane.getMaxY());
                    }
                });

                delBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        showDeleteConfirmDialog(c.getId());
                    }
                });

                topRow.add(editBtn).size(iconBtnSize).padLeft(10);
                topRow.add(delBtn).size(iconBtnSize).padLeft(8);
            }

            one.add(topRow).width(commentsMaxWidth).left().padBottom(4);
            one.row();
            one.add(textLbl).width(commentsMaxWidth).left().padBottom(12);

            commentsTable.add(one).left().padBottom(8);
            commentsTable.row();
        }
    }

    private void showDeleteConfirmDialog(String commentId) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        overlay.setBackground(bg(new Color(0f, 0f, 0f, 0.55f)));

        // Card
        Table card = new Table();
        card.setBackground(bg(new Color(0.16f, 0.16f, 0.18f, 1f)));
        card.pad(18);

        BitmapFont font = new BitmapFont(Gdx.files.internal("fonts/font.fnt"));
        font.getData().setScale(0.62f);

        Label title = new Label("Ste prepričani?", new Label.LabelStyle(font, Color.WHITE));
        Label msg = new Label("Ali želite izbrisati komentar?", new Label.LabelStyle(font, new Color(0.85f, 0.85f, 0.85f, 1f)));
        msg.setWrap(true);

        // Buttons
        TextButton.TextButtonStyle cancelStyle = new TextButton.TextButtonStyle();
        cancelStyle.font = font;
        cancelStyle.fontColor = Color.WHITE;
        cancelStyle.up = bg(new Color(0.30f, 0.30f, 0.32f, 1f));
        cancelStyle.down = bg(new Color(0.22f, 0.22f, 0.24f, 1f));

        TextButton.TextButtonStyle deleteStyle = new TextButton.TextButtonStyle();
        deleteStyle.font = font;
        deleteStyle.fontColor = Color.WHITE;
        deleteStyle.up = bg(new Color(0.70f, 0.22f, 0.22f, 1f));
        deleteStyle.down = bg(new Color(0.58f, 0.18f, 0.18f, 1f));

        TextButton cancelBtn = new TextButton("Prekliči", cancelStyle);
        TextButton deleteBtn = new TextButton("Izbriši", deleteStyle);

        // Layout carda
        card.add(title).left().padBottom(10);
        card.row();
        card.add(msg).width(Math.min(520, commentsMaxWidth)).left().padBottom(16);
        card.row();

        Table btnRow = new Table();
        float btnW = 100f;
        btnRow.add(cancelBtn).width(btnW).height(38).padRight(10);
        btnRow.add(deleteBtn).width(btnW).height(38);

        card.add(btnRow).right();
        overlay.add(card).center();

        cancelBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                overlay.remove();
            }
        });

        deleteBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                overlay.remove();
                commentsStatusLabel.setText("Brišem...");

                NewsApiClient.deleteMyComment(commentId, new NewsApiClient.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (editingCommentId != null && editingCommentId.equals(commentId)) {
                            editingCommentId = null;
                            commentInput.setText("");
                            sendBtn.setText("Pošalji");
                        }
                        loadComments();
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        commentsStatusLabel.setText("Napaka pri brisanju.");
                        Gdx.app.error("COMMENTS", "Delete failed", error);
                    }
                });
            }
        });

        stage.addActor(overlay);
        stage.setKeyboardFocus(null);
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
