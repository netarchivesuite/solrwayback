package dk.kb.netarchivesuite.solrwayback.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.export.GenerateCSV;

import java.util.Iterator;

public class SolrStreamingExportClient  implements SolrStreamingLineBasedExportClientInterface{

  public static final String BRIEF_FL = "title,url,source_file_path,crawl_date,wayback_date";
  

  public static final String FULL_FL =
          "title, host, public_suffix, crawl_year, content_type, content_language, source_file_path,url," +
          "source_file_path,crawl_date,wayback_date";
  
  public static final int DEFAULT_PAGE_SIZE = 1000; //TODO change is content is extracted

  private final Logger log = LoggerFactory.getLogger(SolrStreamingExportClient.class);
  private final Iterator<SolrDocument> solrDocs;
  private final String solrFields;
  private final String[] solrFieldsArray;
  private final String csvFields;
  private final String[] csvFieldsArray;
  private final String query;
  private final String[] filters;
  private boolean first = true;

  public SolrStreamingExportClient(
          SolrClient solrClient, int pageSize, String solrFields, String csvFields,
          boolean expandResources, boolean avoidDuplicates, String query, String... filterQueries) {
    if (solrFields == null || solrFields.isEmpty() || csvFields == null || csvFields.isEmpty()) {
      throw new IllegalArgumentException("fields argument was empty, but must be specified");
    }
    this.solrFieldsArray = solrFields.split(", *");
    this.csvFieldsArray = csvFields.split(", *");
    // TODO: Handle closing in case of exceptions
    //solrDocs = SolrGenericStreaming.iterate(
    solrDocs = SolrStreamShard.iterateStrategy(
            SRequest.builder().
                    solrClient(solrClient).
                    query(query).filterQueries(filterQueries).
                    fields(solrFieldsArray).
                    pageSize(pageSize).
                    expandResources(expandResources).
                    ensureUnique(avoidDuplicates));
    this.solrFields = solrFields;
    this.csvFields = csvFields;
    this.query = query;
    this.filters = filterQueries;
    //solrServer.setRequestWriter(new BinaryRequestWriter()); 
  }

  public static SolrStreamingExportClient createCvsExporter(SolrClient solrClient, String query, String fields, String... filterQueries) {
    // TODO: It does not make sense to have 50000 as page size both for brief and full export

      //Remove spaces in fieldlist
      fields = fields.replace(" ", "");
      
    return new SolrStreamingExportClient(solrClient, DEFAULT_PAGE_SIZE, fields ,fields, false, false, query, filterQueries);
  }

  @Override
  public String next() throws Exception {
    StringBuffer export = new StringBuffer();

    if (first) {
      GenerateCSV.addHeadlineFields(export, csvFieldsArray);
      first = false;
    }

    if (solrDocs.hasNext()) {
      GenerateCSV.generateLine(export, solrDocs.next(), csvFieldsArray);
    }

    return export.toString();
  }
  
  @Override
  public int getPageSize() {
    return DEFAULT_PAGE_SIZE;
  }
}
