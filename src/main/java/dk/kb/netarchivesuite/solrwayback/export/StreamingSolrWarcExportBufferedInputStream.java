package dk.kb.netarchivesuite.solrwayback.export;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import dk.kb.netarchivesuite.solrwayback.parsers.WarcParser;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamingWarcExportClient;

public class StreamingSolrWarcExportBufferedInputStream extends InputStream{

  private int index;
  private List<byte[]> inputBuffer = new ArrayList<>();
  private int maxRecords;
  private int linesRead;
  private SolrStreamingWarcExportClient solrClient;
  private String query;
  private String filterQuery;
  private int solrPagingBufferSize;

  @Override
  public int read(){
    if (linesRead > maxRecords) {
     System.out.println("warcExpport max reached");
      return -1;
    }
    if (inputBuffer.size() == 0) {
      loadMore();
    }

    if (inputBuffer.isEmpty()) {
      System.out.println("warc export empty reached");
      return -1;
    }

    // Get first element of the List
    byte[] bytes = inputBuffer.get(0);
    System.out.println("new bytes buffer:"+bytes.length);
    // Get the byte corresponding to the index and post increment the current
    // index
    byte result = bytes[index++];
    if (index >= bytes.length) {
      // It was the last index of the byte array so we remove it from the list
      // and reset the current index
      inputBuffer.remove(0);
      index = 0;
      linesRead = linesRead + solrPagingBufferSize;
      
    }
    
    return 0xff & result; //We are not in ascii anymore.
  }

  public StreamingSolrWarcExportBufferedInputStream(SolrStreamingWarcExportClient solrClient, String query, String filterQuery,
      int solrPagingBufferSize,  int maxRecords) {
    this.solrClient = solrClient;
    this.maxRecords = maxRecords;
    this.query = query;
    this.filterQuery = filterQuery;
    this.solrPagingBufferSize = solrPagingBufferSize;
  }

  private void loadMore() {
    try {
      System.out.println("load more");
       SolrDocumentList docs = solrClient.exportWarcBuffered(query, filterQuery, solrPagingBufferSize);    
       inputBuffer = new ArrayList<byte[]>();
       for  (SolrDocument doc : docs){      
         String source_file_path = (String) doc.getFieldValue("source_file_path");
         long offset = (Long) doc.getFieldValue("source_file_offset");
         if (source_file_path.toLowerCase().endsWith(".arc")  || source_file_path.toLowerCase().endsWith(".arc.gz")){
           System.out.println("skipping arc record:"+source_file_path);
           continue;
         }
         
         ArcEntry warcEntry = WarcParser.getWarcEntry(source_file_path,offset);
         String warc2HeaderEncoding = warcEntry.getContentEncoding();
         Charset charset = Charset.forName(WarcParser.WARC_HEADER_ENCODING); //Default if none define or illegal charset
    
         if (warc2HeaderEncoding != null){
           try{
               charset = Charset.forName(warc2HeaderEncoding);
           }
           catch (Exception e){
             if (!"binary".equals(warc2HeaderEncoding)){ //This is not a real encoding
                System.out.println("unknown charset:"+warc2HeaderEncoding);
              }
             }
         }         
                   
         inputBuffer.add(warcEntry.getHeader().getBytes(charset));                           
         inputBuffer.add(warcEntry.getBinary());         
         inputBuffer.add("\r\n\r\n".getBytes(WarcParser.WARC_HEADER_ENCODING) );              
                
       }      
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
