package dk.kb.netarchivesuite.solrwayback.solr;

public interface SolrStreamingLineBasedExportClientInterface {

  public String next() throws Exception;
  
  public int getPageSize();
  
}
