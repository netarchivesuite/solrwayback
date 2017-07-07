package dk.kb.netarchivesuite.solrwayback.export;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import dk.kb.netarchivesuite.solrwayback.solr.SolrClient;


/*
 * This class is not intended for use. It is supposed to stream export for unlimited result size CSV export.
 * But it will not work before the 'id' field is made docval, or it will be too slow.  
 */


public class StreamingSolrExportBufferedInputStream extends  InputStream  {
  
  private int index;
  private List<byte[]> inputBuffer = new ArrayList<>();
  private int maxLines;
  private int linesRead;
  private SolrClient solrClient;
  private String query;
  private String filterQuery;
  private int solrPagingBufferSize;
  private boolean fullExport;
  @Override
  public int read() throws IOException {
     
     /*
     if (inputBuffer.isEmpty()) {
          return -1;
     }      
     */
    if ( linesRead == maxLines){
      return -1; 
    }
    if (inputBuffer.size() ==0){
      loadMore();  
      
    }
    
      // Get first element of the List
      byte[] bytes = inputBuffer.get(0);
      // Get the byte corresponding to the index and post increment the current index
      byte result = bytes[index++];
      if (index >= bytes.length) {
          // It was the last index of the byte array so we remove it from the list
          // and reset the current index
          inputBuffer.remove(0);
          index = 0;
          linesRead++;
      }
      return result;
  }

  
  public StreamingSolrExportBufferedInputStream(SolrClient solrClient, String query, String filterQuery, int solrPagingBufferSize, boolean fullExport,int maxBytes){    
    this.solrClient = solrClient;
    this.maxLines=maxBytes;
    this.query=query;
    this.filterQuery=filterQuery;
    this.solrPagingBufferSize = solrPagingBufferSize;    
    this.fullExport=fullExport;
   }
  
  
  
  private void loadMore(){
    System.out.println("loading more");
    inputBuffer.add("123456789\n".getBytes());    
    inputBuffer.add("987654321\n".getBytes());
    
  }
  
  /*
   * This method is supposed to be in SolrClient  
   * public String exportBriefBuffered(String query, String filterQuery, int results, String cursorMark) throws Exception {
    log.info("export brief:" + query +" and filter:"+filterQuery);
    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.add("fl","id,title, arc_full,url,source_file_s,crawl_date,wayback_date");
    solrQuery.add("sort","id asc");
    solrQuery.setQuery(query); // only search images
    solrQuery.setRows(results);
    solrQuery.set("cursorMark","*");
    if (filterQuery != null){
      solrQuery.setFilterQueries(filterQuery);
    }
System.out.println("before query");
    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    System.out.println("after query");
    SolrDocumentList docs = rsp.getResults();
    StringBuffer export = new StringBuffer();
    GenerateCSV.generateFirstLineHeader(export);    
    GenerateCSV.generateSecondLineHeader(export, query, filterQuery);
    GenerateCSV.addHeadlineBrief(export);
            
    long numFound = docs.getNumFound();    
    for ( SolrDocument doc : docs){
     GenerateCSV.generateLineBrief(export, doc);
     doc = null;// For garbage collection 
    }    
    
    return export.toString();
  }
   * 
   */
}
