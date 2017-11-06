package dk.kb.netarchivesuite.solrwayback.export;

import java.io.PrintWriter;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamingExportClient;

public class TestGenerateCSV {
  
  public static void main(String[] args) throws Exception{   
    
    PropertiesLoader.initProperties();
    
    
     SolrStreamingExportClient solr =  new SolrStreamingExportClient("http://narcana-data10.statsbiblioteket.dk:9000/solr/netarchivebuilder");

     String query = "domain:denstoredanske.dk";
     String filter = null;
     StreamingSolrExportBufferedInputStream streamExport = new StreamingSolrExportBufferedInputStream (solr, query,filter,50000,false,1000000);
    
     PrintWriter writer = new PrintWriter("export.txt", "UTF-8");
            
     int read = streamExport.read(); 
    while (read != -1){
    //  System.out.print(Character.toString((char) read));
      writer.write(Character.toString((char) read));
      read=streamExport.read();
    }
    writer.close();
   
    
  }

}
