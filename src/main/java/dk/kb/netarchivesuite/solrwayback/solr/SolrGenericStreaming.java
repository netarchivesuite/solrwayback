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
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.commons.lang3.StringUtils.join;

/**
 * CursorMark-based chunking search client allowing for arbitrary sized result sets.
 */
public class SolrGenericStreaming {

  private static final int DEFAULT_PAGESIZE = 1000; // Due to Solr boolean queries limit
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

  // TODO: Make graph traversal of JavaScript & CSS-includes with expandResources

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
    this(solrServerUrl, buildBaseQuery(DEFAULT_PAGESIZE, fields, false, query, filterQueries),
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
                                       query, filterQueries), false, false);
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
    this(solrServerUrl, buildBaseQuery(pageSize, fields, expandResources, query, filterQueries),
         expandResources, avoidDuplicates);
  }

  /**
   * Advanced version where the user provides the Solrquery object. Not recommended for casual use. If the user insists,
   * note that the cursormark must be set to the start value. If avoidDuplicates is true, the fields "source_file_path"
   * and "source_file_offset" must be returned.
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
    solrServer =  new HttpSolrClient.Builder(solrServerUrl).build();
    this.solrQuery = solrQuery;
    this.expandResources = expandResources;
    this.encountered = avoidDuplicates ? new HashSet<String>() : null;
    this.fields = Arrays.asList(solrQuery.getFields().split(","));
    this.pageSize = solrQuery.getRows();
  }

  private static SolrQuery buildBaseQuery(
          int pageSize, List<String> fields, boolean expandResources, String query, String[] filterQueries) {
    List<String> finalFields = getFields(fields, expandResources);
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.set("facet", "false"); //very important. Must overwrite to false. Facets are very slow and expensive.
    solrQuery.set("stats", "false");
    solrQuery.add("fl", join(finalFields, ","));
    solrQuery.add("sort","score desc, id asc");
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

  private String getID(SolrDocument solrDocument) {
    return solrDocument.getFieldValue("source_file_path") + "@" +
           solrDocument.getFieldValue("source_file_offset");
  }
}
