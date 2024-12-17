package dk.kb.netarchivesuite.solrwayback.export;

import java.io.PrintWriter;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamingExportClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;

public class TestGenerateCSV {

    private static final String SOLR = "http://localhost:8983/solr/netarchivebuilder";

    public static void main(String[] args) throws Exception{
    
    PropertiesLoader.initProperties(UnitTestUtils.getFile("properties/solrwayback_unittest.properties").getPath());
    
    String query = "thomas egense";
    String filter = null;

    String fields = "id, domain,  hash , links_images  ";
    SolrClient solrClient = new HttpJdkSolrClient.Builder(SOLR).build();
     SolrStreamingExportClient solr =  SolrStreamingExportClient.createCvsExporter(solrClient, query,fields, filter);

     StreamingSolrExportBufferedInputStream streamExport = new StreamingSolrExportBufferedInputStream(solr,100);
    
     PrintWriter writer = new PrintWriter("export.txt", "UTF-8");
            
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
