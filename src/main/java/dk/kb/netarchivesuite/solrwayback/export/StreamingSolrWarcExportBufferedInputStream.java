package dk.kb.netarchivesuite.solrwayback.export;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreaming;
import dk.kb.netarchivesuite.solrwayback.util.StreamBridge;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.parsers.ArcHeader2WarcHeader;
import dk.kb.netarchivesuite.solrwayback.parsers.ArcParserFileResolver;
import dk.kb.netarchivesuite.solrwayback.parsers.WarcParser;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import sun.nio.ch.IOUtil;

public class StreamingSolrWarcExportBufferedInputStream extends InputStream{

  private static final Logger log = LoggerFactory.getLogger(StreamingSolrWarcExportBufferedInputStream.class);

  private final SolrGenericStreaming solrClient;
  private final int maxRecords;
  private final boolean gzip;
  private final List<InputStream> entryStreams = new ArrayList<>(); // Ideally a FIFO buffer, but not worth the hassle
  private int docsWarcRead;
  private int docsArcRead;

  /**
   * Create a stream with WARC-content from the records referenced by the solrClient.
   * The parts of the stream is lazy loaded and has no practical limit on sizes.
   * @param solrClient delivers Solr documents specifying the records to stream.
   * @param maxRecords the maximum number of records to deliver.
   * @param gzip if true, the WARC-records will be gzipped. If false, they will be delivered as-is.
   */
  public StreamingSolrWarcExportBufferedInputStream(SolrGenericStreaming solrClient, int maxRecords, boolean gzip) {
    this.solrClient = solrClient;
    this.maxRecords = maxRecords;
    this.gzip = gzip;
  }

  @Override
  public int read() throws IOException {
    // Single byte read is not optimized: Callers should use read(byte[] b, int off, int len) if speed is a factor
    int read = read(SINGLE_BYTE, 0, 1);
    return read == -1 ? -1 : 0xff & SINGLE_BYTE[0];
  }
  private final byte[] SINGLE_BYTE = new byte[1];

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int totalRead = 0;
    while (len > 0) {
      // Do we have any content?
      if (entryStreams.isEmpty()) {
        loadMore();
        if (entryStreams.isEmpty()) {
          log.info("warcExport buffer empty");
          log.info("Warcs read:"+docsWarcRead +" arcs read:"+docsArcRead);
          return totalRead == 0 ? -1 : totalRead; // -1 signals EOS
        }
      }

      // There is content. Read up to max requested
      int read = entryStreams.get(0).read(b, off, len);
      if (read == -1) { // The entryStream is empty. Remove it and go to the next
        try {
          entryStreams.get(0).close();
        } catch (Exception e) {
          log.warn("Error closing entryStream", e);
        }
        entryStreams.remove(0);
        continue;
      }

      // We got some content. Update counters and loop to try and fill the input buffer fully
      totalRead += read;
      off += read;
      len -= read;
    }
    return totalRead == 0 ? -1 : totalRead; // -1 signals EOS
  }

  private void loadMore() {
    try {
      if (docsWarcRead > maxRecords) { //Stop loading more
        log.info("Max documents reached. Stopping loading more documents");
        return;
      }

      SolrDocumentList docs = solrClient.nextDocuments();
      if (docs == null || docs.isEmpty()) {
        log.info("No more documents available");
        return;
      }

      for  (SolrDocument doc : docs){
        String source_file_path = (String) doc.getFieldValue("source_file_path");
        long offset = (Long) doc.getFieldValue("source_file_offset");

        EntryAndHeaders entryAndHeaders = getWARCEntryAndHeaderStream(source_file_path, offset);
        if (entryAndHeaders == null) {
          log.warn(String.format(Locale.ROOT, "Unable to resolve (W)ARC entry %s#%d for %s",
                                 source_file_path, offset, doc.getFieldValue("id")));
          continue;
        }

        if (gzip) {
          entryStreams.add(StreamBridge.outputToGzipInput(out -> {
            try {
              IOUtils.copy(entryAndHeaders.headers, out);
              if (entryAndHeaders.entry.getBinaryArraySize() > 0) {
                IOUtils.copy()
                entryStreams.add(warcEntry.getBinaryLazyLoad());
              }
              entryStreams.add(new ByteArrayInputStream("\r\n\r\n".getBytes(WarcParser.WARC_HEADER_ENCODING)) );
            } catch (IOException e) {
              throw new RuntimeException(
                      "IOException writing entry to gzip stream: " + entryAndHeaders.entry.getUrl(), e);
            }

          }))
        }
        //Do this for both arc/warc 
        if ( warcEntry.getBinary().length > 0){
          entryStreams.add(warcEntry.getBinaryLazyLoad());
        }
        entryStreams.add(new ByteArrayInputStream("\r\n\r\n".getBytes(WarcParser.WARC_HEADER_ENCODING)) );

      }      
    } catch (Exception e) {
      log.error("Unhandled exception in loadMore", e);
      e.printStackTrace();
    }

  }

  private EntryAndHeaders getWARCEntryAndHeaderStream(String source_file_path, long offset) {
    ArcEntry warcEntry;
    InputStream headers;

    // ARC
    if (source_file_path.toLowerCase().endsWith(".arc") || source_file_path.toLowerCase().endsWith(".arc.gz")){
      //log.info("skipping Arc record:"+source_file_path);
      try{
        warcEntry = ArcParserFileResolver.getArcEntry(source_file_path, offset);
      }
      catch(Exception e){ //This will only happen if warc file is not found etc. Should not happen for real.
        log.warn("Error loading arc:"+source_file_path,e);
        return null;
      }


      String warcHeader = ArcHeader2WarcHeader.arcHeader2WarcHeader(warcEntry);
      // The header is (normally) fairly small, so we hold it in memory
      try {
        headers = new ByteArrayInputStream(warcHeader.getBytes(WarcParser.WARC_HEADER_ENCODING));
      } catch (UnsupportedEncodingException e) {
        String message = String.format(Locale.ROOT, "UnsupportedEncodingException for fixed charset '%s' while adding " +
                                                    "headers for %s#%d. This should not happen",
                                       WarcParser.WARC_HEADER_ENCODING, source_file_path, offset);
        log.warn(message, e);
        throw new RuntimeException(message, e);
      }
      docsArcRead++;
    } else {
      try{
        warcEntry = ArcParserFileResolver.getArcEntry(source_file_path,offset);
      }
      catch(Exception e){ //This will only happen if warc file is not found etc. Should not happen for real.
        log.warn("Error loading warc:"+source_file_path,e);
        return null;
      }


      String warc2HeaderEncoding = warcEntry.getContentEncoding();
      Charset charset = Charset.forName(WarcParser.WARC_HEADER_ENCODING); //Default if none define or illegal charset

      if (warc2HeaderEncoding != null){
        try{
          charset = Charset.forName(warc2HeaderEncoding);
        }
        catch (Exception e){
          if (!"binary".equals(warc2HeaderEncoding)){ //This is not a real encoding
            log.warn("unknown charset:"+warc2HeaderEncoding);
          }
        }
      }

      // The header is (normally) fairly small, so we hold it in memory
      headers = new ByteArrayInputStream(warcEntry.getHeader().getBytes(charset));
      docsWarcRead++;
    }
    return new EntryAndHeaders(warcEntry, headers);
  }
  private static class EntryAndHeaders {
    public final ArcEntry entry;
    public final InputStream headers;

    public EntryAndHeaders(ArcEntry entry, InputStream headers) {
      this.entry = entry;
      this.headers = headers;
    }
  }

  /**
   * Resolve the given (W)ARC-file, extract the headers and add them to the entryStreams.
   * @param source_file_path the name of the (W)ARC-file.
   * @param offset           the offset for the record in the (W)ARC-file.
   * @return a representation of the (W)ARC file.
   */
  private ArcEntry addHeadersReturnEntry(String source_file_path, long offset) throws IOException  {
    ArcEntry warcEntry;

    // ARC
    if (source_file_path.toLowerCase().endsWith(".arc") || source_file_path.toLowerCase().endsWith(".arc.gz")){
      //log.info("skipping Arc record:"+source_file_path);
      try{
        warcEntry = ArcParserFileResolver.getArcEntry(source_file_path, offset);
      }
      catch(Exception e){ //This will only happen if warc file is not found etc. Should not happen for real.
        log.warn("Error loading arc:"+source_file_path,e);
        return null;
      }


      String warcHeader = ArcHeader2WarcHeader.arcHeader2WarcHeader(warcEntry);
      // The header is (normally) fairly small, so we hold it in memory
      try {
        entryStreams.add(new ByteArrayInputStream(warcHeader.getBytes(WarcParser.WARC_HEADER_ENCODING)));
      } catch (UnsupportedEncodingException e) {
        String message = String.format(Locale.ROOT, "UnsupportedEncodingException for fixed charset '%s' while adding " +
                                                    "headers for %s#%d. This should not happen",
                                       WarcParser.WARC_HEADER_ENCODING, source_file_path, offset);
        log.warn(message, e);
        throw new RuntimeException(message, e);
      }
      docsArcRead++;
    } else {
      try{
        warcEntry = ArcParserFileResolver.getArcEntry(source_file_path,offset);
      }
      catch(Exception e){ //This will only happen if warc file is not found etc. Should not happen for real.
        log.warn("Error loading warc:"+source_file_path,e);
        return null;
      }


      String warc2HeaderEncoding = warcEntry.getContentEncoding();
      Charset charset = Charset.forName(WarcParser.WARC_HEADER_ENCODING); //Default if none define or illegal charset

      if (warc2HeaderEncoding != null){
        try{
          charset = Charset.forName(warc2HeaderEncoding);
        }
        catch (Exception e){
          if (!"binary".equals(warc2HeaderEncoding)){ //This is not a real encoding
            log.warn("unknown charset:"+warc2HeaderEncoding);
          }
        }
      }

      // The header is (normally) fairly small, so we hold it in memory
      entryStreams.add(new ByteArrayInputStream(warcEntry.getHeader().getBytes(charset)));
      docsWarcRead++;
    }
    return warcEntry;
  }

  @Override
  public void close() {
    for (InputStream entryStream: entryStreams) {
      try {
        entryStream.close();
      } catch (Exception e) {
        log.error("Error closing cached entryStream during outer close()", e);
      }
    }
  }
}
