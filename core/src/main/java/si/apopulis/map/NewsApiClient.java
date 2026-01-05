package si.apopulis.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class NewsApiClient {
    // You can change this to your production API URL
    private static final String API_BASE_URL = System.getProperty("api.base.url", "http://localhost:5001/api");
    private static final String PROVINCE_NEWS_STATS_ENDPOINT = "/provinces/stats/news";
    private static final String NEWS_ENDPOINT = "/news";

    public interface ProvinceNewsCallback {
        void onSuccess(Array<ProvinceNewsMarker> markers);
        void onFailure(Throwable error);
    }

    public interface NewsItemsCallback {
        void onSuccess(Array<NewsItem> newsItems);
        void onFailure(Throwable error);
    }

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
        // Use the /news endpoint with limit
        // Note: Category filtering by name would require category ID lookup
        // For now, fetch all recent news (they'll be filtered by published date)
        String url = API_BASE_URL + NEWS_ENDPOINT + "?limit=" + limit + "&page=1";

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

