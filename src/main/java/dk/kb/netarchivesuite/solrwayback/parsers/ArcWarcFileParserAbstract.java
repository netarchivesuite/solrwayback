package dk.kb.netarchivesuite.solrwayback.parsers;

public class ArcWarcFileParserAbstract {

  

  public static int getStatusCode(String line){//HTTP/1.1 302 Object moved      
    String[] tokens = line.split(" ");
    String status = tokens[1];
    return Integer.parseInt(status);     
    
  }
  
}
