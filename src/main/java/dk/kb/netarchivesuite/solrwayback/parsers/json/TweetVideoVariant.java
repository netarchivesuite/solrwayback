package dk.kb.netarchivesuite.solrwayback.parsers.json;

public class TweetVideoVariant {
    private String url;

    private int bitrate;

    private String contentType;

    public TweetVideoVariant() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
