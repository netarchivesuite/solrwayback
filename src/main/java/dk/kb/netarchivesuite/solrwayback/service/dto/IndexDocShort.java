package dk.kb.netarchivesuite.solrwayback.service.dto;


import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class IndexDocShort {
  
    private long offset;
    private String source_file_path;
    private String crawlDate; // format 2009-12-09T05:32:50Z        
    private String url;
    private String url_norm;
    
    public IndexDocShort(){        
    }

    public long getOffset() {
      return offset;
    }

    public void setOffset(long offset) {
      this.offset = offset;
    }

    public String getSource_file_path() {
      return source_file_path;
    }

    public void setSource_file_path(String source_file_path) {
      this.source_file_path = source_file_path;
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
    
    
    
}
