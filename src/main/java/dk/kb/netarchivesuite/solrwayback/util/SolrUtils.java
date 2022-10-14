package dk.kb.netarchivesuite.solrwayback.util;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class SolrUtils {
    public static final Logger log = LoggerFactory.getLogger(SolrUtils.class);
    public static String NO_REVISIT_FILTER ="record_type:response OR record_type:arc OR record_type:resource";
    public static String indexDocFieldList = "id,score,title,url,url_norm,links_images,source_file_path,source_file,source_file_offset,domain,resourcename,content_type,content_type_full,content_type_norm,hash,type,crawl_date,content_encoding,exif_location,status_code,last_modified,redirect_to_norm";
    public static String indexDocFieldListShort = "url,url_norm,source_file_path,source_file,source_file_offset,crawl_date";

    /**
     * Normalizes a given list of urls and makes a Solr search string from the result.
     *
     * Notice only maximum of 50 urls will be searched for.
     * This method is only called for image- and video search, and we don't want too many hits from same site.
     * @param urls Urls to make the search string from.
     * @return Solr query string to search Solr for the given images.
     */
    public static String createQueryStringForUrls(List<String> urls) {
        if (urls.size() > 50) {
            urls = urls.subList(0, 50);
        }

        StringBuilder query = new StringBuilder();
        query.append("(");
        for (String url : urls) {
            try {
                String urlQueryString = createQueryStringForUrl(url);
                query.append(urlQueryString).append(" OR ");
            }
            catch (Exception e) {
                // This can happen since urls from HTML are extracted without any sanity-check by the warc-indexer.
                // Just ignore.
                log.info("Could not normalise url:" + url);
            }
        }
        query.append("url_norm:none)"); // Just close last OR

        return query.toString();
    }

    /**
     * Normalizes the given url and makes a Solr search string from the result.
     * @param url Url to make the search string from.
     * @return Solr query string.
     */
    public static String createQueryStringForUrl(String url) {
        String canonicalizedUrl = Normalisation.canonicaliseURL(url);
        return "url_norm:\"" + canonicalizedUrl + "\"";
    }

    /**
     * Make a URL to search SolrWayback for the given string.
     * @param searchString String to search for.
     * @return SolrWayback search URL for given string.
     */
    public static String createTwitterSearchURL(String searchString) {
        String searchParams = " AND type%3A\"Twitter Tweet\"";
        return PropertiesLoader.WAYBACK_BASEURL + "search?query=" + searchString + searchParams;
    }

    /**
     * Convert the given list of {@link SolrDocument}s to a list of {@link IndexDoc}s.
     * @param docs 0 or more SolrDocuments.
     * @return a list of {@link IndexDoc}s constructed from the given {@link SolrDocument}s.
     */
    public static ArrayList<IndexDoc> solrDocList2IndexDoc(SolrDocumentList docs) {
        return docs.stream().
                map(SolrUtils::solrDocument2IndexDoc).
                collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Convert the given {@link SolrDocument} to a {@link IndexDoc}.
     *
     * Note that {@link IndexDoc}s only holds a subset of all possible fields from {@link SolrDocument}.
     * @param doc a document from a Solr search.
     * @return an index document.
     */
    public static IndexDoc solrDocument2IndexDoc(SolrDocument doc) {
        IndexDoc indexDoc = new IndexDoc();
        indexDoc.setScore(Double.valueOf((float) doc.getFieldValue("score")));
        indexDoc.setId((String) doc.get("id"));
        indexDoc.setTitle((String) doc.get("title"));
        indexDoc.setSource_file_path((String) doc.get("source_file_path"));
        indexDoc.setResourceName((String) doc.get("resourcename"));
        indexDoc.setDomain((String) doc.get("domain"));
        indexDoc.setUrl((String) doc.get("url"));
        indexDoc.setUrl_norm((String) doc.get("url_norm"));
        indexDoc.setOffset(NetarchiveSolrClient.getOffset(doc));
        // Cope with some minor schema variations:
        if ( doc.get("content_type") instanceof ArrayList) {
            ArrayList<String> types = (ArrayList<String>) doc.get("content_type");
            indexDoc.setContentType(types.get(0));
        } else {
            indexDoc.setContentType((String) doc.get("content_type"));
        }
        indexDoc.setContentTypeNorm((String) doc.get("content_type_norm"));
        indexDoc.setContentEncoding((String) doc.get("content_encoding"));
        indexDoc.setType((String) doc.get("type"));
        indexDoc.setExifLocation((String) doc.get("exif_location"));
        indexDoc.setRedirectToNorm((String) doc.get("redirect_to_norm"));
        indexDoc.setContent_type_full((String) doc.get("content_type_full"));
        Date dateModified = (Date) doc.get("last_modified");
        if (dateModified != null) {
            indexDoc.setLastModifiedLong(dateModified.getTime());
        }

        Object statusCodeObj = doc.get("status_code");
        if (statusCodeObj != null) {
            indexDoc.setStatusCode((Integer) statusCodeObj);
        }
        String hash = (String) doc.get("hash");
        indexDoc.setHash((String) hash);

        Date date = (Date) doc.get("crawl_date");
        indexDoc.setCrawlDateLong(date.getTime());
        indexDoc.setCrawlDate(DateUtils.getSolrDate(date));

        // Cope with some minor schema variations:
        if ( doc.get("content_type") instanceof ArrayList) {
            ArrayList<String> types = (ArrayList<String>) doc.get("content_type");
            indexDoc.setMimeType(types.get(0));
        } else {
            indexDoc.setMimeType((String) doc.get("content_type"));
        }

        indexDoc.setOffset(NetarchiveSolrClient.getOffset(doc));

        Object o = doc.getFieldValue("links_images");
        if (o != null) {
            indexDoc.setImageUrls((ArrayList<String>) o);
        }

        return indexDoc;
    }

    /**
     * Convert the given {@link SolrDocument} to a {@link IndexDocShort}.
     *
     * Note that {@link IndexDocShort}s only holds a tiny subset of all possible fields from {@link SolrDocument}:
     * {@code url}, {@code url_norm}, {@code source_file_path}m {@code source_file_offset} and {@code crawl_date}.
     * @param doc a document from a Solr search.
     * @return a short index document.
     */
    public static IndexDocShort solrDocument2IndexDocShort(SolrDocument doc) {
        IndexDocShort indexDoc = new IndexDocShort();

        indexDoc.setUrl((String) doc.get("url"));
        indexDoc.setUrl_norm((String) doc.get("url_norm"));
        indexDoc.setOffset(NetarchiveSolrClient.getOffset(doc));
        indexDoc.setSource_file_path((String) doc.get("source_file_path"));
        Date date = (Date) doc.get("crawl_date");
        indexDoc.setCrawlDate(DateUtils.getSolrDate(date));
        return indexDoc;
    }
}
