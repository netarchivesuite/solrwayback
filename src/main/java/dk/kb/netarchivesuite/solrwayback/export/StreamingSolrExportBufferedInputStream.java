package dk.kb.netarchivesuite.solrwayback.export;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamingExportClient;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamingLineBasedExportClientInterface;

public class StreamingSolrExportBufferedInputStream extends InputStream {

  private int index;
  private List<byte[]> inputBuffer = new ArrayList<>();
  private final long maxLines;
  private long linesRead;
  private  SolrStreamingLineBasedExportClientInterface solrClient;
  private final int solrPagingBufferSize;

  @Override
  public int read() throws IOException {

    if (linesRead > maxLines) {
      return -1;
    }
    if (inputBuffer.size() == 0) {
      loadMore();
    }

    if (inputBuffer.isEmpty()) {
      //System.out.println("empty");
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
      // TODO: Replace this hack with a proper line counter (check for line break or something like that)
      linesRead = linesRead + solrPagingBufferSize;
    }
    return result;
  }

  public StreamingSolrExportBufferedInputStream(SolrStreamingLineBasedExportClientInterface solrClient, long maxLines) {
    this.solrClient = solrClient;
    this.maxLines = maxLines;
    this.solrPagingBufferSize = solrClient.getPageSize();
  }

  private void loadMore() {
    try {
      String lines = solrClient.next();

      inputBuffer = new ArrayList<byte[]>();
      if (lines != null && !lines.isEmpty()) {
        inputBuffer.add(lines.getBytes("utf-8"));
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
