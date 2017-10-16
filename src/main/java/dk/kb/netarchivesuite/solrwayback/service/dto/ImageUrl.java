package dk.kb.netarchivesuite.solrwayback.service.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ImageUrl {

  private String downloadUrl;
  private String imageUrl; 
  private String hash;
  private String urlNorm;
  private Double latitude;
  private Double longitude;
  private String resourceName;
  
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

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public String getResourceName() {
    return resourceName;
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }     
    
}
