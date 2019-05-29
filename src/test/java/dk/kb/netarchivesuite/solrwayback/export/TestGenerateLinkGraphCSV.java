package dk.kb.netarchivesuite.solrwayback.export;

import java.io.PrintWriter;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamingLinkGraphCSVExportClient;

public class TestGenerateLinkGraphCSV {

    private static final String SOLR_SERVER = "http://belinda:8983/solr/netarchivebuilder";

    public static void main(String[] args) throws Exception{
    
    PropertiesLoader.initProperties();
    
    String query = "katte";
    
     SolrStreamingLinkGraphCSVExportClient solr =  SolrStreamingLinkGraphCSVExportClient.createExporter(SOLR_SERVER, query);

    //MAX 100.000 results
     StreamingSolrExportBufferedInputStream streamExport = new StreamingSolrExportBufferedInputStream(solr,100000);
    
     PrintWriter writer = new PrintWriter("target/linkgraph.csv", "UTF-8");
            
     int read = streamExport.read(); 
    while (read != -1){
    //  System.out.print(Character.toString((char) read));
      writer.write(Character.toString((char) read));
      read=streamExport.read();
    }
    writer.close();
   
    streamExport.close();
    
  }

}
