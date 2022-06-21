package dk.kb.netarchivesuite.solrwayback.parsers.json;

public class TweetMedia {
    private String mediaUrl;

    private String type;

    private TweetVideoInfo videoInfo;

    public TweetMedia() {
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TweetVideoInfo getVideoInfo() {
        return videoInfo;
    }

    public void setVideoInfo(TweetVideoInfo videoInfo) {
        this.videoInfo = videoInfo;
    }
}
