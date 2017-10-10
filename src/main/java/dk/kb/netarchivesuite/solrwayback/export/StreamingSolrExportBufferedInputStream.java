package dk.kb.netarchivesuite.solrwayback.export;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamingExportClient;

public class StreamingSolrExportBufferedInputStream extends InputStream {

  private int index;
  private List<byte[]> inputBuffer = new ArrayList<>();
  private int maxLines;
  private int linesRead;
  private SolrStreamingExportClient solrClient;
  private String query;
  private String filterQuery;
  private int solrPagingBufferSize;
  private boolean fullExport;

  @Override
  public int read() throws IOException {

    if (linesRead > maxLines) {
      return -1;
    }
    if (inputBuffer.size() == 0) {
      loadMore();
    }

    if (inputBuffer.isEmpty()) {
      System.out.println("empty");
      return -1;
    }

    // Get first element of the List
    byte[] bytes = inputBuffer.get(0);
    // Get the byte corresponding to the index and post increment the current
    // index
    byte result = bytes[index++];
    if (index >= bytes.length) {
      // It was the last index of the byte array so we remove it from the list
      // and reset the current index
      inputBuffer.remove(0);
      index = 0;
      linesRead = linesRead + solrPagingBufferSize;
      System.out.println(linesRead);
    }
    return result;
  }

  public StreamingSolrExportBufferedInputStream(SolrStreamingExportClient solrClient, String query, String filterQuery,
      int solrPagingBufferSize, boolean fullExport, int maxLines) {
    this.solrClient = solrClient;
    this.maxLines = maxLines;
    this.query = query;
    this.filterQuery = filterQuery;
    this.solrPagingBufferSize = solrPagingBufferSize;
    this.fullExport = fullExport;
  }

  private void loadMore() {
    try {
      String lines;
      if (fullExport) {
        lines = solrClient.exportFullBuffered(query, filterQuery, solrPagingBufferSize);
      } else {
        lines = solrClient.exportBriefBuffered(query, filterQuery, solrPagingBufferSize);
      }

      inputBuffer = new ArrayList<byte[]>();
      if (lines.length() > 0) {
        inputBuffer.add(lines.getBytes("utf-8"));
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
