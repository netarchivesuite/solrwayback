package dk.kb.netarchivesuite.solrwayback.service.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ImageUrl {

  private String downloadUrl;
  private String imageUrl; 
  private String hash;
  private String urlNorm;
  
  public ImageUrl(){    
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getUrlNorm() {
    return urlNorm;
  }

  public void setUrlNorm(String urlNorm) {
    this.urlNorm = urlNorm;
  }
     
  
}
