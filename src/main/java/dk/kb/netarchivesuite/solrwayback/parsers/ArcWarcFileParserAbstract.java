package dk.kb.netarchivesuite.solrwayback.parsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArcWarcFileParserAbstract {
  private static final Logger log = LoggerFactory.getLogger(ArcWarcFileParserAbstract.class);

  public static int getStatusCode(String line){//HTTP/1.1 302 Object moved      
    String[] tokens = line.split(" ");
    String status = tokens[1];
    return Integer.parseInt(status);     
  }
}
