package dk.kb.netarchivesuite.solrwayback.solr;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class SolrStreamingLinkGraphCSVExportClient implements SolrStreamingLineBasedExportClientInterface {

  public static final String LINKGRAPH_FL = "id,domain,links_domains";
  
  public static final int DEFAULT_PAGE_SIZE = 25000;
  private static final String filters = "content_type_norm:html AND links_domains:* AND url_type:slashpage";
  
  // to see how many distinct domains, from solr admin: stats=true&stats.field=domain&f.domain.stats.cardinality=true
  
  private final Logger log = LoggerFactory.getLogger(SolrStreamingLinkGraphCSVExportClient.class);
  private final SolrGenericStreaming inner;
  private final String solrFields;
  private final String[] solrFieldsArray;
  private final String csvFields;
  private final String[] csvFieldsArray;
  private final String query;
  
  private HashSet<String> domainsCache= new HashSet<String>(); 

  public SolrStreamingLinkGraphCSVExportClient(
          String solrServerUrl, int pageSize, String solrFields, String csvFields,String query) {
    if (solrFields == null || solrFields.isEmpty() || csvFields == null || csvFields.isEmpty()) {
      throw new IllegalArgumentException("fields argument was empty, but must be specified");
    }
    this.solrFieldsArray = solrFields.split(", *");
    this.csvFieldsArray = csvFields.split(", *");
    inner = new SolrGenericStreaming(
            solrServerUrl, pageSize, Arrays.asList(solrFieldsArray), false ,false, 
            query, filters);
    this.solrFields = solrFields;
    this.csvFields = csvFields;
    this.query = query;
    //solrServer.setRequestWriter(new BinaryRequestWriter()); 
  }

  public static SolrStreamingLinkGraphCSVExportClient createExporter(
          String solrServerUrl,  String query) {
    return new SolrStreamingLinkGraphCSVExportClient(
            solrServerUrl, DEFAULT_PAGE_SIZE,  LINKGRAPH_FL, LINKGRAPH_FL, query);
  }

  
  
  
  /*
   * Gephi syntax:
   * a;b;c;d;
   * means a -> b, a -> c,  a->d
   * and this is exactly what we want to do here. a is the domain.
   * 
   */
  public String next() throws Exception {
    log.info("Next called for link graph export:"+query);
    if (inner.hasFinished()) {
      return "";
    }
    StringBuffer export = new StringBuffer();

    SolrDocumentList docs = inner.nextDocuments();
    if (docs == null || docs.isEmpty()) {
      return export.toString();
    }

    for ( SolrDocument doc : docs){
      String domain = (String) doc.getFieldValue("domain");
      ArrayList<String> links = (ArrayList<String>) doc.getFieldValue("links_domains");
      
      
      //TODO make make this limit configurable.
      //if (domainsCache.contains(domain) || links.size() > 1000){ //Only extract each domain once, and no link spammers! Some sites have 100.000 links. 
      if (domainsCache.contains(domain)){ //Only extract each domain once, and no link spammers! Some sites have 100.000 links.
         continue;
      }
      domainsCache.add(domain); 
      
          
      links.remove(domain); //Remove links from the domain to itself.
      if (links.size() >0){ //If now links, dont write      
        export.append(domain); //the domain      
        export.append(",");
        export.append(String.join(",", links)); //All the links comma separated
        export.append("\n");
      }
      
    }
    //if none added, reload. Simple fix to avoid returning empty string when extraction not completed
    if(export.toString().length() == 0){
      log.info("Empty resultset for buffer but more documents to load, automatic reload");
      return next();
    }
    
    
    //System.out.println(export.toString());
    return export.toString();
  }

  @Override
  public int getPageSize() {
    return DEFAULT_PAGE_SIZE;
  }

}
