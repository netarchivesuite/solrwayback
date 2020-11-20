package dk.kb.netarchivesuite.solrwayback.service.dto;

public class PagePreview {

  private String pagePreviewUrl;
  private Long crawlDate;
  private String solrWaybackUrl;
  
  public PagePreview(){
    
  }

  public String getPagePreviewUrl() {
    return pagePreviewUrl;
  }

  public void setPagePreviewUrl(String pagePreviewUrl) {
    this.pagePreviewUrl = pagePreviewUrl;
  }

  public Long getCrawlDate() {
    return crawlDate;
  }

  public void setCrawlDate(Long crawlDate) {
    this.crawlDate = crawlDate;
  }

  public String getSolrWaybackUrl() {
    return solrWaybackUrl;
  }

  public void setSolrWaybackUrl(String solrWaybackUrl) {
    this.solrWaybackUrl = solrWaybackUrl;
  }
  
  
}
