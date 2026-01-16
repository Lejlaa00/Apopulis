package si.apopulis.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.Preferences;
import java.util.UUID;

import si.apopulis.map.model.CommentItem;
import si.apopulis.map.model.NewsItem;

public class NewsApiClient {
    private static final String API_BASE_URL = System.getProperty("api.base.url", "http://localhost:5001/api");
    private static final String PROVINCE_NEWS_STATS_ENDPOINT = "/provinces/stats/news";

    private static final String COMMENTS_ENDPOINT = "/comments/news/";
    private static final String COMMENTS_MAP_ENDPOINT = "/comments/map/";
    private static final String NEWS_ENDPOINT = "/news";

    public interface ProvinceNewsCallback {
        void onSuccess(Array<ProvinceNewsMarker> markers);
        void onFailure(Throwable error);
    }

    public interface NewsItemsCallback {
        void onSuccess(Array<NewsItem> newsItems);
        void onFailure(Throwable error);
    }

    public interface CommentsCallback {
        void onSuccess(Array<CommentItem> comments);
        void onFailure(Throwable error);
    }

    public interface CreateCommentCallback {
        void onSuccess(CommentItem created);
        void onFailure(Throwable error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Throwable error);
    }

    public interface UpdateCommentCallback {
        void onSuccess(CommentItem updated);
        void onFailure(Throwable error);
    }

    private static String getOwnerKey() {
        Preferences prefs = Gdx.app.getPreferences("apopulis");
        String key = prefs.getString("ownerKey", null);
        if (key == null || key.isEmpty()) {
            key = UUID.randomUUID().toString();
            prefs.putString("ownerKey", key);
            prefs.flush();
        }
        return key;
    }

    public static String getLocalOwnerKey() {
        return getOwnerKey();
    }


    // Comments
    public static void fetchCommentsForNews(String newsId, final CommentsCallback callback) {
        String url = API_BASE_URL + COMMENTS_ENDPOINT + newsId;

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(url);
        request.setHeader("Content-Type", "application/json");

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    String responseStr = httpResponse.getResultAsString();
                    if (responseStr == null || responseStr.isEmpty()) {
                        Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Empty response")));
                        return;
                    }
                    Array<CommentItem> comments = parseComments(responseStr);
                    Gdx.app.postRunnable(() -> callback.onSuccess(comments));
                } catch (Exception e) {
                    Gdx.app.error("NewsApiClient", "Error parsing comments response", e);
                    Gdx.app.postRunnable(() -> callback.onFailure(e));
                }
            }

            @Override public void failed(Throwable t) {
                Gdx.app.error("NewsApiClient", "Comments request failed", t);
                Gdx.app.postRunnable(() -> callback.onFailure(t));
            }

            @Override public void cancelled() {
                Gdx.app.log("NewsApiClient", "Comments request cancelled");
                Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Request cancelled")));
            }
        });
    }

    public static void createGuestComment(String newsId, String content, final CreateCommentCallback callback) {
        String url = API_BASE_URL + COMMENTS_ENDPOINT + newsId;

        String safeContent = content == null ? "" : content.replace("\\", "\\\\").replace("\"", "\\\"");
        String ownerKey = getOwnerKey();
        String jsonBody = "{"
            + "\"content\":\"" + safeContent + "\","
            + "\"isSimulated\":true,"
            + "\"simulationId\":\"MAP\","
            + "\"ownerKey\":\"" + ownerKey + "\""
            + "}";

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
        request.setUrl(url);
        request.setHeader("Content-Type", "application/json");
        request.setContent(jsonBody);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    int status = httpResponse.getStatus().getStatusCode();
                    String responseStr = httpResponse.getResultAsString();

                    if (status < 200 || status >= 300) {
                        Gdx.app.postRunnable(() ->
                            callback.onFailure(new Exception("HTTP " + status + ": " + responseStr))
                        );
                        return;
                    }

                    if (responseStr == null || responseStr.isEmpty()) {
                        Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Empty response")));
                        return;
                    }

                    CommentItem created = parseSingleComment(responseStr);
                    Gdx.app.postRunnable(() -> callback.onSuccess(created));

                } catch (Exception e) {
                    Gdx.app.error("NewsApiClient", "Error creating comment", e);
                    Gdx.app.postRunnable(() -> callback.onFailure(e));
                }
            }

            @Override public void failed(Throwable t) {
                Gdx.app.error("NewsApiClient", "Create comment failed", t);
                Gdx.app.postRunnable(() -> callback.onFailure(t));
            }

            @Override public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Request cancelled")));
            }
        });
    }

    public static void deleteMyComment(String commentId, final SimpleCallback callback) {
        String ownerKey = getOwnerKey();
        String url = API_BASE_URL + COMMENTS_MAP_ENDPOINT + commentId + "?ownerKey=" + ownerKey;

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.DELETE);
        request.setUrl(url);
        request.setHeader("Content-Type", "application/json");

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int status = httpResponse.getStatus().getStatusCode();
                String resp = httpResponse.getResultAsString();
                if (status >= 200 && status < 300) {
                    Gdx.app.postRunnable(callback::onSuccess);
                } else {
                    Gdx.app.postRunnable(() -> callback.onFailure(new Exception("HTTP " + status + ": " + resp)));
                }
            }

            @Override public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onFailure(t));
            }

            @Override public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Request cancelled")));
            }
        });
    }

    public static void updateMyComment(String commentId, String newContent, final UpdateCommentCallback callback) {
        String url = API_BASE_URL + COMMENTS_MAP_ENDPOINT + commentId;

        String safe = newContent == null ? "" : newContent.replace("\\", "\\\\").replace("\"", "\\\"");
        String jsonBody = "{"
            + "\"content\":\"" + safe + "\","
            + "\"ownerKey\":\"" + getOwnerKey() + "\""
            + "}";

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.PUT);
        request.setUrl(url);
        request.setHeader("Content-Type", "application/json");
        request.setContent(jsonBody);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    int status = httpResponse.getStatus().getStatusCode();
                    String responseStr = httpResponse.getResultAsString();

                    if (status < 200 || status >= 300) {
                        Gdx.app.postRunnable(() -> callback.onFailure(new Exception("HTTP " + status + ": " + responseStr)));
                        return;
                    }

                    CommentItem updated = parseSingleComment(responseStr);
                    Gdx.app.postRunnable(() -> callback.onSuccess(updated));

                } catch (Exception e) {
                    Gdx.app.postRunnable(() -> callback.onFailure(e));
                }
            }

            @Override public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onFailure(t));
            }

            @Override public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Request cancelled")));
            }
        });
    }


    private static Array<CommentItem> parseComments(String jsonString) {
        Array<CommentItem> comments = new Array<>();
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(jsonString);

        JsonValue arr = root.has("comments") ? root.get("comments") : root;
        if (arr == null || arr.size == 0) return comments;

        for (JsonValue item : arr) {
            if (item == null) continue;
            comments.add(parseCommentObject(item));
        }

        return comments;
    }

    private static CommentItem parseSingleComment(String jsonString) {
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(jsonString);
        return parseCommentObject(root);
    }

    private static CommentItem parseCommentObject(JsonValue item) {
        CommentItem c = new CommentItem();
        c.setId(item.getString("_id", ""));
        c.setContent(item.getString("content", ""));
        c.setCreatedAt(item.getString("createdAt", ""));
        c.setSimulated(item.getBoolean("isSimulated", false));
        c.setOwnerKey(item.getString("ownerKey", null));

        // userId može biti null ili object sa username
        String username = "Guest";
        if (item.has("userId") && item.get("userId") != null && item.get("userId").isObject()) {
            username = item.get("userId").getString("username", "Guest");
        }
        c.setUsername(username);

        return c;
    }

    // News
    public static void fetchProvinceNewsStats(int hours, final ProvinceNewsCallback callback) {
        String url = API_BASE_URL + PROVINCE_NEWS_STATS_ENDPOINT + "?hours=" + hours;

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(url);
        request.setHeader("Content-Type", "application/json");

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    String responseStr = httpResponse.getResultAsString();
                    if (responseStr == null || responseStr.isEmpty()) {
                        Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Empty response")));
                        return;
                    }
                    Array<ProvinceNewsMarker> markers = parseProvinceNewsStats(responseStr);
                    // Post to main thread for UI updates
                    Gdx.app.postRunnable(() -> callback.onSuccess(markers));
                } catch (Exception e) {
                    Gdx.app.error("NewsApiClient", "Error parsing response", e);
                    Gdx.app.postRunnable(() -> callback.onFailure(e));
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.error("NewsApiClient", "Request failed", t);
                Gdx.app.postRunnable(() -> callback.onFailure(t));
            }

            @Override
            public void cancelled() {
                Gdx.app.log("NewsApiClient", "Request cancelled");
                Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Request cancelled")));
            }
        });
    }

    private static Array<ProvinceNewsMarker> parseProvinceNewsStats(String jsonString) {
        Array<ProvinceNewsMarker> markers = new Array<>();
        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(jsonString);

        for (JsonValue item : root) {
            ProvinceNewsMarker marker = new ProvinceNewsMarker();
            marker.setProvinceId(item.getString("provinceId", ""));
            marker.setProvinceName(item.getString("provinceName", ""));
            marker.setProvinceCode(item.getString("provinceCode", ""));
            marker.setCenterLatitude(item.getFloat("centerLatitude", 0));
            marker.setCenterLongitude(item.getFloat("centerLongitude", 0));
            marker.setNewsCount(item.getInt("newsCount", 0));
            markers.add(marker);
        }

        return markers;
    }

    public static void fetchNewsByCategory(String categoryName, int limit, final NewsItemsCallback callback) {
        Array<String> categories = new Array<>();
        categories.add(categoryName);
        fetchNewsByCategories(categories, limit, callback);
    }

    public static void fetchNewsByCategories(Array<String> categoryNames, int limit, final NewsItemsCallback callback) {
        // Build URL with category parameter
        // If "Splošno" is in the list or list is empty, fetch all news
        String url;
        if (categoryNames == null || categoryNames.size == 0 || categoryNames.contains("Splošno", false)) {
            url = API_BASE_URL + NEWS_ENDPOINT + "?limit=" + limit + "&page=1";
        } else {
            // Join categories with comma for backend
            StringBuilder categoriesParam = new StringBuilder();
            for (int i = 0; i < categoryNames.size; i++) {
                if (i > 0) categoriesParam.append(",");
                categoriesParam.append(categoryNames.get(i));
            }
            url = API_BASE_URL + NEWS_ENDPOINT + "?limit=" + limit + "&page=1&category=" + categoriesParam.toString();
        }

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(url);
        request.setHeader("Content-Type", "application/json");

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    String responseStr = httpResponse.getResultAsString();
                    if (responseStr == null || responseStr.isEmpty()) {
                        Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Empty response")));
                        return;
                    }
                    Array<NewsItem> newsItems = parseNewsItems(responseStr);
                    // Post to main thread for UI updates
                    Gdx.app.postRunnable(() -> callback.onSuccess(newsItems));
                } catch (Exception e) {
                    Gdx.app.error("NewsApiClient", "Error parsing news response", e);
                    Gdx.app.postRunnable(() -> callback.onFailure(e));
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.error("NewsApiClient", "News request failed", t);
                Gdx.app.postRunnable(() -> callback.onFailure(t));
            }

            @Override
            public void cancelled() {
                Gdx.app.log("NewsApiClient", "News request cancelled");
                Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Request cancelled")));
            }
        });
    }

    private static Array<NewsItem> parseNewsItems(String jsonString) {
        Array<NewsItem> newsItems = new Array<>();
        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(jsonString);

            // Handle both {news: [...]} and [...] formats
            JsonValue newsArray = root.has("news") ? root.get("news") : root;

            // Check if newsArray is valid
            if (newsArray == null || newsArray.size == 0) {
                return newsItems;
            }

            for (JsonValue item : newsArray) {
                if (item == null) continue;

                NewsItem newsItem = new NewsItem();

                // Basic fields
                newsItem.setId(item.getString("_id", ""));
                newsItem.setTitle(item.getString("title", ""));
                newsItem.setSummary(item.getString("summary", ""));
                newsItem.setContent(item.getString("content", ""));
                newsItem.setAuthor(item.getString("author", ""));
                newsItem.setPublishedAt(item.getString("publishedAt", ""));
                newsItem.setImageUrl(item.getString("imageUrl", ""));
                newsItem.setViews(item.getInt("views", 0));
                newsItem.setLikes(item.getInt("likes", 0));
                newsItem.setDislikes(item.getInt("dislikes", 0));
                newsItem.setCommentsCount(item.getInt("commentsCount", 0));

                // Populated fields
                if (item.has("locationId") && item.get("locationId").isObject()) {
                    JsonValue loc = item.get("locationId");
                    NewsItem.LocationInfo location = new NewsItem.LocationInfo();
                    location.setId(loc.getString("_id", ""));
                    location.setName(loc.getString("name", ""));
                    location.setLatitude(loc.getDouble("latitude", 0.0));
                    location.setLongitude(loc.getDouble("longitude", 0.0));
                    newsItem.setLocation(location);
                }

                if (item.has("categoryId") && item.get("categoryId").isObject()) {
                    JsonValue cat = item.get("categoryId");
                    NewsItem.CategoryInfo category = new NewsItem.CategoryInfo();
                    category.setId(cat.getString("_id", ""));
                    category.setName(cat.getString("name", ""));
                    newsItem.setCategory(category);
                }

                if (item.has("sourceId") && item.get("sourceId").isObject()) {
                    JsonValue src = item.get("sourceId");
                    NewsItem.SourceInfo source = new NewsItem.SourceInfo();
                    source.setId(src.getString("_id", ""));
                    source.setName(src.getString("name", ""));
                    source.setUrl(src.getString("url", ""));
                    newsItem.setSource(source);
                }

                newsItems.add(newsItem);
            }
        } catch (Exception e) {
            Gdx.app.error("NewsApiClient", "Error parsing news JSON", e);
        }

        return newsItems;
    }
}

