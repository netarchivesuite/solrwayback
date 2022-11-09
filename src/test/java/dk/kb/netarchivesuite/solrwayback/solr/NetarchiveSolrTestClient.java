package dk.kb.netarchivesuite.solrwayback.solr;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetarchiveSolrTestClient extends NetarchiveSolrClient{

  private static final Logger log = LoggerFactory.getLogger(NetarchiveSolrTestClient.class);
  /*
   * Called from unittest   
   */
  public static void initializeOverLoadUnitTest(EmbeddedSolrServer server) {
    solrServer=server;
    noCacheSolrServer=server;
    instance = new NetarchiveSolrTestClient();
    log.info("SolrClient initialized with embedded solr for unittest");
  }


  
}
