package dk.kb.netarchivesuite.solrwayback.export;

import java.io.PrintWriter;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamingExportClient;

public class TestGenerateCSV {

    private static final String NARC10 = "http://narcana-data10.statsbiblioteket.dk:9000/solr/netarchivebuilder";

    public static void main(String[] args) throws Exception{
    
    PropertiesLoader.initProperties();
    
    String query = "domain:denstoredanske.dk";
    String filter = null;

     SolrStreamingExportClient solr =  SolrStreamingExportClient.createExporter(NARC10, true, query, filter);

     StreamingSolrExportBufferedInputStream streamExport = new StreamingSolrExportBufferedInputStream(
             solr, 50000,1000000);
    
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
