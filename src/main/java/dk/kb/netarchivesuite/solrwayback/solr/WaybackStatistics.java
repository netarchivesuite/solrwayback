package dk.kb.netarchivesuite.solrwayback.solr;


public class WaybackStatistics {

  private String url_norm;
  private long numberOfHarvest;
  private long domainHarvestTotalContentLength;
  private long numberHarvestDomain;
  private String domain;
  private String firstHarvestDate;
  private String nextHarvestDate;
  private String previousHarvestDate;
  private String lastHarvestDate;
  private String harvestDate;
 
    
  public String getUrl_norm() {
    return url_norm;
  }
  public void setUrl_norm(String url_norm) {
    this.url_norm = url_norm;
  }
  public long getNumberOfHarvest() {
    return numberOfHarvest;
  }
  public void setNumberOfHarvest(long numberOfHarvest) {
    this.numberOfHarvest = numberOfHarvest;
  }
  public String getFirstHarvestDate() {
    return firstHarvestDate;
  }
  public void setFirstHarvestDate(String firstHarvestDate) {
    this.firstHarvestDate = firstHarvestDate;
  }
  public String getNextHarvestDate() {
    return nextHarvestDate;
  }
  public void setNextHarvestDate(String nextHarvestDate) {
    this.nextHarvestDate = nextHarvestDate;
  }
  public String getPreviousHarvestDate() {
    return previousHarvestDate;
  }
  public void setPreviousHarvestDate(String previousHarvestDate) {
    this.previousHarvestDate = previousHarvestDate;
  }
  public String getLastHarvestDate() {
    return lastHarvestDate;
  }
  public void setLastHarvestDate(String lastHarvestDate) {
    this.lastHarvestDate = lastHarvestDate;
  }
  public String getHarvestDate() {
    return harvestDate;
  }
  public void setHarvestDate(String harvestDate) {
    this.harvestDate = harvestDate;
  }
  public long getNumberHarvestDomain() {
    return numberHarvestDomain;
  }
  public void setNumberHarvestDomain(long numberHarvestDomain) {
    this.numberHarvestDomain = numberHarvestDomain;
  }
  public String getDomain() {
    return domain;
  }
  public void setDomain(String domain) {
    this.domain = domain;
  }
  public long getDomainHarvestTotalContentLength() {
    return domainHarvestTotalContentLength;
  }
  public void setDomainHarvestTotalContentLength(long domainHarvestTotalContentLength) {
    this.domainHarvestTotalContentLength = domainHarvestTotalContentLength;
  }
  @Override
  public String toString() {
    return "WaybackStatistics [url_norm=" + url_norm + ", numberOfHarvest=" + numberOfHarvest
        + ", domainHarvestTotalContentLength=" + domainHarvestTotalContentLength + ", numberHarvestDomain="
        + numberHarvestDomain + ", domain=" + domain + ", firstHarvestDate=" + firstHarvestDate + ", nextHarvestDate="
        + nextHarvestDate + ", previousHarvestDate=" + previousHarvestDate + ", lastHarvestDate=" + lastHarvestDate
        + ", harvestDate=" + harvestDate + "]";
  }
     
}
