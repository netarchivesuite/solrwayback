package dk.kb.netarchivesuite.solrwayback.export;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.solr.SolrClient;

public class TestGenerateCSV {
  
  public static void main(String[] args) throws Exception{   
    
    PropertiesLoader.initProperties();
    SolrClient solr = SolrClient.getInstance();
   
    
    String csv = solr.exportBrief("thomas egense",null, 200);
    
    
    System.out.println(csv);
   
    
  }

}
