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

    public interface ProvinceNewsCallback {
        void onSuccess(Array<ProvinceNewsMarker> markers);
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
                    Array<ProvinceNewsMarker> markers = parseProvinceNewsStats(responseStr);
                    callback.onSuccess(markers);
                } catch (Exception e) {
                    Gdx.app.error("NewsApiClient", "Error parsing response", e);
                    callback.onFailure(e);
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.error("NewsApiClient", "Request failed", t);
                callback.onFailure(t);
            }

            @Override
            public void cancelled() {
                Gdx.app.log("NewsApiClient", "Request cancelled");
                callback.onFailure(new Exception("Request cancelled"));
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
}

