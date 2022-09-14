package dk.kb.netarchivesuite.solrwayback.solr;

import dk.kb.netarchivesuite.solrwayback.parsers.ArcParserFileResolver;
import dk.kb.netarchivesuite.solrwayback.parsers.HtmlParserUrlRewriter;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.apache.commons.lang3.StringUtils.join;

/**
 * CursorMark-based chunking search client allowing for arbitrary sized result sets.
 */
public class SolrGenericStreaming implements Iterable<SolrDocument> {

  private static final int DEFAULT_PAGESIZE = 1000; // Due to Solr boolean queries limit
  public static final String DEFAULT_SORT = "score desc, id asc";

  private static SolrClient solrServer;
  private final Logger log = LoggerFactory.getLogger(SolrGenericStreaming.class);
  private final boolean expandResources;
  private final int pageSize;

  private final SolrQuery solrQuery;
  private final Set<String> encountered;
  private String cursorMark = CursorMarkParams.CURSOR_MARK_START;
  private long duplicatesRemoved = 0;
  private List<String> fields;
  private SolrDocumentList undelivered = null; // Leftover form previous call to keep deliveries below pageSize
  private boolean hasFinished = false;

  private String streamDeduplicateField = null; // If set, timeProximity is used
  private Object lastStreamDeduplicateValue = null; // Used with timeProximity

  // TODO: Make graph traversal of JavaScript & CSS-includes with expandResources

  /**
   * Streams documents that are closest in time to crawl_time, removing duplicates.
   * If expandResources is enabled, the extra resources are not deduplicated
   *
   * @param solrServerUrl complete address for a Solr server.
   * @param fields        comma separated fields to export.
   * @param expandResources     if true, embedded resources for HTML pages are extracted and added to the delivered
   *                            lists of Solr Documents.
   *                            Note: Indirect references (through JavaScript & CSS) are not followed.
   *                            Note: Duplicate removal does not work with expandResources.
   * @param idealTime           The time that the resources should be closest to, stated as a Solr timestamp
   *                            {@code YYYY-MM-DDTHH:mm:SSZ}. Also supports {@code oldest} and {@code newest} as values.
   * @param deduplicateField    The field to use for de-duplication. This is typically {@code url}.
   * @param query         standard Solr query.
   * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
   *                      If multiple filters are to be used, consider collapsing them into one:
   *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
   */
  // TODO: When https://github.com/ukwa/webarchive-discovery/issues/214 gets implemented it should be possible to use Last-Modied/Date from HTTP headers instead of crawl_date
  public static SolrGenericStreaming timeProximity(String solrServerUrl, List<String> fields, boolean expandResources,
                                                   String idealTime, String deduplicateField,
                                                   String query, String... filterQueries) {
    // Extra steps for timeProximity:
    // 1) Construct sort "<deduplicateField> asc, abs(sub(ms(2014-01-03T11:56:58Z), crawl_date)) asc")
    // 2) Keep track of latest received deduplicateField. When the value changes, accept the document and
    //    remember the new value for future deduplication
    String it = idealTime;
    if ("newest".equals(idealTime)) {
      it = "9999-12-31T23:59:59Z";
    } else if ("oldest".equals(idealTime)) {
      it = "0001-01-01T00:00:01Z";
    } else if (!ISO_TIME.matcher(idealTime).matches()) {
      throw new IllegalArgumentException(
              "The idealTime '" + idealTime + "' does not match 'oldest', 'newest' or 'YYYY-MM-DDTHH:mm:SSZ");
    }
    String sort = String.format(Locale.ROOT, "%s asc, abs(sub(ms(%s), crawl_date)) asc", deduplicateField, it);
    SolrQuery totalQuery = buildBaseQuery(DEFAULT_PAGESIZE, fields, expandResources, query, filterQueries, sort);
    SolrGenericStreaming stream = new SolrGenericStreaming(solrServerUrl, totalQuery, expandResources, false);
    // TODO: Validate that deduplicateField does not need to be part of fields
    stream.streamDeduplicateField = deduplicateField;
    return stream;
  }
  private static final Pattern ISO_TIME =Pattern.compile("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[012][0-9]:[0-5][0-9]Z");

  /**
   * Default page size 1000, expandResources=false and avoidDuplicates=false.
   * @param solrServerUrl complete address for a Solr server.
   * @param fields        the fields to export.
   * @param query         standard Solr query.
   * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
   *                      If multiple filters are to be used, consider collapsing them into one:
   *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
   */
  public SolrGenericStreaming(String solrServerUrl, List<String> fields, String query, String... filterQueries) {
    this(solrServerUrl, buildBaseQuery(DEFAULT_PAGESIZE, fields, false, query, filterQueries, DEFAULT_SORT),
         false, false);
  }

  /**
   * Default page size 1000, expandResources=false and avoidDuplicates=false.
   * @param solrServerUrl complete address for a Solr server.
   * @param fields        comma separated fields to export.
   * @param query         standard Solr query.
   * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
   *                      If multiple filters are to be used, consider collapsing them into one:
   *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
   */
  public SolrGenericStreaming(String solrServerUrl, String fields, String query, String... filterQueries) {
    this(solrServerUrl, buildBaseQuery(DEFAULT_PAGESIZE, Arrays.asList(fields.split(", *")), false,
                                       query, filterQueries, DEFAULT_SORT), false, false);
  }

  /**
   * @param solrServerUrl       complete address for a Solr server.
   * @param pageSize            paging size. 1000-100,000 depending on fields.
   * @param fields              the fields to export.
   * @param expandResources     if true, embedded resources for HTML pages are extracted and added to the delivered
   *                            lists of Solr Documents.
   *                            Note: Indirect references (through JavaScript & CSS) are not followed.
   * @param avoidDuplicates     if true, duplicates are removed. Note that a HashSet is created to keep track of
   *                            encountered documents. For result sets above tens of millions, this may result in
   *                            out of memory.
   * @param query               standard Solr query.
   * @param filterQueries       optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
   *                            If multiple filters are to be used, consider collapsing them into one:
   *                            {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
   */
  public SolrGenericStreaming(
          String solrServerUrl, int pageSize, List<String> fields, boolean expandResources, boolean avoidDuplicates,
          String query, String... filterQueries) {
    this(solrServerUrl, buildBaseQuery(pageSize, fields, expandResources, query, filterQueries, DEFAULT_SORT),
         expandResources, avoidDuplicates);
  }

  /**
   * Advanced version where the user provides the SolrQuery object. Not recommended for casual use.
   * The cursormark is set to the start value if it not already defined in the solrQuery.
   * The rows is set to {@link #DEFAULT_PAGESIZE} if it is not already defined in the solrQuery.
   *
   * If avoidDuplicates is true, the fields "source_file_path" and "source_file_offset" must be requested by fl.
   * @param solrServerUrl       complete address for a Solr server.
   * @param solrQuery           a Solr query object ready for use.
   * @param expandResources     if true, embedded resources for HTML pages are extracted and added to the delivered
   *                            lists of Solr Documents.
   *                            Note: Indirect references (through JavaScript & CSS) are not followed.
   * @param avoidDuplicates     if true, duplicates are removed. Note that a HashSet is created to keep track of
   *                            encountered documents. For result sets above tens of millions, this may result in
   *                            out of memory.
   */
  public SolrGenericStreaming(
          String solrServerUrl, SolrQuery solrQuery, boolean expandResources, boolean avoidDuplicates) {
    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, solrQuery.get(
            CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START));
    solrQuery.set(CommonParams.ROWS, solrQuery.get(CommonParams.ROWS, Integer.toString(DEFAULT_PAGESIZE)));
    solrServer =  new HttpSolrClient.Builder(solrServerUrl).build();
    this.solrQuery = solrQuery;
    this.expandResources = expandResources;
    this.encountered = avoidDuplicates ? new HashSet<>() : null;
    this.fields = Arrays.asList(solrQuery.getFields().split(","));
    this.pageSize = solrQuery.getRows();
  }

  /**
   * Stream the Solr response one document at a time.
   * @return a stream of SolrDocuments.
   */
  public Stream<SolrDocument> stream() {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), 0), false);
  }

  /**
   * @return an iterator of SolrDocuments.
   */
  @Override
  public Iterator<SolrDocument> iterator() {
    return new Iterator<SolrDocument>() {
      SolrDocumentList list = null;
      int index = 0;

      @Override
      public boolean hasNext() {
        // Request new list if it is depleted and there are more document lists available
        if ((list == null || index == list.size()) && !hasFinished()) {
          try {
            list = nextDocuments();
            index = 0;
          } catch (Exception e) {
            throw new RuntimeException("Exception requesting next batch", e);
          }
        }
        // Remove list if it is depleted
        if (list != null && index == list.size()) {
          list = null;
        }
        return list != null;
      }

      @Override
      public SolrDocument next() {
        if (!hasNext()) {
          throw new NoSuchElementException("No more elements");
        }
        return list.get(index++);
      }
    };
  }

  private static SolrQuery buildBaseQuery(
          int pageSize, List<String> fields, boolean expandResources, String query, String[] filterQueries,
          String sort) {
    List<String> finalFields = getFields(fields, expandResources);
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.set("stats", "false");
    solrQuery.add("fl", join(finalFields, ","));
    solrQuery.add("sort", sort);
    solrQuery.setRows(pageSize);
    solrQuery.setQuery(query);
    if (filterQueries != null) {
      for (String filter: filterQueries) {
        if (filter != null) {
          solrQuery.addFilterQuery(filter);
        }
      }
    }
    
    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START);
    return solrQuery;
  }

  public SolrDocumentList nextDocuments() throws Exception {
    if (hasFinished) {
      return null;
    }
    if (undelivered != null && undelivered.size() > pageSize) {
      return ensurePagesize(undelivered);
    }
    solrQuery.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark );
    QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
    cursorMark = rsp.getNextCursorMark();
    SolrDocumentList documents = rsp.getResults();

    if (streamDeduplicateField != null) {
      documents = streamDeduplicate(documents);
    }

    if (expandResources) {
      documents = expandResources(documents);
    }
    if (encountered != null) {
      documents = removeDuplicates(documents);
    }
    if (undelivered != null) {
      NetarchiveSolrClient.mergeInto(undelivered, documents);
      documents = undelivered;
      undelivered = null;
    }
    if (documents.isEmpty()) {
      hasFinished = false;
    }
    //log.info("next cursormark warc export:"+cursorMark );
    return ensurePagesize(documents);
  }

  /**
   * If there are more than {@link #pageSize} documents, exactly pageSize documents are returned and the rest are
   * stored af {@link #undelivered}. else the input documents are returned.
   */
  private SolrDocumentList ensurePagesize(SolrDocumentList documents) {
    if (documents == null || documents.size() < pageSize) {
      return documents;
    }

    SolrDocumentList newDocs = new SolrDocumentList();
    newDocs.addAll(documents.subList(0, pageSize));

    undelivered = new SolrDocumentList();
    undelivered.addAll(documents.subList(pageSize, documents.size()));

    return newDocs;
  }

  private SolrDocumentList expandResources(SolrDocumentList documents) {
    int initialSize = documents.size();
    for (int i = 0 ; i < initialSize ; i++) {
      if ("html".equals(documents.get(i).getFieldValue("content_type_norm"))) {
        documents.addAll(getHTMLResources(documents.get(i)));
      }
    }
    return documents;
  }

  private SolrDocumentList getHTMLResources(SolrDocument html) {
    try {
      String sourceFile = html.getFieldValue("source_file_path").toString();
      Long offset = Long.parseLong(html.getFieldValue("source_file_offset").toString());
      ArcEntry arc= ArcParserFileResolver.getArcEntry(sourceFile, offset);
      HashSet<String> resources = HtmlParserUrlRewriter.getResourceLinksForHtmlFromArc(arc);
      
      return NetarchiveSolrClient.getInstance().findNearestDocuments(resources, arc.getCrawlDate(), join(fields, ","));
    } catch (Exception e) {
      e.printStackTrace();
      log.warn("Unable to get resources for Solrdocument " + html);
      return new SolrDocumentList();
    }
  }

  private static List<String> getFields(List<String> fields, boolean expandResources) {
    List<String> finalFields = new ArrayList<>(fields == null || fields.isEmpty() ?
            Arrays.asList("source_file_path", "source_file_offset") :
            fields); // ensure mutability
    if (expandResources && !finalFields.contains("content_type_norm")) {
      finalFields.add("content_type_norm");
    }
    return finalFields;
  }

  public long getDuplicatesRemoveCount() {
    return duplicatesRemoved;
  }

  public boolean hasFinished() {
    return hasFinished;
  }

  private SolrDocumentList removeDuplicates(SolrDocumentList documents) {
    for (int i = documents.size()-1 ; i >= 0 ; i--) {
      //if (encountered.add(getID(documents.get(i)))) { // Why does this not work?
      if (encountered.contains(getID(documents.get(i)))) {
        documents.remove(i);
        duplicatesRemoved++;
      } else {
        encountered.add(getID(documents.get(i)));
      }
    }
    return documents;
  }

  /**
   * Streaming deduplication where the incoming documents are expected to be in order.
   */
  private SolrDocumentList streamDeduplicate(SolrDocumentList documents) {
    List<SolrDocument> unique = new ArrayList<>(documents.size());
    for (SolrDocument doc: documents) {
      if (lastStreamDeduplicateValue == null ||
          !lastStreamDeduplicateValue.equals(doc.getFieldValue(streamDeduplicateField))) {
          lastStreamDeduplicateValue = doc.getFieldValue(streamDeduplicateField);
          unique.add(doc);
      } else {
        duplicatesRemoved++;
      }
    }
    documents.clear();
    documents.addAll(unique);
    return documents;
  }

  private String getID(SolrDocument solrDocument) {
    return solrDocument.getFieldValue("source_file_path") + "@" +
           solrDocument.getFieldValue("source_file_offset");
  }
}
