package dk.kb.netarchivesuite.solrwayback.export;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dk.kb.netarchivesuite.solrwayback.util.DelayedInputStream;
import dk.kb.netarchivesuite.solrwayback.util.NamedConsumer;
import dk.kb.netarchivesuite.solrwayback.util.StatusInputStream;
import dk.kb.netarchivesuite.solrwayback.util.StreamBridge;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.parsers.ArcHeader2WarcHeader;
import dk.kb.netarchivesuite.solrwayback.parsers.ArcParserFileResolver;
import dk.kb.netarchivesuite.solrwayback.parsers.WarcParser;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

public class StreamingSolrWarcExportBufferedInputStream extends InputStream{

  private static final Logger log = LoggerFactory.getLogger(StreamingSolrWarcExportBufferedInputStream.class);

  //private final SolrGenericStreaming solrClient;
  private final Iterator<SolrDocument> solrDocs;
  private final long maxRecords;
  private final boolean gzip;
  private final List<InputStream> entryStreams = new ArrayList<>(); // Ideally a FIFO buffer, but not worth the hassle
  private long docsWarcRead;
  private long docsArcRead;

  private int heapCache = 10*1024*1024; // 10MB TODO: Make this an option

  /**
   * Create a stream with WARC-content from the records referenced by the solrClient.
   * The parts of the stream is lazy loaded and has no practical limit on sizes.
   * @param solrDocs   the Solr documents specifying the records to stream. The documents MUST include the fields
   *                   {@code source_file_path} and {@code source_file_offset}.
   * @param maxRecords the maximum number of records to deliver.
   * @param gzip if true, the WARC-records will be gzipped. If false, they will be delivered as-is.
   */
  public StreamingSolrWarcExportBufferedInputStream(Iterator<SolrDocument> solrDocs, long maxRecords, boolean gzip) {
    this.solrDocs = solrDocs;
    this.maxRecords = maxRecords;
    this.gzip = gzip;
  }

  /**
   * Create a stream with WARC-content from the records in the solrDocs Stream.
   * The parts of the stream is lazy loaded and has no practical limit on sizes.
   * @param solrDocs   the Solr documents specifying the records to stream. The documents MUST include the fields
   *                   {@code source_file_path} and {@code source_file_offset}.
   * @param maxRecords the maximum number of records to deliver.
   * @param gzip if true, the WARC-records will be gzipped. If false, they will be delivered as-is.
   */
  public StreamingSolrWarcExportBufferedInputStream(Stream<SolrDocument> solrDocs, long maxRecords, boolean gzip) {
    this.solrDocs = solrDocs.iterator();
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

  private long readFromCurrent = 0;
  private long processedStreams = 0;

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int totalRead = 0;
    while (len > 0) {
      // Do we have streams available?
      if (entryStreams.isEmpty()) {
        // No streams. Try to load more
        loadMore();
        if (entryStreams.isEmpty()) {
          // Still no streams. Stop processing
          log.info("warcExport buffer empty");
          log.info("Warcs read:"+docsWarcRead +" arcs read:"+docsArcRead);
          return totalRead == 0 ? -1 : totalRead; // -1 signals EOS
        }
      }

      // There are streams. Read content from the first one
      int read = entryStreams.get(0).read(b, off, len);
      if (read == -1) { // The entryStream is empty. Remove it and go to the next
        /* Too spammy
          log.debug(String.format(Locale.ENGLISH, "Emptied entryStream #%d (contained %d bytes). " +
                                                "Switching to next one or loadMore() if the buffer is empty",
                                processedStreams+1, readFromCurrent));
        */
        try {
          entryStreams.get(0).close();
          processedStreams++;
        } catch (Exception e) {
            log.debug("boo", e);
          log.warn("Error closing entryStream", e);
        }
        entryStreams.remove(0);
        readFromCurrent = 0;
        continue;
      }

      // We got some content. Update counters and loop to try and fill the input buffer fully
      totalRead += read;
      readFromCurrent += read;
      off += read;
      len -= read;
    }

    if (totalRead == 0) {
      log.debug("No more content in last remaining stream. Closing export. " +
                "WARCS read: " + docsWarcRead + " ARCs read: " + docsArcRead);
    }
    return totalRead == 0 ? -1 : totalRead; // -1 signals EOS
  }
  AtomicInteger c = new AtomicInteger(0);
  AtomicInteger cLazy = new AtomicInteger(0);

  /**
   * Resolve more content streams and add them to {@link #entryStreams}.
   */
  private void loadMore() {
    try {
      if (docsWarcRead+docsArcRead > maxRecords) { //Stop loading more
        log.info("loadMore(): Max documents reached (" + maxRecords + "). Stopping loading of more documents");
        return;
      }
      while (solrDocs.hasNext() && entryStreams.isEmpty()) {
        // We stream eventhough there is only 1 element in preparation of batching at a later time
        addRecordsToStream(Stream.of(solrDocs.next()));
      }
      // log.debug("Got " + (docs == null ? 0 : docs.size()) + " Solr documents");
      if (entryStreams.isEmpty()) {
        log.info("loadMore(): No more documents available after " + (docsWarcRead+docsArcRead) + " docs read");
      }
    } catch (Exception e) {
      log.error("Unhandled exception in loadMore", e);
    }
  }

  /**
   * Given a list of Solr records with WARC paths and offsets, derive WARC entry representations from these and
   * send them to {@link #addRecordsToStream(List)}.
   * @param docs Solr documents with {@code source_file_path} and {@code source_file_offset} fields.
   */
  private void addRecordsToStream(Stream<SolrDocument> docs) throws IOException {
    AtomicInteger docCount = new AtomicInteger(0);
    List<EntryAndHeaders> entriesAndHeaders = docs
            .peek(doc -> docCount.incrementAndGet())
            .map(this::docToEntry)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (entriesAndHeaders.isEmpty()) {
      log.info("addRecordsToStream: No WARC records derived from " + docCount.get() + " Solr documents");
      return;
    }

    if (gzip) {
      addGzipRecordsToStream(entriesAndHeaders);
    } else {
      addRecordsToStream(entriesAndHeaders);
    }
  }

  private long warcsResolveAttempt = 0;

  /**
   * Resolve a WARC entrys from the given Solr doc.
   * @param doc a Solr document with {@code source_file_path} and {@code source_file_offset} fields.
   * @return a representation of the corresponding WARC record or null if it is unresolvable.
   */
  private EntryAndHeaders docToEntry(SolrDocument doc) {
    warcsResolveAttempt++;
    String source_file_path = (String) doc.getFieldValue("source_file_path");
    long offset = (Long) doc.getFieldValue("source_file_offset");
    EntryAndHeaders singleEntry;
    try {
      singleEntry = getWARCEntryAndHeaderStream(source_file_path, offset);
    } catch (Exception e) {
      log.warn(String.format(Locale.ENGLISH, "Exception resolving (W)ARC entry representation #%d for %s#%d. Skipping entry",
                             warcsResolveAttempt + 1, source_file_path, offset));
      return null;
    }
    if (singleEntry == null) {
      log.warn(String.format(Locale.ENGLISH, "Unable to resolve (W)ARC entry representation #%d for %s#%d",
                             warcsResolveAttempt + 1, source_file_path, offset));
    }
    return singleEntry;
  }

  /**
   * Given a list of WARC entry representations, construct InputStreams for the WARC content for these and add
   * them to {@link #entryStreams}. If the content for a WARC entry if faulty, it is repaired (the WARC header
   * {@code Content-Length} is adjusted). If the content cannot be resolved at all, an error is logged and the entry is
   * skipped.
   * @param entriesAndHeaders a list of WARC entry representations.
   */
  private void addRecordsToStream(List<EntryAndHeaders> entriesAndHeaders) {
    entriesAndHeaders.forEach(
            entryAndHeader -> entryStreams.add(
                    new DelayedInputStream(() -> getWARCEntryStream(entryAndHeader))));
  }

  /**
   * Given a list of WARC entry representations, construct gzipped InputStreams for the WARC content for these and add
   * them to {@link #entryStreams}. If the content for a WARC entry if faulty, it is repaired (the WARC header
   * {@code Content-Length} is adjusted). If the content cannot be resolved at all, an error is logged and the entry is
   * skipped.
   * @param entriesAndHeaders a list of WARC entry representations.
   * @throws IOException if the overall processing failed. This should not happen under normal circumstances.
   */
  private void addGzipRecordsToStream(List<EntryAndHeaders> entriesAndHeaders) throws IOException {
    List<Consumer<OutputStream>> providers = new ArrayList<>(entriesAndHeaders.size());
    for (EntryAndHeaders entryAndHeader: entriesAndHeaders) {
      ArcEntry entry = entryAndHeader.entry;
      try {
        providers.add(new NamedConsumer<>( // Wrapping in NamedConsumer for logging and debugging
                StreamBridge.gzip(out -> { // Add the lambda to the list for later activation
                    cLazy.incrementAndGet();
                    try {
                        IOUtils.copy(getWARCEntryStream(entryAndHeader), out);
                    } catch (Exception e) {
                        log.warn(String.format(
                                Locale.ENGLISH, "Exception during copying of bytes from export lambda #%d " +
                                        "with payload size %d bytes for URL '%s'",
                                cLazy.get(), entry.getBinaryArraySize(), entry.getUrl()), e);
                    }
                }), "url='" + entry.getUrl()));
      } catch (Exception e) {
        log.error(String.format(
                Locale.ENGLISH,
                "Exception getting delayed stream for export lambda #%d with payload size %d bytes for URL '%s'",
                c.incrementAndGet(), entry.getBinaryArraySize(), entry.getUrl()), e);
      }
    }

    // All the providers are added in a single call, so that they will all be processed by the same thread.
    // If they are added one at a time, they will require a thread for each call.
    //log.debug("loadMore() adding " + providers.size() + " lazy resolved records");
    entryStreams.add(StreamBridge.outputToInput(providers));
  }

  /**
   * Given a WARC entry representation, construct an InputStream for the WARC content.
   * If the payload for the WARC entry if faulty, the WARC header {@code Content-Length} is adjusted accordingly.
   * If the headers cannot be resolved, an exception is thrown.
   * @param entryAndHeaders a WARC entry representation.
   * @throws RuntimeException if the headers of the WARC entry could be resolved or a similar show-stopping problem
   * were encountered.
   */
  private InputStream getWARCEntryStream(EntryAndHeaders entryAndHeaders) {
    final String id = entryAndHeaders.entry.getArcSource() + "#" + entryAndHeaders.entry.getOffset();
    try {
      // Retrieve the payload to local cache (heap or storage, depending on size)
      StatusInputStream payload = entryAndHeaders.entry.getBinaryArraySize() > 0 ?
              StreamBridge.guaranteedStream(entryAndHeaders.entry.getBinaryRaw(), heapCache) :
              StreamBridge.guaranteedStream(new ByteArrayInputStream(new byte[0]), heapCache);
      if (payload.getStatus() == StatusInputStream.STATUS.exception) {
        log.warn("getDelayedStream: Exception encountered while caching payload for '{}'. Delivering partial content",
                id);
      }

      // Resolve the headers
      StatusInputStream headers = payload.size() != entryAndHeaders.entry.getBinaryArraySize() ?
              // The stated payload size does not fit the real size (probably truncated WARC), so adjust WARC headers
              getAndAdjustPayloadLength(entryAndHeaders.headers, heapCache, payload.size(), id) :
              // Everything seems to be in order
              StreamBridge.guaranteedStream(entryAndHeaders.headers, heapCache);
      if (headers.getStatus() != StatusInputStream.STATUS.ok) {
        close(payload, "payload for " + id);
        throw new IOException("Unable to deliver headers for '" + id + "'", headers.getException());
      }

      InputStream trailer = new ByteArrayInputStream("\r\n\r\n".getBytes(WarcParser.WARC_HEADER_ENCODING));

      //Usefull but too spammy
      //log.debug(String.format(Locale.ENGLISH, "Delivering delayed stream for '%s' with a total of %d bytes", id, headers.size() + payload.size() + 4));
      return StreamBridge.concat(headers, payload, trailer);
    } catch (Exception e) {
      throw new RuntimeException("Exception lazily constructing input streams for '" + id + "'", e);
    }
  }

  // Close while logging exceptions
  private void close(StatusInputStream payload, String id) {
    try {
      payload.close();
    } catch (Exception e) {
      log.warn("Exception closing stream '" + id + "'", e);
    }
  }

  /**
   * Return a stream with the warc-headers where the WARC {@code Content-Length} has been corrected.
   * @param headers a WARC entry representation. Headers will be extracted, adjusted and returned.
   * @param heapCache the maximum amount of bytes to cache on the heap.
   * @param payloadLength the size of the payload.
   * @param id an identifier for the resource. Used for error messages.
   * @return an Inputstream with the adjusted WARC header {@code Content-Length}.
   */
  private StatusInputStream getAndAdjustPayloadLength(
          InputStream headers, int heapCache, long payloadLength, String id) throws IOException {
    // Ensure the header-stream is fully readable and if not, return what was read
    StatusInputStream raw = StreamBridge.guaranteedStream(headers, heapCache);
    if (raw.getStatus() != StatusInputStream.STATUS.ok) {
      log.warn("getAndAdjustPayloadLength: Unable to get headers for '" + id + "'");
      return raw;
    }

    // Load the headers
    ByteArrayOutputStream fullHeaders = new ByteArrayOutputStream((int) raw.size());
    IOUtils.copy(raw, fullHeaders);
    byte[] fullHeaderBytes = fullHeaders.toByteArray();
    int httpBytes = 0;

    // Determine HTTP-headers-length
    boolean countBytes = false;
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new ByteArrayInputStream(fullHeaderBytes), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (countBytes) { // Inside the HTTP header part, so count every line
          httpBytes += line.length() + 2; // +2 because of \r\n
          continue;
        }
        countBytes = line.isEmpty(); // Switch from WARC to HTTP-headers on first empty line
      }
    }

    long totalNonWARCSize = httpBytes + payloadLength;

    // Replace Content-Length in the WARC-header with new length
    ByteArrayOutputStream bos = new ByteArrayOutputStream((int) (raw.size() + 5));
    try (OutputStreamWriter os = new OutputStreamWriter(bos, StandardCharsets.UTF_8);
         BufferedReader reader = new BufferedReader(
                 new InputStreamReader(new ByteArrayInputStream(fullHeaderBytes), StandardCharsets.UTF_8))) {
      String line;
      boolean lengthReplaced = false;
      while ((line = reader.readLine()) != null) {
        if (lengthReplaced || !line.startsWith("Content-Length:")) {
          os.write(line);
          os.write("\r\n");
          continue;
        }
        String newContentLength = "Content-Length: " + totalNonWARCSize;
        if (!newContentLength.equals(line)) {
          log.debug(String.format(Locale.ENGLISH, "Replacing WARC header line '%s' with '%s' for '%s'",
                                  line, newContentLength, id));
        }
        os.write(newContentLength);
        os.write("\r\n");
        lengthReplaced = true; // No further replacement
      }
      raw.close();
      os.flush();
    }
    return StreamBridge.guaranteedStream(new ByteArrayInputStream(bos.toByteArray()), heapCache);
  }

  /**
   * Resolve parsed (W)ARC entries as well as an explicit header stream.
   * @param source_file_path a WARC file.
   * @param offset a WARC offset.
   * @return a pair consisting og (w)arcEntry and header stream.
   */
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

  /**
   * Simple pair of {@link ArcEntry} and a stream of the headers from the entry.
   */
  private static class EntryAndHeaders {
    public final ArcEntry entry;
    public final InputStream headers;

    public EntryAndHeaders(ArcEntry entry, InputStream headers) {
      this.entry = entry;
      this.headers = headers;
    }
  }

  /**
   * Non-failing close that ensures that all {@link #entryStreams} are closed.
   */
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
