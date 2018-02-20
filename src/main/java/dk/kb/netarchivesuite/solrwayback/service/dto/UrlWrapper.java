package dk.kb.netarchivesuite.solrwayback.service.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UrlWrapper {

  private String url;
  
  public UrlWrapper(){
    
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
    
  
}
