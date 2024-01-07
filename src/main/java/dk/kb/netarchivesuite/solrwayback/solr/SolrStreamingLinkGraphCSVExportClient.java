package dk.kb.netarchivesuite.solrwayback.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class SolrStreamingLinkGraphCSVExportClient implements SolrStreamingLineBasedExportClientInterface {

  public static final String LINKGRAPH_FL = "id,domain,links_domains";
  
  public static final int DEFAULT_PAGE_SIZE = 25000;
  private static final String filters = "content_type_norm:html AND links_domains:* AND url_type:slashpage";
  
  // to see how many distinct domains, from solr admin: stats=true&stats.field=domain&f.domain.stats.cardinality=true
  
  private final Logger log = LoggerFactory.getLogger(SolrStreamingLinkGraphCSVExportClient.class);
  private final Iterator<SolrDocument> solrDocs;
  private final String solrFields;
  private final String[] solrFieldsArray;
  private final String csvFields;
  private final String[] csvFieldsArray;
  private final String query;
  
  private HashSet<String> domainsCache= new HashSet<String>(); 

  public SolrStreamingLinkGraphCSVExportClient(
          SolrClient solrClient, int pageSize, String solrFields, String csvFields, String query) {
    if (solrFields == null || solrFields.isEmpty() || csvFields == null || csvFields.isEmpty()) {
      throw new IllegalArgumentException("fields argument was empty, but must be specified");
    }
    this.solrFieldsArray = solrFields.split(", *");
    this.csvFieldsArray = csvFields.split(", *");

    // TODO: Handle closing in case of exceptions
    //solrDocs = SolrGenericStreaming.iterate(
    solrDocs = SRequest.builder().
            solrClient(solrClient).
            query(query).filterQueries(filters).
            fields(solrFields).
            pageSize(pageSize).
            iterate();

    this.solrFields = solrFields;
    this.csvFields = csvFields;
    this.query = query;
    //solrServer.setRequestWriter(new BinaryRequestWriter()); 
  }

  public static SolrStreamingLinkGraphCSVExportClient createExporter(SolrClient solrClient,  String query) {
    return new SolrStreamingLinkGraphCSVExportClient(
            solrClient, DEFAULT_PAGE_SIZE,  LINKGRAPH_FL, LINKGRAPH_FL, query);
  }

  /*
   * Gephi syntax:
   * a;b;c;d;
   * means a -> b, a -> c,  a->d
   * and this is exactly what we want to do here. a is the domain.
   * 
   */
  @SuppressWarnings("unchecked")
  @Override
  public String next() throws Exception {
    StringBuilder export = new StringBuilder();

    while (export.length() == 0 && solrDocs.hasNext()) {
      SolrDocument doc = solrDocs.next();
      String domain = (String) doc.getFieldValue("domain");
      ArrayList<String> links = (ArrayList<String>) doc.getFieldValue("links_domains");

      //TODO make make this limit configurable.
      //if (domainsCache.contains(domain) || links.size() > 1000){ //Only extract each domain once, and no link spammers! Some sites have 100.000 links.
      if (domainsCache.contains(domain)) { //Only extract each domain once, and no link spammers! Some sites have 100.000 links.
        // TODO: Maybe sort by domain and collect all links from each domain with a set limit instead?
        continue;
      }
      domainsCache.add(domain);


      links.remove(domain); //Remove links from the domain to itself.
      if (links.size() > 0) { //If now links, dont write
        export.append(domain); //the domain
        export.append(",");
        export.append(String.join(",", links)); //All the links comma separated
        export.append("\n");
      }
    }
    return export.toString();
  }

  @Override
  public int getPageSize() {
    return DEFAULT_PAGE_SIZE;
  }

}
