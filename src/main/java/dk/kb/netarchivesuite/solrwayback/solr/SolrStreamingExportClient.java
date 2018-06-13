package dk.kb.netarchivesuite.solrwayback.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.export.GenerateCSV;

import java.util.Arrays;

public class SolrStreamingExportClient {

  public static final String BRIEF_FL = "title,url,source_file_path,crawl_date,wayback_date";
  public static final String BRIEF_CSV = "title,crawl_date,waybackurl";

  public static final String FULL_FL =
          "title, host, public_suffix, crawl_year, content_type, content_language, source_file_path,url," +
          "source_file_path,crawl_date,wayback_date";
  public static final String FULL_CSV =
          "title,host,public_suffix,crawl_year,content_type,content_language,crawl_date,url,wayback_date,waybackurl";

  public static final int DEFAULT_PAGE_SIZE = 50000;

  private final Logger log = LoggerFactory.getLogger(SolrStreamingExportClient.class);
  private final SolrGenericStreaming inner;
  private final String solrFields;
  private final String[] solrFieldsArray;
  private final String csvFields;
  private final String[] csvFieldsArray;
  private final String query;
  private final String[] filters;
  private boolean first = true;

  public SolrStreamingExportClient(
          String solrServerUrl, int pageSize, String solrFields, String csvFields,
          boolean expandResources, boolean avoidDuplicates, String query, String... filterQueries) {
    if (solrFields == null || solrFields.isEmpty() || csvFields == null || csvFields.isEmpty()) {
      throw new IllegalArgumentException("fields argument was empty, but must be specified");
    }
    this.solrFieldsArray = solrFields.split(", *");
    this.csvFieldsArray = csvFields.split(", *");
    inner = new SolrGenericStreaming(
            solrServerUrl, pageSize, Arrays.asList(solrFieldsArray), expandResources, avoidDuplicates,
            query, filterQueries);
    this.solrFields = solrFields;
    this.csvFields = csvFields;
    this.query = query;
    this.filters = filterQueries;
    //solrServer.setRequestWriter(new BinaryRequestWriter()); 
  }

  public static SolrStreamingExportClient createExporter(
          String solrServerUrl, boolean brief, String query, String... filterQueries) {
    // TODO: It does not make sense to have 50000 as page size both for brief and full export
    return new SolrStreamingExportClient(
            solrServerUrl, DEFAULT_PAGE_SIZE, brief ? BRIEF_FL : FULL_CSV, brief ? BRIEF_CSV : FULL_CSV,
            false, false, query, filterQueries);
  }

  public String next() throws Exception {
    if (inner.hasFinished()) {
      return "";
    }
    StringBuffer export = new StringBuffer();

    if (first) {
      GenerateCSV.generateFirstLineHeader(export);    //Add this to first cursor mark
      GenerateCSV.generateSecondLineHeader(export, query, filters == null ? null : Arrays.toString(filters));
      GenerateCSV.addHeadlineFields(export, csvFieldsArray);
      first = false;
    }

    SolrDocumentList docs = inner.nextDocuments();
    if (docs == null || docs.isEmpty()) {
      return export.toString();
    }

    for ( SolrDocument doc : docs){
     GenerateCSV.generateLine(export, doc, csvFieldsArray);
    }
    System.out.println("return size: " + export.length());
    return export.toString();
  }

}
