package dk.kb.netarchivesuite.solrwayback.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrStreamingWarcExportClient {

  private static SolrClient solrServer;
  private final Logger log = LoggerFactory.getLogger(SolrStreamingWarcExportClient.class);
  private String cursorMark = CursorMarkParams.CURSOR_MARK_START;
    
  
  public SolrStreamingWarcExportClient(String solrServerUrl){
    solrServer =  new HttpSolrClient.Builder(solrServerUrl).build();
    //solrServer.setRequestWriter(new BinaryRequestWriter()); 
    cursorMark = CursorMarkParams.CURSOR_MARK_START; //Reset to start again 
  }
   
  
  //
  public SolrDocumentList exportWarcBuffered(String query, String filterQuery, int pageSize) throws Exception {   
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","source_file_path,source_file_offset");
    solrQuery.add("sort","score desc, id asc");
    solrQuery.setQuery(query);  
    solrQuery.setRows(pageSize);            
    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark );
    
    if (filterQuery != null){
      solrQuery.setFilterQueries(filterQuery);
    }
    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
     
    cursorMark = rsp.getNextCursorMark();
    //log.info("next cursormark warc export:"+cursorMark );
    SolrDocumentList docs = rsp.getResults();
         
    return docs;
  }
}
