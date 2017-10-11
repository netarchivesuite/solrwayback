package dk.kb.netarchivesuite.solrwayback.service.dto;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PageResource {

  private String url;
  private String downloadUrl;
  private String contentType;
  private Date crawlTime;
  private String timeDifference;
  
  public PageResource(){
    
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Date getCrawlTime() {
    return crawlTime;
  }

  public void setCrawlTime(Date crawlTime) {
    this.crawlTime = crawlTime;
  }

  public String getTimeDifference() {
    return timeDifference;
  }

  public void setTimeDifference(String timeDifference) {
    this.timeDifference = timeDifference;
  }
  
  
}
