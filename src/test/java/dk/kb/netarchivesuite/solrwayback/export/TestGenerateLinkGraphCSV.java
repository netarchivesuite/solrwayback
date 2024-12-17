package dk.kb.netarchivesuite.solrwayback.export;

import java.io.PrintWriter;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamingLinkGraphCSVExportClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;

public class TestGenerateLinkGraphCSV {

    private static final String SOLR_SERVER = "http://belinda:8983/solr/netarchivebuilder";

    public static void main(String[] args) throws Exception{
    
    PropertiesLoader.initProperties(UnitTestUtils.getFile("properties/solrwayback_unittest.properties").getPath());
    
    String query = "katte";

    SolrClient solrClient = new HttpJdkSolrClient.Builder(PropertiesLoader.SOLR_SERVER).build();
     SolrStreamingLinkGraphCSVExportClient solr =  SolrStreamingLinkGraphCSVExportClient.createExporter(solrClient, query);

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
