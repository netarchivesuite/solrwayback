package dk.kb.netarchivesuite.solrwayback.service.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MementoDoc {

    private String crawlDate; // format 2009-12-09T05:32:50Z
    private String url;
    private String url_norm;
    private int content_length;
    private Long wayback_date;
    private String content_type;
    private String source_file_path;
    private long source_file_offset;

    public MementoDoc(){
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl_norm() {
        return url_norm;
    }
    public void setUrl_norm(String url_norm) {
        this.url_norm = url_norm;
    }

    public String getCrawlDate() {
        return crawlDate;
    }
    public void setCrawlDate(String crawlDate) {
        this.crawlDate = crawlDate;
    }

    public int getContent_length() {return content_length;}
    public void setContent_length(int content_length) {this.content_length = content_length;}

    public Long getWayback_date() {return wayback_date;}
    public void setWayback_date(Long wayback_date) {this.wayback_date = wayback_date;}

    public String getContent_type() {return content_type;}
    public void setContent_type(String content_type) {this.content_type = content_type;}

    public String getSource_file_path() {return source_file_path;}
    public void setSource_file_path(String source_file_path) {this.source_file_path = source_file_path;}

    public void setSource_file_offset(long source_file_offset) {this.source_file_offset = source_file_offset;}
    public long getSource_file_offset() {return source_file_offset;}
}
