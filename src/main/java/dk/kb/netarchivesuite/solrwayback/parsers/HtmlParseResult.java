package dk.kb.netarchivesuite.solrwayback.parsers;

public class HtmlParseResult {
  
  private String htmlReplaced;
  private int numberOfLinksReplaced;
  private int numberOfLinksNotFound;
  
  
  public HtmlParseResult(){    
  }

  public String getHtmlReplaced() {
    return htmlReplaced;
  }

  public void setHtmlReplaced(String htmlReplaced) {
    this.htmlReplaced = htmlReplaced;
  }

  public int getNumberOfLinksReplaced() {
    return numberOfLinksReplaced;
  }


  public void setNumberOfLinksReplaced(int numberOfLinksReplaced) {
    this.numberOfLinksReplaced = numberOfLinksReplaced;
  }


  public int getNumberOfLinksNotFound() {
    return numberOfLinksNotFound;
  }


  public void setNumberOfLinksNotFound(int numberOfLinksNotFound) {
    this.numberOfLinksNotFound = numberOfLinksNotFound;
  }
  
  

}
