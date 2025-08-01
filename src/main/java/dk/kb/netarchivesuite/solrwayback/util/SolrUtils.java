package dk.kb.netarchivesuite.solrwayback.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;
import dk.kb.netarchivesuite.solrwayback.service.dto.MementoDoc;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SolrUtils {
    public static final Logger log = LoggerFactory.getLogger(SolrUtils.class);
    public static String NO_REVISIT_FILTER ="record_type:response OR record_type:arc OR record_type:resource";
    public static String indexDocFieldList = "id,score,title,url,url_norm,links_images,source_file_path,source_file,source_file_offset,domain,resourcename,content_type,content_type_full,content_type_norm,hash,type,crawl_date,content_encoding,exif_location,status_code,last_modified,redirect_to_norm";
    public static String indexDocFieldListShort = "url,url_norm,source_file_path,source_file,source_file_offset,crawl_date";
    public static String arcEntryDescriptorFieldList = "url,url_norm,source_file_path,source_file_offset,hash,content_type";
    public static String mementoDocFieldList = "content_length,wayback_date,content_type_served";

    /**
     * The {@code shardCache} is used by the {@link #getShards(String, String)} method. It caches forever, which does
     * present a potential problem if the Solr Cloud layout is changed without restarting SolrWayback.
     */
    // TODO: Add a timeout or other form of cache invalidation to this cache
    private static final Map<String, List<Shard>> shardCache = new HashMap<>();

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
     * Convert the given {@link SolrDocument} to a {@link MementoDoc}.
     * See {@link #mementoDocFieldList} for Solr fields for the MementoDoc.
     * This document is used to fetch data used in the memento framework.
     *
     * Note that {@link MementoDoc}s only holds a tiny subset of all possible fields from {@link SolrDocument}:
     * {@code url}, {@code url_norm}, {@code wayback_date} and {@code content_length}}.
     * @param doc a document from a Solr search.
     * @return a short index document.
     */
    public static MementoDoc solrDocument2MementoDoc(SolrDocument doc) {
        MementoDoc mementoDoc = new MementoDoc();

        mementoDoc.setContent_length((int) doc.get("content_length"));
        mementoDoc.setUrl((String) doc.get("url"));
        mementoDoc.setWayback_date((Long) doc.get("wayback_date"));
        mementoDoc.setUrl_norm((String) doc.get("url_norm"));
        mementoDoc.setContent_type((String) doc.get("content_type_served"));
        mementoDoc.setSource_file_path((String) doc.get("source_file_path"));
        mementoDoc.setSource_file_offset((long) doc.get("source_file_offset"));
        mementoDoc.setType((String) doc.get("type"));
        mementoDoc.setStatusCode((int) doc.get("status_code"));

        return mementoDoc;
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
        solrQuery.getMap().entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length)))
                .forEach(entry -> qc.set(entry.getKey(), entry.getValue()));
      return qc;
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
     * <ul>
     *     <li>Strings are processes using {@link #createPhrase(String)}</li>
     *     <li>Dates are converted using {@link DateUtils#getSolrDateFull(Date)}</li>
     *     <li>Collections are expanded</li>
     *     <li>All else is processed using {@link Objects#toString}</li>
     * </ul>
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
        if (value instanceof String) {
            return createPhrase((String) value);
        }
        if (value instanceof String[]) {
            return Arrays.stream((String[])value)
                    .map(SolrUtils::fieldValueToString)
                    .collect(Collectors.joining(", ", "[", "]"));
        }
        if (value instanceof int[]) {
            return Arrays.toString((int[])value);
        }
        if (value instanceof long[]) {
            return Arrays.toString((long[])value);
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
        Stream<String> fullFiltersStream =Stream.concat(Stream.of(predefinedFilterField + ":" + predefinedFilterValue), filtersStream);
        return fullFiltersStream
                .collect(Collectors.joining(") AND (", "(", ")"));
    }

    /**
     * Retrieve the shard names for the default collection on the default Solr.
     * If it is not possible to retrieve shard names, e.g. if the Solr is running in standalone mode,
     * null is returned.
     * <p>
     * Note: Due to the Solr alias mechanism, shardIDs are not guaranteed to be unique in the result.
     * To avoid ambiguity they are returned as {@link Shard} where the encapsulating collection is stated.
     * @return a list of the shards in the collection or null is the shard names could not be determined.
     */
    // TODO: Cache this
    public static List<Shard> getShards() {
        // http://localhost:8983/solr/netarchivebuilder/
        Matcher m = SOLR_COLLECTION_PATTERN.matcher(PropertiesLoader.SOLR_SERVER);
        if (!m.matches()) {
            log.warn("Unable to match Solr and collection from '{}' using pattern '{}'",
                     PropertiesLoader.SOLR_SERVER, SOLR_COLLECTION_PATTERN.pattern());
            return null;
        }
        return getShards(m.group(1), m.group(2));
    }
    private static final Pattern SOLR_COLLECTION_PATTERN = Pattern.compile("(http.*)/([^/]+)/?$");

    /**
     * Retrieve the shard names for the given {@code collection} in the given {@code solrBase}.
     * If it is not possible to retrieve shard names, e.g. if the Solr is running in standalone mode,
     * null is returned.
     * <p>
     * Note: Due to the Solr alias mechanism, shardIDs are not guaranteed to be unique in the result.
     * To avoid ambiguity they are returned as {@link Shard} where the encapsulating collection is stated.
     * @param solrBase   an address for a running Solr, such as {@code http://localhost:8983/solr}.
     * @param collection a Solr collection, such as {@code netarchivebuilder}.
     * @return a list of the shards in the collection or null if the shard names could not be determined.
     */
    public static List<Shard> getShards(String solrBase, String collection) {
        final String cacheKey = solrBase + "___" + collection;
        if (!shardCache.containsKey(cacheKey)) {
            cacheShards(cacheKey, solrBase, collection);
        }
        List<Shard> shards = shardCache.get(cacheKey);
        return shards == null || shards.isEmpty() ? null : shards;
    }

    /**
     * Retrieve the shard names for the given {@code collection} in the given {@code solrBase},
     * store the result in {@link #shardCache}.
     * @param cacheKey   the key to use when storing the result in the {@link #shardCache}.
     * @param solrBase   an address for a running Solr, such as {@code http://localhost:8983/solr}.
     * @param collection a Solr collection, such as {@code netarchivebuilder}.
     */
    private static void cacheShards(String cacheKey, String solrBase, String collection) {
        try {
            URI clusterStatusUrl = URI.create(solrBase + "/admin/collections?action=CLUSTERSTATUS");
            String statusJSON = IOUtils.toString(clusterStatusUrl, StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode statusRoot = mapper.readTree(statusJSON);
            // Maybe the collection is an alias
            List<String> collectionIDs = new ArrayList<>();
            if (!statusRoot.get("cluster").has("aliases")) {
                collectionIDs.add(collection);
            } else {
                collectionIDs.addAll(Arrays.asList(
                        statusRoot.get("cluster").get("aliases").get(collection).asText().split(", *")));
                log.debug("Resolved alias '{}' to collections {} ", collection, collectionIDs);
            }

            List<Shard> shardNames = new ArrayList<>();
            for (String collectionID: collectionIDs) {
                JsonNode shardsJSON = statusRoot.get("cluster").get("collections").get(collectionID).get("shards");
                for (Iterator<String> it = shardsJSON.fieldNames(); it.hasNext(); ) {
                    shardNames.add(new Shard(collectionID, it.next()));
                }
            }
            if (shardNames.isEmpty()) {
                log.warn("Unable to resolve shard names for Solr '{}' collectionIDs '{}'. " +
                         "Possibly because the Solr is running as standalone or from bundle release",
                         solrBase, collectionIDs);
            }
            shardCache.put(cacheKey, shardNames);
        } catch (Exception e) {
            log.warn("Could not resolve shard names for Solr '{}' collection '{}'. " +
                     "Possibly because the Solr is running as standalone or from bundle release",
                     solrBase, collection);
            shardCache.put(cacheKey, Collections.emptyList());
        }
    }

    /**
     * Representation of a shardID and its encapsulating collectionID.
     */
    public static final class Shard {
        public final String collectionID;
        public final String shardID;

        /**
         * Forgiving constructor. If {@code shardID} is classified {@code mycollection:myshard}, {@code collectionID}
         * is ignored and the classifying collection is used instead.
         * @param collectionID the collection containing the shard. Ignored if {@code sardID}
         * @param shardID      the shard itself.
         */
        public Shard(String collectionID, String shardID) {
            if (shardID.contains(":")) {
                String[] tokens = shardID.split(":", 2);
                collectionID = tokens[0];
                shardID = tokens[1];
            }
            this.collectionID =   collectionID;
            this.shardID = shardID;
        }

        @Override
        public String toString() {
            return collectionID + ":" + shardID;
        }
    }

    /**
     * The collection or collection alias for the overall SolrWayback setup.
     * @return The base collection for the overall SolrWayback setup.
     */
    public static String getBaseCollection() {
        Matcher m = SOLR_COLLECTION_PATTERN.matcher(PropertiesLoader.SOLR_SERVER);
        if (!m.matches()) {
            throw new IllegalStateException(String.format(
                    Locale.ROOT, "Unable to match Solr and collection from '%s' using pattern '%s'",
                    PropertiesLoader.SOLR_SERVER, SOLR_COLLECTION_PATTERN.pattern()));
        }
        return m.group(2);
    }
}
