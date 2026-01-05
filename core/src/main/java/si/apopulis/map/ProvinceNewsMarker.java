package si.apopulis.map;

public class ProvinceNewsMarker {
    private String provinceId;
    private String provinceName;
    private String provinceCode;
    private float centerLatitude;
    private float centerLongitude;
    private int newsCount;

    public ProvinceNewsMarker() {
    }

    public ProvinceNewsMarker(String provinceId, String provinceName, String provinceCode,
                              float centerLatitude, float centerLongitude, int newsCount) {
        this.provinceId = provinceId;
        this.provinceName = provinceName;
        this.provinceCode = provinceCode;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.newsCount = newsCount;
    }

    public String getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(String provinceId) {
        this.provinceId = provinceId;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public float getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(float centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public float getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(float centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public int getNewsCount() {
        return newsCount;
    }

    public void setNewsCount(int newsCount) {
        this.newsCount = newsCount;
    }

    @Override
    public String toString() {
        return "ProvinceNewsMarker{" +
                "provinceName='" + provinceName + '\'' +
                ", newsCount=" + newsCount +
                ", centerLat=" + centerLatitude +
                ", centerLon=" + centerLongitude +
                '}';
    }
}

