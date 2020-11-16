package dk.kb.netarchivesuite.solrwayback.service.dto;

import java.util.ArrayList;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TimestampsForPage {

  private Date pageCrawlDate;
  private String pageUrl;
  private String pagePreviewUrl;
  private String maximumTimeDifferenceBackward;
  private String maximumTimeDifferenceForward;
  private ArrayList<PageResource> resources = new ArrayList<PageResource>();
  private ArrayList<String> notHarvested =  new ArrayList<String>();

  
  public Date getPageCrawlDate() {
    return pageCrawlDate;
  }

  public void setPageCrawlDate(Date pageCrawlDate) {
    this.pageCrawlDate = pageCrawlDate;
  }

  public String getPageUrl() {
    return pageUrl;
  }

  public void setPageUrl(String pageUrl) {
    this.pageUrl = pageUrl;
  }

  public String getPagePreviewUrl() {
    return pagePreviewUrl;
  }

  public void setPagePreviewUrl(String pagePreviewUrl) {
    this.pagePreviewUrl = pagePreviewUrl;
  }

 

  public ArrayList<PageResource> getResources() {
    return resources;
  }

  public void setResources(ArrayList<PageResource> resources) {
    this.resources = resources;
  }

  public ArrayList<String> getNotHarvested() {
    return notHarvested;
  }

  public void setNotHarvested(ArrayList<String> notHarvested) {
    this.notHarvested = notHarvested;
  }

public String getMaximumTimeDifferenceBackward() {
    return maximumTimeDifferenceBackward;
}

public void setMaximumTimeDifferenceBackward(String maximumTimeDifferenceBackward) {
    this.maximumTimeDifferenceBackward = maximumTimeDifferenceBackward;
}

public String getMaximumTimeDifferenceForward() {
    return maximumTimeDifferenceForward;
}

public void setMaximumTimeDifferenceForward(String maximumTimeDifferenceForward) {
    this.maximumTimeDifferenceForward = maximumTimeDifferenceForward;
}
  
  
}
