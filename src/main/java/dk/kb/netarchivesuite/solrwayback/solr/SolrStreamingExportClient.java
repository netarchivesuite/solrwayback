package dk.kb.netarchivesuite.solrwayback.solr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.export.GenerateCSV;

public class SolrStreamingExportClient {

  private HttpSolrClient solrServer;
  private final Logger log = LoggerFactory.getLogger(SolrStreamingExportClient.class);
  private String cursorMark = CursorMarkParams.CURSOR_MARK_START;
    
  
  public SolrStreamingExportClient(String solrServerUrl){
    solrServer = new HttpSolrClient(solrServerUrl);
    solrServer.setRequestWriter(new BinaryRequestWriter()); 
    cursorMark = CursorMarkParams.CURSOR_MARK_START; //Reset to start again 
  }
   
  
  public String exportBriefBuffered(String query, String filterQuery, int pageSize) throws Exception {
    log.info("export brief:" + query +" and filter:"+filterQuery);
    StringBuffer export = new StringBuffer();
    if (CursorMarkParams.CURSOR_MARK_START.equals(cursorMark)){ //generate header only first time  
      GenerateCSV.generateFirstLineHeader(export);    //Add this to first cursor mark
      GenerateCSV.generateSecondLineHeader(export, query, filterQuery);
      GenerateCSV.addHeadlineBrief(export);                             
    }
    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","title, url,source_file_path,crawl_date,wayback_date");
    solrQuery.add("sort","id asc");
    solrQuery.setQuery(query); // only search images
    solrQuery.setRows(pageSize);            
    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark );
    
    if (filterQuery != null){
      solrQuery.setFilterQueries(filterQuery);
    }
    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
     
    cursorMark = rsp.getNextCursorMark();
    log.info("next cursormark brief export:"+cursorMark );
    
    SolrDocumentList docs = rsp.getResults();
  
    
    for ( SolrDocument doc : docs){
     GenerateCSV.generateLineBrief(export, doc);     
     doc = null;// For garbage collection 
    }    
    System.out.println("return size:"+export.length());
    return export.toString();
  }


  public String exportFullBuffered(String query, String filterQuery, int pageSize) throws Exception {
    log.info("export full:" + query +" and filter:"+filterQuery);
    StringBuffer export = new StringBuffer();
    if (CursorMarkParams.CURSOR_MARK_START.equals(cursorMark)){ //generate header only first time  
    
    
    GenerateCSV.generateFirstLineHeader(export);    
    GenerateCSV.generateSecondLineHeader(export, query, filterQuery);
    GenerateCSV.addHeadlineFull(export);
    }
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","title, host, public_suffix, crawl_year, content_type, content_language url, source_file_path,url,source_file_path,crawl_date,wayback_date");    
    solrQuery.add("sort","id asc");
    solrQuery.setQuery(query); 
    solrQuery.setRows(pageSize);            
    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark );
    
    
    if (filterQuery != null){
      solrQuery.setFilterQueries(filterQuery);
    }

    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    cursorMark = rsp.getNextCursorMark();
    log.info("next cursormark full export:"+cursorMark );
    
    
    SolrDocumentList docs = rsp.getResults();

    
    for ( SolrDocument doc : docs){
      GenerateCSV.generateLineFull(export, doc);
      doc = null;// For garbage collection
    }    

    return export.toString();
  }

}
