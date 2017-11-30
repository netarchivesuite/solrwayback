package dk.kb.netarchivesuite.solrwayback.service.dto.statistics;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DomainYearStatistics {

  private int year;
  private int ingoingLinks;
  private int sizeInKb;
  private int uniquePages;
  private String domain;
  
  public  DomainYearStatistics(){       
  }


  public int getYear() {
    return year;
  }


  public void setYear(int year) {
    this.year = year;
  }





  public int getIngoingLinks() {
    return ingoingLinks;
  }


  public void setIngoingLinks(int ingoingLinks) {
    this.ingoingLinks = ingoingLinks;
  }


  public int getSizeInKb() {
    return sizeInKb;
  }


  public void setSizeInKb(int sizeInKb) {
    this.sizeInKb = sizeInKb;
  }


  public int getTotalPages() {
    return uniquePages;
  }


  public void setTotalPages(int totalPages) {
    this.uniquePages = totalPages;
  }


  public String getDomain() {
    return domain;
  }


  public void setDomain(String domain) {
    this.domain = domain;
  }
  
  
  
  
}
