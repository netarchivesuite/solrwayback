package dk.kb.netarchivesuite.solrwayback.util;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SolrUtils {
    public static final Logger log = LoggerFactory.getLogger(SolrUtils.class);
    public static String NO_REVISIT_FILTER ="record_type:response OR record_type:arc OR record_type:resource";
    public static String indexDocFieldList = "id,score,title,url,url_norm,links_images,source_file_path,source_file,source_file_offset,domain,resourcename,content_type,content_type_full,content_type_norm,hash,type,crawl_date,content_encoding,exif_location,status_code,last_modified,redirect_to_norm";
    public static String indexDocFieldListShort = "url,url_norm,source_file_path,source_file,source_file_offset,crawl_date";
    public static String arcEntryDescriptorFieldList = "url,url_norm,source_file_path,source_file_offset,hash,content_type";

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
        return "url_norm:" + createPhrase(canonicalizedUrl);
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
     * See {@link #indexDocFieldList} for Solr fields for the IndexDoc.
     *
     * Note that {@link IndexDoc}s only holds a subset of all possible fields from {@link SolrDocument}.
     * @param doc a document from a Solr search.
     * @return an index document.
     */
    public static IndexDoc solrDocument2IndexDoc(SolrDocument doc) {
        if (doc == null) {
            throw new NullPointerException("The input SolrDocument was null");
        }
        IndexDoc indexDoc = new IndexDoc();
        if (doc.containsKey("score")) { // Not an essential value
            indexDoc.setScore(Double.valueOf((float) doc.getFieldValue("score")));
        }
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
        if (date == null) {
            throw new IllegalArgumentException("Mandatory crawl_date not available in SolrDocument");
        }
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
     * See {@link #indexDocFieldListShort} for Solr fields for the IndexDocShort.
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

    /**
     * Convert the given {@link SolrDocument} to an {@link ArcEntryDescriptor}.
     * @param solrDoc a Solr document with {@code url}, {@code url_norm}, {@code source_file_path},
     * {@code source_file_offset}, {@code hash} and {@code content_type} defined.
     * @return an ARC entry descriptor.
     */
    public static ArcEntryDescriptor solrDocument2ArcEntryDescriptor(SolrDocument solrDoc) {
//        return indexDoc2ArcEntryDescriptor(solrDocument2IndexDoc(solrDoc));
        ArcEntryDescriptor desc = new ArcEntryDescriptor();
        desc.setUrl((String) solrDoc.get("url"));
        desc.setUrl_norm((String) solrDoc.get("url_norm"));
        desc.setSource_file_path((String) solrDoc.get("source_file_path"));
        desc.setOffset((Long) solrDoc.get("source_file_offset"));
        desc.setHash((String) solrDoc.get("hash"));
        desc.setContent_type(getSingleStringValue(solrDoc, "content_type"));
        return desc;
    }

    /**
     * If the content of the field is a list, {@code Objects.toString(...)} of the first element is returned.
     * If the content of the field is a single value, {@code Objects.toString(...)} of the element is returned.
     * @param solrDoc a SolrDocument.
     * @param field the field to get the first value from.
     * @return a String representation of the first value in the field.
     * @throws ArrayIndexOutOfBoundsException if a value could not be extracted.
     */
    public static String getSingleStringValue(SolrDocument solrDoc, String field) {
        if (!solrDoc.containsKey(field)) {
            log.warn("The field '{}' was not available in the SolrDocument", field);
            throw new NullPointerException("The field '" + field + "' was not available in the SolrDocument");
        }
        Object value = solrDoc.get(field);
        if (value instanceof List) {
            return Objects.toString(((List<?>) value).get(0));
        }
        return Objects.toString(value);
    }

    /**
     * Create a function, typically for use as a filter on a stream, that prunes the fields for a given Solr document
     * down to the given list of fields and ensures that the order of the fields in the Solr document matches the
     * order stated with {@code fields}.
     * <p>
     * Child document, if any, are copied as-is to the resulting Solr document.
     * @param fields a list of Solr fields.
     * @return a new SolrDocument, where the fields are pruned and sorted.
     * @see #reduceAndSortFields(List)
     */
    public static Function<SolrDocument, SolrDocument> reduceAndSortFieldsFunction(List<String> fields) {
        return doc -> {
            Map<String, Object> entries = new LinkedHashMap<>(doc.size());
            for (String fieldName: fields) {
              if (doc.containsKey(fieldName)) {
                entries.put(fieldName, doc.get(fieldName));
              }
            }
            SolrDocument newDoc = new SolrDocument(entries);
            if (doc.hasChildDocuments()) {
                newDoc.addChildDocuments(doc.getChildDocuments());
            }
            return newDoc;
        };
    }

    /**
     * Create a consumer, typically for use in a forEach on a stream, that prunes the fields for a given Solr document
     * down to the given list of fields and ensures that the order of the fields in the Solr document matches the
     * order stated with {@code fields}.
     * <p>
     * Child documents are left untouched.
     * @param fields a list of Solr fields.
     * @see #reduceAndSortFieldsFunction(List)
     */
    public static Consumer<SolrDocument> reduceAndSortFields(List<String> fields) {
        return doc -> {
            Map<String, Object> entries = new LinkedHashMap<>(doc.size());
            for (String fieldName: fields) {
              if (doc.containsKey(fieldName)) {
                entries.put(fieldName, doc.get(fieldName));
              }
            }
            List<SolrDocument> children = doc.hasChildDocuments() ? new ArrayList<>(doc.getChildDocuments()) : null;
            doc.clear();
            doc.putAll(entries);
            if (children != null) {
                doc.addChildDocuments(children);
            }
        };
    }

    /**
     * Convert the given {@link IndexDoc} to an {@link ArcEntryDescriptor}.
     * @param indexDoc a document with {@link IndexDoc#getUrl()}, {@link IndexDoc#getUrl_norm()},
     * {@link IndexDoc#getSource_file_path()}, {@link IndexDoc#getOffset()}, {@link IndexDoc#getHash()} and
     * {@link IndexDoc#getMimeType()} returning usable values.
     * @return an ARC entry descriptor.
     */
    public static ArcEntryDescriptor indexDoc2ArcEntryDescriptor(IndexDoc indexDoc) {
        if (indexDoc == null) {
            throw new NullPointerException("The IndexDoc was null");
        }
        ArcEntryDescriptor desc = new ArcEntryDescriptor();
        desc.setUrl(indexDoc.getUrl());
        desc.setUrl_norm(indexDoc.getUrl_norm());
        desc.setSource_file_path(indexDoc.getSource_file_path());
        desc.setHash(indexDoc.getHash());
        desc.setOffset(indexDoc.getOffset());
        desc.setContent_type(indexDoc.getMimeType());
        return desc;
    }

    /**
     * Makes an independent copy of the given SolrQuery by deep-copying the {@code String[]} values.
     * @param solrQuery any Solr query.
     * @return an independent copy of the given Solr query.
     */
    public static SolrQuery deepCopy(SolrQuery solrQuery) {
      SolrQuery qc = new SolrQuery();
      solrQuery.getMap().entrySet().stream().
              peek(entry -> entry.setValue(Arrays.copyOf(entry.getValue(), entry.getValue().length))).
              forEach(entry -> qc.set(entry.getKey(), entry.getValue()));
      return qc;
    }

    /**
     * Sets properties-defined parameters.
     * This should be called with ALL SolrQuery instances before issuing the query.
     *
     * The semantics of whether it should be called before or after setting method specific parameters is unclear.
     * @param solrQuery a Solr query
     */
    public static void setSolrParams(SolrQuery solrQuery) {
        HashMap<String, String> SOLR_PARAMS_MAP = PropertiesLoader.SOLR_PARAMS_MAP;
        for (String key : SOLR_PARAMS_MAP.keySet()) {
            solrQuery.set(key,SOLR_PARAMS_MAP.get(key));
        }
    }

    /**
     * Quotes the given phrase and escapes characters that needs escaping (backslash and quote).
     * {@code foo \bar "zoo} becomes {@code "foo \\bar \"zoo"}.
     * @param phrase any phrase.
     * @return the phrase quoted and escaped.
     */
    public static String createPhrase(String phrase) {
        return "\"" + phrase.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * Construct a new array from the given strArray and the additions.
     * The result is guaranteed to be independent from the given strArray.
     * @param strArray  any String array.
     * @param additions 0 or more additions to the strArray.
     * @return a concatenation of strArray and additions.
     */
    public static String[] extend(String[] strArray, String... additions) {
        String[] extended = new String[strArray.length + additions.length];
        System.arraycopy(strArray, 0, extended, 0, strArray.length);
        System.arraycopy(additions, 0, extended, strArray.length, additions.length);
        return extended;
    }

    /**
     * Construct a String array from the given str and the additions.
     * @param str any String.
     * @param additions 0 or more additions to the str.
     * @return a concatenation of str and additions.
     */
    public static String[] extend(String str, String... additions) {
        String[] extended = new String[1 + additions.length];
        extended[0] = str;
        System.arraycopy(additions, 0, extended, 1, additions.length);
        return extended;
    }

    /**
     * Convert the given Solr field value to a String.
     * <p>
     * For most values this is a simple toString, but
     * Dates are converted using {@link DateUtils#getSolrDateFull(Date)} and Collections are expanded.
     * @param value a value from a Solr field.
     * @return a String representation of the given Solr field value or null if the input was null;
     */
    public static String fieldValueToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return DateUtils.getSolrDate((Date) value);
        }
        if (value instanceof String[]) {
            return Arrays.toString((String[])value);
        }
        if (value instanceof int[]) {
            return Arrays.toString((int[])value);
        }
        if (value instanceof long[]) {
            return Arrays.toString((int[])value);
        }
        if (value instanceof float[]) {
            return Arrays.toString((float[])value);
        }
        if (value instanceof double[]) {
            return Arrays.toString((double[])value);
        }
        if (value instanceof ModifiableSolrParams) {
            return value.getClass().getSimpleName() +
                   "(" + fieldValueToString(((ModifiableSolrParams)value).getMap()) + ")";
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>)value;
            return map.entrySet().stream().
                    map(SolrUtils::fieldValueToString).
                    collect(Collectors.joining(", ", "{", "}"));
        }
        if (value instanceof Map.Entry) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>)value;
            return fieldValueToString(entry.getKey()) + "=" +
                   fieldValueToString(entry.getValue());
        }
        if (value instanceof Collection) {
            return ((Collection<?>)value).stream().
                    map(SolrUtils::fieldValueToString).
                    collect(Collectors.joining(", ", "[", "]"));
        }
        return Objects.toString(value);
    }

    /**
     * Combine a predefined filter query, with filters applied in the frontend.
     *
     * @param predefinedFilterField specifies which field the predefined filter applies to.
     * @param predefinedFilterValue is the value given to the predefined field.
     * @param filterQueries         contains all filters that have been applied to the query in the frontend.
     * @return                      a single string containing all filterqueries that are to be applied.
     */
    public static String combineFilterQueries(String predefinedFilterField, String predefinedFilterValue, String[] filterQueries) {
        Stream<String> filtersStream = Stream.of(filterQueries);
        Stream<String> fullFiltersStream =Stream.concat(Stream.of(predefinedFilterField + ":(" + predefinedFilterValue + ")"), filtersStream);
        return fullFiltersStream
                .collect(Collectors.joining(" AND ", "(", ")"));
    }
}
