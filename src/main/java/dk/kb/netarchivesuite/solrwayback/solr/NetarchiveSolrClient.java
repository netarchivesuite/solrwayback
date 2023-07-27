package dk.kb.netarchivesuite.solrwayback.solr;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dk.kb.netarchivesuite.solrwayback.util.CollectionUtils;
import dk.kb.netarchivesuite.solrwayback.util.Processing;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import dk.kb.netarchivesuite.solrwayback.util.UrlUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;

import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.*;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.GroupParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.util.NamedList;

import org.apache.solr.common.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.FacetCount;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.service.dto.statistics.DomainStatistics;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;

public class NetarchiveSolrClient {
    private static final Logger log = LoggerFactory.getLogger(NetarchiveSolrClient.class);
    private static final long M = 1000000; // ns -> ms

    protected static SolrClient solrServer;
    protected static SolrClient noCacheSolrServer;
    protected static NetarchiveSolrClient instance = null;
    protected static IndexWatcher indexWatcher = null;
    protected static Pattern TAGS_VALID_PATTERN = Pattern.compile("[-_.a-zA-Z0-9Ã¦Ã¸Ã¥Ã†Ã˜Ã…]+");
    private final AtomicLong lenientAttempts = new AtomicLong(0);
    private final AtomicLong lenientSuccesses = new AtomicLong(0);

    protected Boolean solrAvailable = null;

    protected NetarchiveSolrClient() { // private. Singleton
    }

    // Example url with more than 1000 rewrites:
    // http://belinda:9721/webarchivemimetypeservlet/services/wayback?waybackdata=20140119010303%2Fhttp%3A%2F%2Fbillige-skilte.dk%2F%3Fp%3D35

    /*
     * Called from initialcontextlistener when tomcat is starting up.
     *
     */
    public static void initialize(String solrServerUrl) {
        SolrClient innerSolrClient = new HttpSolrClient.Builder(solrServerUrl).build();

        if (PropertiesLoader.SOLR_SERVER_CACHING) {
            int maxCachingEntries = PropertiesLoader.SOLR_SERVER_CACHING_MAX_ENTRIES;
            int maxCachingSeconds = PropertiesLoader.SOLR_SERVER_CACHING_AGE_SECONDS;
            solrServer = new CachingSolrClient(innerSolrClient, maxCachingEntries,  maxCachingSeconds, -1); //-1 means no maximum number of connections 
            log.info("SolrClient initialized with caching properties: maxCachedEntrie="+maxCachingEntries +" cacheAgeSeconds="+maxCachingSeconds);
        } else {
            solrServer = new HttpSolrClient.Builder(solrServerUrl).build();
            log.info("SolClient initialized without caching");
        }

        // some of the solr query will never using cache. word cloud(cache memory) + playback resolving etc. (cache poisoning)
        noCacheSolrServer = new HttpSolrClient.Builder(solrServerUrl).build();

        // solrServer.setRequestWriter(new BinaryRequestWriter()); // To avoid http
        // error code 413/414, due to monster URI. (and it is faster)

        instance = new NetarchiveSolrClient();

        if (PropertiesLoader.SOLR_SERVER_CHECK_INTERVAL > 0) {
            indexWatcher = new IndexWatcher(
                    innerSolrClient, PropertiesLoader.SOLR_SERVER_CHECK_INTERVAL, instance::indexStatusChanged);
        }

        log.info("SolrClient initialized with solr server url:" + solrServerUrl);
    }

    public static NetarchiveSolrClient getInstance() {
        if (instance == null) {
            throw new IllegalArgumentException("SolrClient not initialized");
        }
        return instance;
    }

    private void indexStatusChanged(IndexWatcher.STATUS status) {
        switch (status) {
            case changed:
                if (solrServer instanceof CachingSolrClient) {
                    ((CachingSolrClient)solrServer).clearCache();
                }
                break;
            case available:
                solrAvailable = true;
                break;
            case unavailable:
                solrAvailable = false;
                break;
            case undetermined:
                log.debug("Got IndexWatcher.STATUS.undetermined. This should not be possible");
                break;
            default:
                log.warn("Unsupported value for IndexWatcher.STATUS: '{}'", status);
        }
    }

    /**
     * Requires a running {@link IndexWatcher}. If not enabled, the result will always be true.
     * Enabled per default, controlled by {@link PropertiesLoader#SOLR_SERVER_CHECK_INTERVAL}).
     * @return true if the backing Solr is available, else false. null if not determined
     */
    public Boolean isSolrAvailable() {
        return solrAvailable;
    }

    /*
     * Delegate
     */
    public List<FacetCount> getDomainFacets(String domain, int facetLimit, boolean ingoing, Date crawlDateStart, Date crawlDateEnd) throws Exception {

        if (ingoing) {
            return getDomainFacetsIngoing(domain, facetLimit, crawlDateStart, crawlDateEnd);
        } else {
            return getDomainFacetsOutgoing(domain, facetLimit, crawlDateStart, crawlDateEnd);
        }
    }

    /*
     * Get other domains linking to this domain
     *
     */
    public List<FacetCount> getDomainFacetsIngoing(String domain, int facetLimit, Date crawlDateStart, Date crawlDateEnd) throws Exception {

        String dateStart = DateUtils.getSolrDate(crawlDateStart);
        String dateEnd = DateUtils.getSolrDate(crawlDateEnd);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("links_domains:\"" + domain + "\" AND -domain:\"" + domain + "\"");
        solrQuery.setRows(0);
        solrQuery.set("facet", "true");
        solrQuery.add("facet.field", "domain");
        solrQuery.add("facet.limit", "" + facetLimit);
        solrQuery.addFilterQuery("crawl_date:[" + dateStart + " TO " + dateEnd + "]");

        solrQuery.add("fl","id");
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
        List<FacetCount> facetList = new ArrayList<FacetCount>();
        FacetField facet = rsp.getFacetField("domain");
        for (Count c : facet.getValues()) {
            FacetCount fc = new FacetCount();
            fc.setValue(c.getName());
            fc.setCount(c.getCount());
            facetList.add(fc);
        }
        return facetList;
    }

    /*
     * Get the domains this domain links to this domain
     */
    public List<FacetCount> getDomainFacetsOutgoing(String domain, int facetLimit, Date crawlDateStart, Date crawlDateEnd) throws Exception {

        String dateStart = DateUtils.getSolrDate(crawlDateStart);
        String dateEnd = DateUtils.getSolrDate(crawlDateEnd);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("domain:\"" + domain + "\"");

        solrQuery.setRows(0);
        solrQuery.set("facet", "true");
        solrQuery.add("facet.field", "links_domains");
        solrQuery.add("facet.limit", "" + (facetLimit + 1)); // +1 because itself will be removed and is almost certain of resultset is self-linking
        solrQuery.addFilterQuery("crawl_date:[" + dateStart + " TO " + dateEnd + "]");
        solrQuery.add("fl","id");                                                                                                                                                                  // request
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = noCacheSolrServer.query(solrQuery, METHOD.POST); //do not cache
        List<FacetCount> facetList = new ArrayList<FacetCount>();
        FacetField facet = rsp.getFacetField("links_domains");

        // We have to remove the domain itself.
        for (Count c : facet.getValues()) {
            if (!c.getName().equalsIgnoreCase(domain)) {
                FacetCount fc = new FacetCount();
                fc.setValue(c.getName());
                fc.setCount(c.getCount());
                facetList.add(fc);
            }
        }
        return facetList;
    }

    
    

    /*
    public String getRawSolrQuery(String query,List<String> fq,String fieldList, int rows, int startRow,HashMap<String,String> rawQueryParams)  throws Exception{        
         SolrQuery  solrQuery = new SolrQuery(query);
         solrQuery.setRows(rows);
         solrQuery.setStart(startRow);
         if (fieldList != null && !fieldList.equals("")) {                    
             solrQuery.setFields(fieldList.split(","));             
         }
         
         if ( fq != null) {
             for (String filter : fq) {
                 solrQuery.add("fq",filter);
             }
         }
         
         if ( rawQueryParams != null) {
             for (String param : rawQueryParams.keySet()) {                 
                 solrQuery.add(param,rawQueryParams.get(param));
             }
         }
         
         setSolrParams(solrQuery);

         NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
         rawJsonResponseParser.setWriterType("json");

         QueryRequest req = new QueryRequest(solrQuery);
         req.setResponseParser(rawJsonResponseParser);
         
         NamedList<Object> resp = solrServer.request(req);        
         String jsonResponse = (String) resp.get("response");        
         return jsonResponse;                          
     }
    */
    
    /*
     * The logic for getting the 4 dates in 2 queries is too complicated, and only
     * gives small performance boost...
     */
    public WaybackStatistics getWayBackStatistics(int statusCode, String url, String url_norm, String crawlDate) throws Exception {
        final long startNS = System.nanoTime();
        WaybackStatistics stats = new WaybackStatistics();
        stats.setStatusCode(statusCode); // this is know when calling the method, so no need to extract it from Solr.
        stats.setUrl(url);
        stats.setUrl_norm(url_norm);
        // These will only be set if they are different from input (end points). So set
        // them below
        stats.setLastHarvestDate(crawlDate);
        stats.setFirstHarvestDate(crawlDate);

        // We query for 1 result to get the domain.
        String domain = null;
        stats.setHarvestDate(crawlDate);
        final String statsField = "crawl_date";

        long results = 0;

        String query = "url_norm:\"" + url_norm + "\" AND crawl_date:{\"" + crawlDate + "\" TO *]";

        SolrQuery solrQuery = new SolrQuery(query);

        solrQuery.setRows(1);
        solrQuery.setGetFieldStatistics(true);
        solrQuery.setGetFieldStatistics(statsField);

        long call1ns = -System.nanoTime();
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
        call1ns += System.nanoTime();
        final long call1nsSolr = rsp.getQTime();

        results += rsp.getResults().getNumFound();
        if (rsp.getResults().getNumFound() != 0) {
            domain = (String) rsp.getResults().get(0).getFieldValue("domain");
            final FieldStatsInfo fieldStats = rsp.getFieldStatsInfo().get(statsField);
            if (fieldStats != null) {
                stats.setLastHarvestDate(DateUtils.getSolrDate((Date) fieldStats.getMax()));
                String next = DateUtils.getSolrDate((Date) fieldStats.getMin());
                if (!crawlDate.equals(next)) {
                    stats.setNextHarvestDate(next);// Dont want same as next
                }
            }
        }

        solrQuery = new SolrQuery("(url_norm:\"" + url_norm + "\") AND crawl_date:[* TO \"" + crawlDate + "\"}");
        solrQuery.setRows(1);
        solrQuery.add("fl", "domain");
        solrQuery.setGetFieldStatistics(true);
        solrQuery.setGetFieldStatistics(statsField);

        long call2ns = -System.nanoTime();
        SolrUtils.setSolrParams(solrQuery);
        rsp = solrServer.query(solrQuery, METHOD.POST);
        call2ns += System.nanoTime();
        final long call2nsSolr = rsp.getQTime();

        results += rsp.getResults().getNumFound();
        if (rsp.getResults().getNumFound() != 0) {
            domain = (String) rsp.getResults().get(0).getFieldValue("domain");
            final FieldStatsInfo fieldStats = rsp.getFieldStatsInfo().get(statsField);
            if (fieldStats != null) {
                stats.setFirstHarvestDate(DateUtils.getSolrDate((Date) fieldStats.getMin()));
                String previous = DateUtils.getSolrDate((Date) fieldStats.getMax());
                if (!crawlDate.equals(previous)) { // Dont want same as previous
                    stats.setPreviousHarvestDate(previous);
                }
            }
        }

        stats.setNumberOfHarvest(results + 1); // The +1 is the input value that is not included in any of the two result sets.

        long callDomain = -1;
        long callDomainSolr = -1;
        if (domain == null) {
            // This can happen if we only have 1 harvest. It will not be include in the
            // {x,*] og [*,x } since x is not included
            solrQuery = new SolrQuery("url_norm:\"" + url_norm + "\"");
            solrQuery.setRows(1);
            solrQuery.setGetFieldStatistics(true);
            solrQuery.setGetFieldStatistics(statsField);

            callDomain = -System.nanoTime();
            SolrUtils.setSolrParams(solrQuery);
            rsp = solrServer.query(solrQuery, METHOD.POST);
            callDomain += System.nanoTime();
            callDomainSolr = rsp.getQTime();
            if (rsp.getResults().size() == 0) {
                return stats; // url never found.
            }
            domain = (String) rsp.getResults().get(0).getFieldValue("domain");
        }
        stats.setDomain(domain);
        solrQuery = new SolrQuery("domain:\"" + domain + "\"");
        solrQuery.setRows(0);
        solrQuery.setGetFieldStatistics(true);
        solrQuery.setGetFieldStatistics("content_length");

        long call3ns = -System.nanoTime();
        SolrUtils.setSolrParams(solrQuery);
        rsp = solrServer.query(solrQuery, METHOD.POST);
        call3ns += System.nanoTime();
        final long call3nsSolr = rsp.getQTime();

        long numberHarvestDomain = rsp.getResults().getNumFound();
        stats.setNumberHarvestDomain(numberHarvestDomain);
        if (numberHarvestDomain != 0) {
            final FieldStatsInfo fieldStats = rsp.getFieldStatsInfo().get("content_length");
            if (fieldStats != null) {
                double totalContentLength = (Double) fieldStats.getSum();
                stats.setDomainHarvestTotalContentLength((long) totalContentLength);
            }
        }

        log.info(String.format(
                "Wayback statistics for url='%s', solrdate=%s extracted in %d ms "
                        + "(call_1=%d ms (qtime=%d ms), call_2=%d ms (qtime=%d ms), call_3=%d ms (qtime=%d ms), " + "domain_call=%d ms (qtime=%d ms))",
                url_norm.length() > 50 ? url_norm.substring(0, 50) + "..." : url_norm, crawlDate, (System.nanoTime() - startNS) / M, call1ns / M, call1nsSolr,
                call2ns / M, call2nsSolr, call3ns / M, call3nsSolr, callDomain / M, callDomainSolr));
        return stats;

    }

    /**
     * Find images matching the given searchString, deduplicating on {@code url_norm} and prioritizing versions
     * closest to the given timestamp.
     * @param searchString Solr search String (aka query).
     * @param timeStamp ISO-timestamp, Solr style: {@code 2011-10-14T14:44:00Z}.
     * @return ArcEntry representation of the matching images.
     */
    public ArrayList<ArcEntryDescriptor> findImagesForTimestamp(String searchString, String timeStamp) {
        return SolrGenericStreaming.create(
                        SRequest.builder().
                                query(searchString).
                                filterQueries("content_type_norm:image",   // only images
                                              SolrUtils.NO_REVISIT_FILTER, // No binary for revisits.
                                              "image_size:[2000 TO *]").   // No small images. (fillers etc.)
                                fields(SolrUtils.indexDocFieldList).
                                timeProximityDeduplication(timeStamp, "url_norm").
                                maxResults(50) // TODO: Make this an argument instead
                ).
                stream().
                map(SolrUtils::solrDocument2ArcEntryDescriptor).
                collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Calls {@link dk.kb.netarchivesuite.solrwayback.util.UrlUtils#punyCodeAndNormaliseUrl(String)} on each URL and
     * attempts a {@code url_norm:"<url>"} search. Duplicates are removed and if there is a hit for the URL, the
     * resulting {@link SolrDocument} is passed on. This step is done in batches.
     * <p>
     * All URLs without hits are resolved individually using {@link dk.kb.netarchivesuite.solrwayback.util.UrlUtils#lenientURLQuery(String)},
     * where the document with the highest score is selected. If there are no hits for an URL after this step, it is
     * discarded.
     * <p>
     * In both cases the {@link SolrDocument} is enriched with the original URL {@code originalURL:"<url>"}.
     * @param fields the fields to return from the URLs. If not present, {@code url} and {@code url_norm} will be added.
     * @param urls a Stream of URLs to resolve.
     * @param filterQueries optional filter queries for resolving.
     * @return a Stream of Solr documents with resolved {@code url}, {@code url_norm} plus the requested fields.
     */
    public Stream<SolrDocument> searchURLs(List<String> fields, Stream<String> urls, String... filterQueries) {
        // Handle processing in batches of 1000 for low latency and low memory overhead
        return CollectionUtils.splitToStreams(urls, 1000).
                flatMap(batch -> searchURLsSingleTake(fields, batch, filterQueries));
    }

    /**
     * Calls {@link dk.kb.netarchivesuite.solrwayback.util.UrlUtils#punyCodeAndNormaliseUrl(String)} on each URL and
     * attempts a {@code url_norm:"<url>"} search. Duplicates are removed and if there is a hit for the URL, the
     * resulting {@link SolrDocument} is passed on.
     * <p>
     * This implementation performs a full resolve of all URLs before delivery, which delays the time before first
     * delivered {@code SolrDocument} and introduces a memory overhead: This method should only be called for a limited
     * amount of URLs, such as 1000-10,000.
     * <p>
     * All URLs without hits are resolved individually using {@link dk.kb.netarchivesuite.solrwayback.util.UrlUtils#lenientURLQuery(String)},
     * where the document with the highest score is selected. If there are no hits for an URL after this step, it is
     * discarded.
     * <p>
     * In both cases the {@link SolrDocument} is enriched with the original URL {@code originalURL:"<url>"}.
     * @param fields the fields to return from the URLs. If not present, {@code url} and {@code url_norm} will be added.
     * @param urls a Stream of URLs to resolve.
     * @param filterQueries optional filter queries for resolving.
     * @return a Stream of Solr documents with resolved {@code url}, {@code url_norm} plus the requested fields.
     */
    private Stream<SolrDocument> searchURLsSingleTake(
            List<String> fields, Stream<String> urls, String... filterQueries) {
        ArrayList<String> allFields = new ArrayList<>(fields);
        if (!fields.contains("url")) {
            allFields.add("url");
        }
        if (!fields.contains("url_norm")) {
            allFields.add("url_norm");
        }

        // Stream<String> of <= 1000 URLs

        // Create list of [originalURL, normURL]
        List<Pair<String, String>> urlPairs = getNormalisedURLs(urls);

        // Try direct url_norm query resolving
        Map<String, SolrDocument> direct = resolveURLsDirect(allFields, urlPairs, filterQueries);

        // Use lenient resolving on the rest
        Stream<Pair<String, String>> unresolved = urlPairs.stream().
                filter(urlPair -> !direct.containsKey(urlPair.first()));

        Map<String, SolrDocument> lenient = resolveURLsLenient(allFields, unresolved, filterQueries);

        // Merge the results from direct and lenient and enrich with the SolrDocuments with originalURL
        return urlPairs.stream().
                map(Pair::first). // originalURL
                        map(originalURL -> getValueFromMaps(originalURL, direct, lenient)).
                filter(Objects::nonNull).
                peek(resultPair -> resultPair.second().setField("originalURL", resultPair.first())).
                map(Pair::second);
    }

    /**
     * Perform direct {@code url_norm:<normURL>} searches in batches of 1000 and deliver the result as a
     * {code Map<originalURL, SolrDocument>}. This is a fairly lightweight process.
     * @param fields the fields to return in the SolrDocument. These <b>must</b> include {@code url_norm}.
     * @param urlPairs pairs of {@code originalURL, normURL}.
     * @param filterQueries 0 or more filters for the search.
     * @return a {@code Map<originalURL, Solrdocument>} for all resolved URLs.
     */
    private Map<String, SolrDocument> resolveURLsDirect(
            List<String> fields, List<Pair<String, String>> urlPairs, String... filterQueries) {
        return resolveURLsDirect(fields, null, urlPairs, filterQueries);
    }

    /**
     * Perform searches for all given URLs, deduplicating on {@code url_norm} and prioritizing those closest to the
     * given timestamp if idealTime is not null. No practical limit on the number of URLs or the search result.
     * The URL-requests are batched in groups of 1000 for performance.
     * @param fields the fields to return in the SolrDocument. These <b>must</b> include {@code url_norm}.
     * @param idealTime ISO-timestamp, Solr style: {@code 2011-10-14T14:44:00Z}.
     * @param urlPairs pairs of {@code originalURL, normURL}.
     * @param filterQueries 0 or more filters for the search.
     * @return a {@code Map<originalURL, Solrdocument>} for all resolved URLs.
     */
    private Map<String, SolrDocument> resolveURLsDirect(
            List<String> fields, String idealTime, List<Pair<String, String>> urlPairs, String... filterQueries) {
        if (!fields.contains("url_norm")) {
            throw new IllegalArgumentException("fields does not contain url_norm");
        }

        // Create list of url queries for the normURLs
        Stream<String> urlQueries = urlPairs.stream().
                map(Pair::second).
                map(normURL -> "url_norm:" + SolrUtils.createPhrase(normURL));

        // Resolve SolrDocuments using direct url_norm search and store them in a Map with url_norm as key
        SRequest request = SRequest.builder().
                queries(urlQueries).
                queryBatchSize(1000). // Same as partitionSize in splitToStreams
                //usePaging(false). // Optimize Solr lookups (no longer needed)
                fields(fields).
                filterQueries(filterQueries);
        if (idealTime != null) {
            request = request.timeProximityDeduplication(idealTime, "url_norm");
        } else {
            request = request.deduplicateField("url_norm");
        }

        Map<String, SolrDocument> normResolved = request.stream().
                collect(Collectors.toMap(value -> Objects.toString(value.getFieldValue("url_norm")),
                                         value -> value));

        // Convert the Map of [url_norm, SolrDocument] to a Map of [originalURL, SolrDocument]
        return urlPairs.stream().
                map(urlPair -> new Pair<String, SolrDocument>(urlPair.first(), normResolved.get(urlPair.second()))).
                filter(urlPair -> Objects.nonNull(urlPair.second())).
                collect(Collectors.toMap(Pair::first, Pair::second));
    }

    /**
     * Perform lenient resolving of originalURLs and deliver the result as a {code Map<originalURL, SolrDocument>}.
     * The URLs are resolved individually but in parallel. This is a fairly heavy process.
     * @param fields the fields to return in the SolrDocument. These <b>must</b> include {@code url_norm}.
     * @param urlPairs pairs of {@code originalURL, normURL}.
     * @param filterQueries 0 or more filters for the search.
     * @return a {@code Map<originalURL, SolrDocument>} for all resolved URLs.
     */
    private Map<String, SolrDocument> resolveURLsLenient(
            List<String> fields, Stream<Pair<String, String>> urlPairs, String[] filterQueries) {
        if (!fields.contains("url_norm")) {
            throw new IllegalStateException("fields does not contain 'url_norm'");
        }
        // Create jobs for unresolved originalURLs that delivers [originalURL, SolrDocument]
        Stream<Callable<Pair<String, SolrDocument>>> lenientJobs = urlPairs.
                map(Pair::first). // Only the originalURL is relevant when doing lenient resolving
                map(originalURL -> () -> new Pair<>(
                        originalURL,
                        resolveURLLenient(fields, originalURL, filterQueries)));

        // Run jobs and collect Map with [originalURL, SolrDocument]
        return Processing.batch(lenientJobs).
                peek(jobPair -> {
                    if (Objects.isNull(jobPair.second())) {
                        log.debug("Unable to lenient resolve '{}'", jobPair.first());
                    }
                }).
                filter(jobPair -> Objects.nonNull(jobPair.second())).
                peek(jobPair -> {
                    String originalURL = jobPair.first();
                    String normURL = Objects.toString(jobPair.second().getFieldValue("url_norm"));
                    if (originalURL.equals(normURL)) {
                        log.debug("Note: Lenient resolved '{}', but the url_norm was equal to the originalURL",
                                  originalURL);
                    } else {
                        log.debug("Lenient resolved '{}' to '{}'", originalURL, normURL);
                    }
                    }
                ).
                collect(Collectors.toMap(Pair::first, Pair::second));
    }

    /**
     * Perform lenient resolving of a single URL and deliver the first result, if any.
     * This uses {@link UrlUtils#lenientURLQuery(String)} for constructing the query.
     * @param fields the fields to return in the SolrDocument. These <b>must</b> include {@code url_norm}.
     * @param url a raw URL.
     * @param filterQueries 0 or more filters for the search.
     * @return the first SolrDocument matching the lenient query or null if there were no matches.
     */
    public SolrDocument resolveURLLenient(List<String> fields, String url, String... filterQueries) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(UrlUtils.lenientURLQuery(url));
        solrQuery.setFilterQueries(filterQueries);
        solrQuery.set(CommonParams.ROWS, 1);
        solrQuery.set(CommonParams.FL, String.join(",", fields));
        solrQuery.set(HighlightParams.HIGHLIGHT, false);
        solrQuery.set(FacetParams.FACET, false);
        solrQuery.set(GroupParams.GROUP, false);
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse response;
        try {
            lenientAttempts.incrementAndGet();
            response = noCacheSolrServer.query(solrQuery);
            if (response.getResults() != null && !response.getResults().isEmpty()) {
                lenientSuccesses.incrementAndGet();
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception trying to resolve URL lenient for '" + url + "'", e);
        }
        return response.getResults().isEmpty() ? null : response.getResults().get(0);
    }

    /**
     * Searches Solr for a video matching the given search string. If no indexed entry is found returns null.
     * @param videoQueryString String to query Solr for.
     * @return ArcEntryDescriptor containing info on first video found in search or null if no results found.
     * @throws Exception If communication with Solr fails.
     */
    public ArcEntryDescriptor findVideo(String videoQueryString) throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(videoQueryString);
        SolrUtils.setSolrParams(solrQuery);
        solrQuery.setRows(1); // Just get one result

        solrQuery.set("facet", "false"); // Very important. Must overwrite to false. Facets are very slow and expensive.
        solrQuery.add("fq", "content_type_norm:video"); // only videos
        solrQuery.add("fq", SolrUtils.NO_REVISIT_FILTER);
        solrQuery.add("fl", SolrUtils.indexDocFieldList);

        QueryResponse response = solrServer.query(solrQuery, METHOD.POST);

        SolrDocumentList queryResults = response.getResults();
        if (queryResults.getNumFound() == 0) {
            return null;
        } else {
            return SolrUtils.solrDocument2ArcEntryDescriptor(queryResults.get(0));
        }
    }

    public SearchResult search(String searchString, int results) throws Exception {
        return search(searchString, null, results);
    }

    public SearchResult search(String searchString, String filterQuery) throws Exception {
        return search(searchString, filterQuery, 50);
    }

    public ArrayList<Date> getHarvestTimesForUrl(String url) throws Exception {
        ArrayList<Date> dates = new ArrayList<Date>();
        
        String query=UrlUtils.fixLegacyNormaliseUrlErrorQuery(url);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery = new SolrQuery(query);
        solrQuery.set("facet", "false"); // very important. Must overwrite to false. Facets are very slow and expensive.
        solrQuery.add("fl", "id,crawl_date");
        solrQuery.setRows(1000000);
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = loggedSolrQuery("getHarvestTimeForUrl", solrQuery);

        SolrDocumentList docs = rsp.getResults();

        for (SolrDocument doc : docs) {
            Date date = (Date) doc.get("crawl_date");
            dates.add(date);
        }
        return dates;
    }

    /*
     * Fast solr method that counts number of results
     */
    public long countResults(String query, String... filterQueries) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery = new SolrQuery(query);
        solrQuery.set("facet", "false"); // very important. Must overwrite to false. Facets are very slow and expensive.
        solrQuery.add("fl", "id");
        solrQuery.setFilterQueries(filterQueries);
        solrQuery.setRows(0);
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
        return rsp.getResults().getNumFound();
    }

    public String getConcatedTextFromHtmlForQuery(String query,String filterQuery) throws Exception {

        SolrQuery solrQuery = new SolrQuery();
        solrQuery = new SolrQuery(query);

        solrQuery.add("fl", "id, content_text_length, content");
        solrQuery.addFilterQuery("content_type_norm:html", "content_text_length:[1000 TO *]"); // only html pages and pages with many words.
        if (filterQuery != null && filterQuery.length() >0) {
            solrQuery.addFilterQuery(filterQuery);
        }
        solrQuery.setRows(5000);

        long solrNS = -System.nanoTime();
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = noCacheSolrServer.query(solrQuery, METHOD.POST); //do not cache
        solrNS += System.nanoTime();
        SolrDocumentList docs = rsp.getResults();

        StringBuilder b = new StringBuilder();
        long totaltLength = 0;
        for (SolrDocument doc : docs) {
            b.append(doc.getFieldValue("content"));
            b.append(doc.getFieldValue(" "));// Space between next document.
            totaltLength += ((int) doc.getFieldValue("content_text_length"));
        }
        log.info(String.format("Total extracted content length for wordcloud:%d, total hits:%d only using first 1000 hits" + " in %d ms (qtime=%d ms)",
                totaltLength, rsp.getResults().getNumFound(), solrNS / M, rsp.getQTime()));
        return b.toString();
    }

    public ArrayList<IndexDoc> getHarvestPreviewsForUrl(int year,String url) throws Exception {

        String urlNormFixed = normalizeUrl(url);
        urlNormFixed = urlNormFixed.replace("\\", "\\\\");
        SolrQuery solrQuery = new SolrQuery();
        solrQuery = new SolrQuery("(url_norm:\"" + urlNormFixed + "\"");
        solrQuery.set("facet", "false"); // very important. Must overwrite to false. Facets are very slow and expensive.
        solrQuery.add("fl", "id, crawl_date,source_file_path, source_file, source_file_offset, score");
        solrQuery.add("sort", "crawl_date asc");
        solrQuery.add("fq","crawl_year:"+year);
        solrQuery.setRows(1000000);

        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
        SolrUtils.setSolrParams(solrQuery);
        SolrDocumentList docs = rsp.getResults();

        ArrayList<IndexDoc> indexDocs = SolrUtils.solrDocList2IndexDoc(docs);
        return indexDocs;
    }


    public ArrayList<FacetCount> getPagePreviewsYearInfo(String url) throws Exception {

        String urlNormFixed = normalizeUrl(url);
        urlNormFixed = urlNormFixed.replace("\\", "\\\\");
        SolrQuery solrQuery = new SolrQuery();
        solrQuery = new SolrQuery("(url_norm:\"" + urlNormFixed + "\"");

        solrQuery.set("facet", "true");
        solrQuery.add("facet.field", "crawl_year");
        solrQuery.add("facet.limit", "100"); //All years...
        solrQuery.add("fl","id");
        solrQuery.setRows(0);
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
        ArrayList<FacetCount> facetList = new ArrayList<FacetCount>();
        FacetField facet = rsp.getFacetField("crawl_year");
        for (Count c : facet.getValues()) {
            FacetCount fc = new FacetCount();
            fc.setValue(c.getName());
            fc.setCount(c.getCount());
            facetList.add(fc);
        }


        return facetList;
    }


    public IndexDoc getArcEntry(String source_file_path, long offset) throws Exception {

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.set("facet", "false"); // very important. Must overwrite to false. Facets are very slow and expensive.
        solrQuery.add("fl", SolrUtils.indexDocFieldList);

        String query = null;

        // This is due to windows path in solr field source_file_offset. For linux the
        // escape does nothing
        // String pathEscaped= ClientUtils.escapeQueryChars(source_file_path); This is
        // done by the warc-indexer now

        query = "source_file_path:\"" + source_file_path + "\" AND source_file_offset:" + offset;
        solrQuery.setQuery(query);
        solrQuery.setRows(1);

        // QueryResponse rsp = loggedSolrQuery("getArchEntry", solrQuery); //Timing disabled due to spam. Also only took 1-5 millis
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = noCacheSolrServer.query(solrQuery, METHOD.POST);
        SolrDocumentList docs = rsp.getResults();

        if (docs.getNumFound() == 0) {
            throw new Exception("Could not find arc entry in index:" + source_file_path + " offset:" + offset);
        }

        ArrayList<IndexDoc> indexDocs = SolrUtils.solrDocList2IndexDoc(docs);

        return indexDocs.get(0);
    }

    /*
     *
     * public SearchResult imageLocationSearch(String searchString, int results)
     * throws Exception { log.info("imageLocationsearch for:" + searchString);
     * SearchResult result = new SearchResult(); SolrQuery solrQuery = new
     * SolrQuery(); solrQuery.set("facet", "false"); //very important. Must
     * overwrite to false. Facets are very slow and expensive. solrQuery.add("fl",
     * indexDocFieldList); solrQuery.setQuery(searchString); // only search images
     * solrQuery.setRows(results); solrQuery.setFilterQueries(
     * +" and filter:"+filterQuery););
     *
     *
     * QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST); SolrDocumentList
     * docs = rsp.getResults();
     *
     *
     * result.setNumberOfResults(docs.getNumFound()); ArrayList<IndexDoc> indexDocs
     * = solrDocList2IndexDoc(docs); result.setResults(indexDocs);
     * log.info("search for:" + searchString + " found:" +
     * result.getNumberOfResults()); return result; }
     *
     */

    /*
     * Sort can be null. Also define order for sort, example: sort =
     * "crawl_date asc";
     */

    public ArrayList<IndexDoc> imagesLocationSearchWithSort(String searchText, String filterQuery, int results, double latitude, double longitude,
                                                            double radius, String sort) throws Exception {
        log.info("imagesLocationSearch:" + searchText + " coordinates:" + latitude + "," + longitude + " radius:" + radius);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.set("facet", "false"); // very important. Must overwrite to false. Facets are very slow and expensive.
        solrQuery.add("fl", SolrUtils.indexDocFieldList);
        solrQuery.add("group", "true");
        solrQuery.add("group.field", "hash"); // Notice not using url_norm. We want really unique images.
        solrQuery.add("group.format", "simple");
        solrQuery.add("group.limit", "1");
        if (sort != null) {
            solrQuery.add("sort", sort);
        }
        solrQuery.setRows(results);
        
        
        SolrUtils.setSolrParams(solrQuery); //NOT SURE ABOUT THIS ONE!
        
        // The 3 lines defines geospatial search. The ( ) are required if you want to
        // AND with another query
        solrQuery.setQuery("({!geofilt sfield=exif_location}) AND " + searchText);
        solrQuery.setParam("pt", latitude + "," + longitude);
        solrQuery.setParam("d", "" + radius);

        if (filterQuery != null) {
            solrQuery.setFilterQueries(filterQuery);
        }

        QueryResponse rsp = solrServer.query(solrQuery);

        // SolrDocumentList docs = rsp.getResults();
        SolrDocumentList docs = rsp.getGroupResponse().getValues().get(0).getValues().get(0).getResult();
        ArrayList<IndexDoc> indexDocs = SolrUtils.solrDocList2IndexDoc(docs);

        return indexDocs;
    }

    public SearchResult search(String searchString, String filterQuery, int results) throws Exception {
        SearchResult result = new SearchResult();
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.set("facet", "false"); // very important. Must overwrite to false. Facets are very slow and expensive.
        solrQuery.add("fl", SolrUtils.indexDocFieldList);
        solrQuery.setQuery(searchString); // only search images
        solrQuery.setRows(results);
        if (filterQuery != null) {
            solrQuery.setFilterQueries(filterQuery);
        }
        
      
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = loggedSolrQuery("search", solrQuery);
        SolrDocumentList docs = rsp.getResults();

        result.setNumberOfResults(docs.getNumFound());
        ArrayList<IndexDoc> indexDocs = SolrUtils.solrDocList2IndexDoc(docs);
        result.setResults(indexDocs);
        return result;
    }

    public long numberOfDocuments() throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
        SolrDocumentList docs = rsp.getResults();
        return docs.getNumFound();
    }

    public ArrayList<IndexDoc> findNearestHarvestTimeForMultipleUrlsFullFields(Collection<String> urls, String timeStamp) {
        return findNearestDocuments(SolrUtils.indexDocFieldList, timeStamp, urls.stream()).
                map(SolrUtils::solrDocument2IndexDoc).
                collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<IndexDocShort> findNearestHarvestTimeForMultipleUrlsFewFields(Collection<String> urls, String timeStamp){
        return findNearestDocuments(SolrUtils.indexDocFieldListShort, timeStamp, urls.stream()).
                map(SolrUtils::solrDocument2IndexDocShort).
                collect(Collectors.toCollection(ArrayList::new));
    }

    public Stream<IndexDocShort> findNearestHarvestTimeForSingleUrlFewFields(String url, String timeStamp){
        return findNearestDocuments(SolrUtils.indexDocFieldListShort, timeStamp, Stream.of(url))
                .map(SolrUtils::solrDocument2IndexDocShort);
    }


    /**
     * Resolves {@link IndexDocShort}s for the given URLs.
     * If lenient is true, fuzzy matching is used for URLs that cannot be located through simple {@code url_norm} search.
     * Lenient matching calls {@link IndexDocShort#setUrl(String)} with the original URL, instead of the URL returned
     * in the {@link SolrDocument} from the search.
     * @param urls      URLs for the resources to locate.
     * @param timeStamp in case of multiple hits for a single URL, the resource with harvest time closest to this
     *                  timestamp is preferred.
     * @param lenient   if true, lenient matching is used for the resources that were not located using simple search.
     * @return a list of {@link IndexDocShort} for the given URLs.
     */
    public ArrayList<IndexDocShort> findNearestUrlsShort(Collection<String> urls, String timeStamp, boolean lenient) {
        Stream<SolrDocument> docs = lenient ?
                findNearestDocumentsLenient(SolrUtils.indexDocFieldListShort, timeStamp, urls.stream()) :
                findNearestDocuments(SolrUtils.indexDocFieldListShort, timeStamp, urls.stream());

        return docs.
                map(SolrUtils::solrDocument2IndexDocShort).
                collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Perform searches for all given URLs, deduplicating on {@code url_norm} and prioritizing those closest to the
     * given timestamp. No practical limit on the number of URLs or the search result.
     * <p>
     * Note: Revisits are not considered as candidates: See {@link SolrUtils#NO_REVISIT_FILTER}.
     *
     * @param fieldList the fields to return.
     * @param timeStamp ISO-timestamp, Solr style: {@code 2011-10-14T14:44:00Z}.
     * @param urls      0 or more URLs, which will be normalised and searched with {@code url_norm:"normalized_url"}.
     * @param filterQueries 0 or more filterQueries for restring the URL search.
     * @return the documents with the given URLs.
     */
    public Stream<SolrDocument> findNearestDocuments(
            String fieldList, String timeStamp, Stream<String> urls, String... filterQueries) {
        final int chunkSize = 1000;

        Stream<String> urlQueries = urls.
                filter(url -> !url.startsWith("data:")).
                map(NetarchiveSolrClient::normalizeUrl).
                map(SolrUtils::createPhrase).
                map(url -> "url_norm:" + url);

        return SRequest.builder().
                queries(urlQueries).
                filterQueries(SolrUtils.extend(SolrUtils.NO_REVISIT_FILTER, filterQueries)). // No binary for revists
                queryBatchSize(chunkSize). // URL-searches are single-clause queries, so we can use large batches
                pageSize(chunkSize).
                //usePaging(false). // 1 URL = 1 hit as we deduplicate on url_norm (no longer needed)
                fields(fieldList).
                timeProximityDeduplication(timeStamp, "url_norm").
                stream();
    }

    /**
     * Perform searches for all given URLs, deduplicating on {@code url_norm} and prioritizing those closest to the
     * given timestamp. No practical limit on the number of URLs or the search result.
     * <p>
     * If a document cannot be resolved using direct matching with {@code url_norm:<normURL>}, lenient matching is used.
     * Lenient first locates the {@code url_norm} closest to the original URL, then feeds that {@code url_norm} to
     * time prioritized resolving. When producing {@link SolrDocument}s, a normalised version of the original URL is
     * set as the document's {@code url_norm} instead of the one originally returned by Solr.
     * <p>
     * Note: Revisits are not considered as candidates: See {@link SolrUtils#NO_REVISIT_FILTER}.
     *
     * @param fields the fields to return. If not present, {@code url} and {@code url_norm} will be added
     * @param idealTime ISO-timestamp, Solr style: {@code 2011-10-14T14:44:00Z}.
     * @param urls      0 or more URLs, which will be normalised and searched with {@code url_norm:"normalized_url"}.
     * @param filterQueries 0 or more filterQueries for restring the URL search.
     * @return the documents with the given URLs.
     */
    public Stream<SolrDocument> findNearestDocumentsLenient(
            String fields, String idealTime, Stream<String> urls, String... filterQueries) {
        final int chunkSize = 1000;
        String[] extendedFilterQueries = SolrUtils.extend(SolrUtils.NO_REVISIT_FILTER, filterQueries);

        // Handle processing in batches of 1000 for fast resolving
        return CollectionUtils.splitToStreams(urls.filter(url -> !url.startsWith("data:")), chunkSize).
                flatMap(batch -> findNearestDocumentLenientSingleTake(
                        fields, idealTime, batch, extendedFilterQueries));
    }

    /**
     * Perform searches for all given URLs, deduplicating on {@code url_norm} and prioritizing those closest to the
     * given timestamp.
     * <p> 
     * This implementation performs a full resolve of all URLs before delivery, which delays the time before first
     * delivered {@code Solrdocument} and introduces a memory overhead: This method should only be called for a limited
     * amount of URLs, such as 1000-10,000.
      <p>
     * If a document cannot be resolved using direct matching with {@code url_norm:<normURL>}, lenient matching is used.
     * Lenient first locates the {@code url_norm} closest to the original URL, then feeds that {@code url_norm} to
     * time prioritized resolving. When producing {@link SolrDocument}s, a normalised version of the original URL is
     * set as the document's {@code url_norm} instead of the one originally returned by Solr.
     * <p>
     * Note: Revisits are not considered as candidates: See {@link SolrUtils#NO_REVISIT_FILTER}.
     *
     * @param fields        the fields to return. If not present, {@code url} and {@code url_norm} will be added
     * @param urls          0 or more URLs, which will be normalised and searched with {@code url_norm:"normalized_url"}.
     * @param idealTime     ISO-timestamp, Solr style: {@code 2011-10-14T14:44:00Z}.
     * @param filterQueries 0 or more filterQueries for restring the URL search.
     * @return the documents with the given URLs.
     */
    private Stream<SolrDocument> findNearestDocumentLenientSingleTake(
            String fields, String idealTime, Stream<String> urls, String[] filterQueries) {
        // Ensure url & url_norm is returned
        ArrayList<String> allFields = new ArrayList<>(Arrays.asList(fields.split(", *")));
        if (!fields.contains("url")) {
            allFields.add("url");
        }
        if (!fields.contains("url_norm")) {
            allFields.add("url_norm");
        }

        // Stream<String> of <= 1000 URLs

        // Create list of [originalURL, normURL]
        List<Pair<String, String>> urlPairs = getNormalisedURLs(urls);

        // Try direct url_norm query resolving
        Map<String, SolrDocument> direct = resolveURLsDirect(allFields, idealTime, urlPairs, filterQueries);

        // Use lenient resolving on the unresolved to get url_norm
        Stream<Pair<String, String>> unresolved = urlPairs.stream().
                filter(urlPair -> !direct.containsKey(urlPair.first()));

        List<Pair<String, String>> lenientURLPairs = // [originalURL, lenientResolvedNormURL]
                resolveURLsLenient(Collections.singletonList("url_norm"), unresolved, filterQueries).
                        entrySet().stream().
                        filter(entry -> entry.getValue().containsKey("url_norm")).
                        map(entry -> new Pair<>(
                                entry.getKey(),
                                Objects.toString(entry.getValue().getFieldValue("url_norm")))).
                        collect(Collectors.toList());

        // Use the leniently resolved url_norm for time-proximity lookup
        Map<String, SolrDocument> lenient =
                resolveURLsDirect(allFields, idealTime, lenientURLPairs, filterQueries);

        // Merge the results from direct and lenient and enrich with the SolrDocuments with originalURL
        return urlPairs.stream().
                map(Pair::first). // originalURL
                map(originalURL -> getValueFromMaps(originalURL, direct, lenient)).
                filter(Objects::nonNull).
                peek(resultPair -> resultPair.second().setField("originalURL", resultPair.first())).
                peek(resultPair -> resultPair.second().setField(
                        "url_norm", UrlUtils.punyCodeAndNormaliseUrlSafe(resultPair.first()))).
                map(Pair::second);
    }

    /**
     * Ensures the given URLs are valid HTTP(S) URLs, normalises them and ensures distinct URLs.
     * This holds the full result in memory before delivery.
     * @param urls any URLs.
     * @return a list of {@code [originalURL, normURL]}.
     */
    private List<Pair<String, String>> getNormalisedURLs(Stream<String> urls) {
        return urls.
                map(url -> new Pair<>(url, UrlUtils.punyCodeAndNormaliseUrlSafe(url))).
                filter(urlPair -> Objects.nonNull(urlPair.second())).
                distinct().
                collect(Collectors.toList());
    }

    /**
     * Iterate maps checking if there is a value for the given key. Upon firencounter, return that value.
     * If no maps holds the value, null is returned.
     * @param key  a key for the wanted value.
     * @param maps 0 or more maps that might hold a value for the key.
     * @return the value for the key if it exists in any map, else null.
     */
    @SafeVarargs
    private static <T> Pair<String, T> getValueFromMaps(String key, Map<String, T>... maps) {
        return Arrays.stream(maps).
                map(map -> map.get(key)).
                filter(Objects::nonNull).
                map(map -> new Pair<>(key, map)).
                findFirst().
                orElse(null);
    }

    public static void mergeInto(SolrDocumentList main, SolrDocumentList additional) {
        if (additional == null) {
            return;
        }
        main.addAll(additional);
        if (additional.getMaxScore() != null) {
            main.setMaxScore(main.getMaxScore() == null ? additional.getMaxScore() : Math.max(main.getMaxScore(), additional.getMaxScore()));
        }
        main.setNumFound(main.getNumFound() + additional.getNumFound());
    }

    private SolrDocumentList groupsToDoc(QueryResponse rsp) {
        SolrDocumentList docs = new SolrDocumentList();
        if (rsp == null || rsp.getGroupResponse() == null || rsp.getGroupResponse().getValues() == null || rsp.getGroupResponse().getValues().isEmpty()) {
            return docs;
        }
        for (GroupCommand groupCommand : rsp.getGroupResponse().getValues()) {
            for (Group group : groupCommand.getValues()) {
                mergeInto(docs, group.getResult());
            }
        }
        return docs;
    }

     /**
      * Creates a query for 1 or more URLs, taking care to quote URLs and escape characters where needed.
      * The result will be in the form {@code field:("url1" OR "url2")} or {@code field:("url1" AND "url2")}
      * depending on operator.
      * <p>
      * Note: {@code data:}-URLs are ignored as they will never match.
      * @param field    the field to query. Typically {@code url} or {@code url_norm}.
      * @param operator {@code AND} or {@code OR}.
      * @param urls     the URLs to create a query for.
      * @return a query for the given URLs.
      */
    @SuppressWarnings("SameParameterValue")
    private String urlQueryJoin(String field, String operator, Iterable<String> urls) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append(field).append(":(");
        for (String url : urls) {
            if (url.startsWith("data:") ) {
                 continue;
             }
            if (!first) {
                sb.append(" ").append(operator).append(" ");
            }
            first = false;
            sb.append(SolrUtils.createPhrase(normalizeUrl(url)));
        }
        sb.append(")");
        return sb.toString();
    }

    
    /*
     * Notice here do we not fix url_norm
     */
    public IndexDoc findClosestHarvestTimeForUrl(String url, String timeStamp) throws Exception {

        if (url == null || timeStamp == null) {
            throw new IllegalArgumentException("harvestUrl or timeStamp is null"); // Can happen for url-rewrites that are not corrected
        }
        //log.info("sort time:"+timeStamp + " url:"+url);
        // normalize will remove last slash if not slashpage
        boolean slashLast = url.endsWith("/");

        String urlNormQuery = UrlUtils.fixLegacyNormaliseUrlErrorQuery(url);                
        
        String query = urlNormQuery +" AND status_code:200"; //Maybe also allow 400 and 404?: (status_code:200 OR status_code:400 OR status_code:404).          
        
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);

        solrQuery.setFilterQueries(SolrUtils.NO_REVISIT_FILTER); // No binary for revists.

        solrQuery.set("facet", "false"); // very important. Must overwrite to false. Facets are very slow and expensive.
        solrQuery.add("sort", "abs(sub(ms(" + timeStamp + "), crawl_date)) asc");
        solrQuery.add("fl", SolrUtils.indexDocFieldList);
        // solrQuery.setRows(1);
        // code below is temporary fix for the solr bug. Get the nearest and find which
        // one is nearest.
        // The solr sort is bugged when timestamps are close. The bug is also present in
        // other methods in this class, but not as critical there.
        // Hoping for a solr fix....
        solrQuery.setRows(10);
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = loggedSolrQuery(
                String.format("findClosestHarvestTimeForUrl(url='%s', timestamp=%s)", url.length() > 50 ? url.substring(0, 50) + "..." : url, timeStamp),
                solrQuery);

        SolrDocumentList docs = rsp.getResults();
        if (docs == null || docs.size() == 0) {
            return null;
        }
        ArrayList<IndexDoc> indexDocs = SolrUtils.solrDocList2IndexDoc(docs);

        // Return the one nearest
        int bestIndex = 0; // This would be correct if solr could sort correct.
        // Solr uses a precisionsStep you can define in schema.xml if you want precision
        // to seconds. But this is not done in warc-indexer 3.0 schema.
        // Instead we extract the top 10 and find the nearest but checking against all.

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        long inputCrawlDate = dateFormat.parse(timeStamp).getTime(); // From the input
        long bestMatchDifference = Long.MAX_VALUE;

        for (int i = 0; i < indexDocs.size(); i++) {

            IndexDoc doc = indexDocs.get(i);
            boolean docHasSlashLast = doc.getUrl().endsWith("/");
            // small hack to make sure http/https not are mixed. Protocol is not into the
            // schema yet. Would be nice if protocol was a field in schema
            if ((url.startsWith("http://") && doc.getUrl().startsWith("http://")) || (url.startsWith("https://") && doc.getUrl().startsWith("https://"))) {
                // log.info("same protocol:"+url + ":"+doc.getUrl());
            } else {
                // Not a problem just need to see how often it happens for now.
                // log.info("Same url has been harvests for both HTTP and HTTPS: "+url + " and
                // "+doc.getUrl());
                continue; // Skip
            }

            if (slashLast && !docHasSlashLast) { // url_norm will be same with and without / last. But they are different pages
                log.info("Ignoring URL due to '/' as end of url:" + url + " found:" + doc.getUrl_norm());
                continue;
            }

            // If redirect, do not return the same url as this will give endless redirect.
            // This can happen due to the http://www.test.dk http://test.dk is normalized to
            // the same.
            if (doc.getStatusCode() >= 300 && doc.getStatusCode() < 400) {
                if (doc.getUrl().equals(url)) { // Do not return the same for redirect.
                    log.info("Stopping endless direct for url:" + url + " and found url:" + doc.getUrl());
                    continue; // skip
                }
            }

            String crawlDateDoc = doc.getCrawlDate();
            long crawlDateForDocument = dateFormat.parse(crawlDateDoc).getTime(); // For this document
            long thisMatch = Math.abs(inputCrawlDate - crawlDateForDocument);
            if (thisMatch < bestMatchDifference) {
                bestIndex = i;
                bestMatchDifference = thisMatch;
            }
        }

        if (bestIndex != 0) {
            log.warn("Fixed Solr time sort bug, found a better match, # result:" + bestIndex);
        }
        return indexDocs.get(bestIndex);
    }

    /**
     * Build the query for ngram
     */
    public SolrQuery buildSolrQueryForPeriod(String query, String startDate, String endDate) {
        log.info("query between " + startDate + "T00:00:00Z and " + endDate + "T23:59:59Z");
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query); //Smurf labs forces text:query
        solrQuery.setRows(0); // 1 page only
        solrQuery.add("fl", "id");// rows are 0 anyway
        solrQuery.add("fq","content_type_norm:html"); // only html pages
        solrQuery.add("fq","crawl_date:[" + startDate + "T00:00:00Z TO " + endDate + "T23:59:59Z]");
        return solrQuery;
    }

    /**
     * Returns the number of documents for the ngram
     */
    public Long countTextHtmlForPeriod(String query, String startDate, String endDate) throws Exception {
        SolrQuery solrQuery = buildSolrQueryForPeriod(query, startDate, endDate);
        solrQuery.add("fq", SolrUtils.NO_REVISIT_FILTER); // do not include record_type:revisit
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
        return rsp.getResults().getNumFound();
    }

    public Long countTagHtmlForPeriod(String query, String startDate, String endDate) throws Exception {
        if (!TAGS_VALID_PATTERN.matcher(query).matches()) {
            throw new InvalidArgumentServiceException("Tag syntax not accepted:" + query);
        }
        SolrQuery solrQuery = buildSolrQueryForPeriod("elements_used:\"" + query + "\"", startDate, endDate);
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
        return rsp.getResults().getNumFound();
    }

    // Not used anymore
    public HashMap<Integer, Long> getYearHtmlFacets(String query) throws Exception {
        // facet=true&facet.field=crawl_year&facet.sort=index&facet.limit=500
        if (!TAGS_VALID_PATTERN.matcher(query).matches()) {
            throw new InvalidArgumentServiceException("Tag syntax not accepted:" + query);
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("elements_used:\"" + query + "\"");
        solrQuery.setFilterQueries("content_type_norm:html"); // only html pages
        solrQuery.setRows(0); // 1 page only
        solrQuery.add("fl", "id");// rows are 0 anyway
        solrQuery.set("facet", "true");
        solrQuery.set("facet.field", "crawl_year");
        solrQuery.set("facet.sort", "index");
        solrQuery.set("facet.limit", "500"); // 500 is higher than number of different years
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);

        FacetField facetField = rsp.getFacetField("crawl_year");

        HashMap<Integer, Long> allCount = new HashMap<Integer, Long>();

        for (FacetField.Count c : facetField.getValues()) {
            allCount.put(Integer.parseInt(c.getName()), c.getCount());
        }
        return allCount;
    }

    public IndexDoc findExactMatchPWID(String url, String utc) throws Exception {

        String url_norm = normalizeUrl(url);
        String pwidQuery = "url_norm:\"" + url_norm + "\" AND crawl_date:\"" + utc + "\"";
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(pwidQuery);
        solrQuery.setRows(1); // 1 page only

        solrQuery.add("fl", SolrUtils.indexDocFieldList);
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = loggedSolrQuery("pwidQuery", solrQuery);

        SolrDocumentList docs = rsp.getResults();
        if (docs.size() == 0) {
            return null;
        }

        IndexDoc indexDoc = SolrUtils.solrDocument2IndexDoc(docs.get(0));
        return indexDoc;
    }
    
    // Not used anymore
    public HashMap<Integer, Long> getYearFacetsHtmlAll() throws Exception {
        // facet=true&facet.field=crawl_year&facet.sort=index&facet.limit=500

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        solrQuery.setRows(0); // 1 page only
        solrQuery.add("fl", "id");// rows are 0 anyway
        solrQuery.set("facet", "true");
        solrQuery.set("facet.field", "crawl_year");
        solrQuery.set("facet.sort", "index");
        solrQuery.set("facet.limit", "500"); // 500 is higher than number of different years

        solrQuery.add("fq","content_type_norm:html"); // only html pages
        solrQuery.add("fq", SolrUtils.NO_REVISIT_FILTER); // do not include record_type:revisit
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);

        FacetField facetField = rsp.getFacetField("crawl_year");

        HashMap<Integer, Long> allCount = new HashMap<Integer, Long>();

        for (FacetField.Count c : facetField.getValues()) {
            allCount.put(Integer.parseInt(c.getName()), c.getCount());
        }
        return allCount;
    }

    // Not used anymore
    public HashMap<Integer, Long> getYearTextHtmlFacets(String query) throws Exception {
        // facet=true&facet.field=crawl_year&facet.sort=index&facet.limit=500

        /*
         * if (!OK.matcher(query).matches()) { throw new
         * InvalidArgumentServiceException("Tag syntax not accepted:"+query); }
         */

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query); //Smurf labs forces text:query               
        solrQuery.setRows(0); // 1 page only
        solrQuery.add("fl", "id");// rows are 0 anyway
        solrQuery.set("facet", "true");
        solrQuery.set("facet.field", "crawl_year");
        solrQuery.set("facet.sort", "index");
        solrQuery.set("facet.limit", "500"); // 500 is higher than number of different years

        solrQuery.add("fq","content_type_norm:html"); // only html pages
        solrQuery.add("fq", SolrUtils.NO_REVISIT_FILTER); // do not include record_type:revisit
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);

        FacetField facetField = rsp.getFacetField("crawl_year");

        HashMap<Integer, Long> allCount = new HashMap<Integer, Long>();

        for (FacetField.Count c : facetField.getValues()) {
            allCount.put(Integer.parseInt(c.getName()), c.getCount());
        }
        return allCount;
    }

    public ArrayList<IndexDoc> findNearestForResourceNameAndDomain(String domain, String resourcename, String timeStamp) throws Exception {
        String searchString = "domain:\"" + domain + "\" AND resourcename:\"" + resourcename + "\"";
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(searchString);
        solrQuery.set("facet", "false");
        solrQuery.set("group", "true");
        solrQuery.set("group.field", "domain");
        solrQuery.set("group.size", "10");
        solrQuery.set("group.sort", "abs(sub(ms(" + timeStamp + "), crawl_date)) asc");
        solrQuery.add("fl", SolrUtils.indexDocFieldList);
        solrQuery.setFilterQueries(SolrUtils.NO_REVISIT_FILTER); // No binary for revists.
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
        SolrDocumentList docs = groupsToDoc(rsp);
        return SolrUtils.solrDocList2IndexDoc(docs);
    }

    public String searchJsonResponseOnlyFacets(String query, List<String> fq, boolean revisits) throws Exception {
        log.info("Solr query(only facets): " + query + " fg:" + fq + "revisits:" + revisits);

        SolrQuery solrQuery = new SolrQuery();

        // Build all query params in map

        solrQuery.set("rows", "0"); // Only facets
        solrQuery.set("q", query);
        solrQuery.set("fl", "id");
        solrQuery.set("wt", "json");
        solrQuery.set("hl", "off");
        solrQuery.set("q.op", "AND");
        solrQuery.set("indent", "true");
        solrQuery.set("f.crawl_year.facet.limit", "100"); // Show all crawl_years. Maybe remove limit to property file as well
        solrQuery.set("f.crawl_year.facet.sort", "index"); // Sort by year and not count.

        if (!revisits) {
            solrQuery.add("fq", SolrUtils.NO_REVISIT_FILTER); // do not include record_type:revisit
        }
        if (fq != null) {
            for (String filter : fq) {
                solrQuery.add("fq", filter);
            }
        }
        if (!PropertiesLoaderWeb.FACETS.isEmpty()) {
            solrQuery.set("facet", "true");
            for (String facet : PropertiesLoaderWeb.FACETS) {
                solrQuery.add("facet.field", facet);
            }
        }

        SolrUtils.setSolrParams(solrQuery);
        
        NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
        rawJsonResponseParser.setWriterType("json");

        QueryRequest req = new QueryRequest(solrQuery);
        req.setResponseParser(rawJsonResponseParser);
        
        NamedList<Object> resp = solrServer.request(req);        
        String jsonResponse = (String) resp.get("response");
        return jsonResponse;
    }

    public String searchJsonResponseOnlyFacetsLoadMore( String query, List<String> fq, String facetField, boolean revisits) throws Exception {
        log.info("Solr query(load more from facet): "+query +" fg:"+fq+ " revisits:"+revisits +" facetField:"+facetField);

        if (!PropertiesLoaderWeb.FACETS.contains(facetField)){
            throw new IllegalArgumentException("Facet not allowed on field:"+facetField);
        }

        SolrQuery solrQuery = new SolrQuery();

        //Build all query params in map

        solrQuery.set("rows", "0"); //Only facets
        solrQuery.set("q", query);
        solrQuery.set("fl", "id");
        solrQuery.set("wt", "json");
        solrQuery.set("hl", "off");
        solrQuery.set("q.op", "AND");
        solrQuery.set("indent", "true");
        solrQuery.add("facet.field", facetField);
        solrQuery.set("facet", "true");
        solrQuery.set("f."+facetField+".facet.limit", "50"); //just hardcode for now



        if (!revisits){
            solrQuery.set("fq", SolrUtils.NO_REVISIT_FILTER); // do not include record_type:revisit
        }
        if ( fq != null) {
            for (String filter : fq) {
                solrQuery.add("fq",filter);
            }
        }

        
        SolrUtils.setSolrParams(solrQuery);
        
        NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
        rawJsonResponseParser.setWriterType("json");

        QueryRequest req = new QueryRequest(solrQuery);
        req.setResponseParser(rawJsonResponseParser);
        
        NamedList<Object> resp = solrServer.request(req);
        String jsonResponse = (String) resp.get("response");
        return jsonResponse;
    }

    public String searchJsonResponseNoFacets(String query, List<String> fq, boolean grouping, boolean revisits, Integer start, String sort) throws Exception {
        log.info("SolrQuery (no facets):" + query + " grouping:" + grouping + " revisits:" + revisits + " start:" + start);

        String startStr = "0";
        if (start != null) {
            startStr = start.toString();
        }

        SolrQuery solrQuery = new SolrQuery();

        // Build all query params in map

        solrQuery.set("rows", PropertiesLoaderWeb.SEARCH_PAGINATION.toString());
        solrQuery.set("start", startStr);
        solrQuery.set("q", query);
        solrQuery.set("fl", "id,score,title,hash,source_file_path,source_file_offset,url,url_norm,wayback_date,domain,content_type,crawl_date,content_type_norm,type, collection,collection_id");
        solrQuery.set("wt", "json");
        solrQuery.set("hl", "on");
        solrQuery.set("q.op", "AND");
        solrQuery.set("indent", "true");
        solrQuery.set("facet", "false"); // No facets!
        if (sort != null && !"".equals(sort)) {
            solrQuery.set("sort", sort);
        }

        if (grouping) {
            // Both group and stats must be enabled at same time
            solrQuery.set("group", "true");
            solrQuery.set("group.field", "url");
            solrQuery.set("stats", "true");
            solrQuery.set("stats.field", "{!cardinality=0.5}url");
            solrQuery.set("group.format", "simple");
            solrQuery.set("group.limit", "1");
        }

        if (!revisits) {
            solrQuery.set("fq", SolrUtils.NO_REVISIT_FILTER); // do not include record_type:revisit
        }
        if (fq != null) {
            for (String filter : fq) {
                solrQuery.add("fq", filter);
            }
        }
       
        SolrUtils.setSolrParams(solrQuery);
        
        NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
        rawJsonResponseParser.setWriterType("json");

        QueryRequest req = new QueryRequest(solrQuery);
        req.setResponseParser(rawJsonResponseParser);

        NamedList<Object> resp = solrServer.request(req);
        String jsonResponse = (String) resp.get("response");
        return jsonResponse;
    }

    /*
     * field list is a comma seperated list of fields. If null all fields will loaded
     * 
     */
    public String idLookupResponse(String id, String fieldList) throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.set("rows", "1");
        solrQuery.set("q", "id:\"" + id + "\"");
        solrQuery.set("wt", "json");
        solrQuery.set("q.op", "AND");
        solrQuery.set("indent", "true");
        solrQuery.set("facet", "false");
        
        if (fieldList!= null) {
          solrQuery.set("fl",fieldList);        
        }
        
        NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
        rawJsonResponseParser.setWriterType("json");

        QueryRequest req = new QueryRequest(solrQuery);
        req.setResponseParser(rawJsonResponseParser);
        SolrUtils.setSolrParams(solrQuery);
        NamedList<Object> resp = solrServer.request(req);
        String jsonResponse = (String) resp.get("response");
        return jsonResponse;
    }

    /*
     * Uses the stats component and hyperloglog for ultra fast performance instead
     * of grouping, which does not work well over many shards.
     * <p>
     * Extract statistics for a given domain and year. Number of unique pages (very
     * precise due to hyperloglog) Number of ingoing links (very precise due to
     * hyperloglog) Total size (of the unique pages). (not so precise due, tests
     * show max 10% error, less for if there are many pages)
     */
    public DomainStatistics domainStatistics(String domain, String startDate, String endDate) throws Exception {

        DomainStatistics stats = new DomainStatistics();
        stats.setDate(startDate);
        stats.setDomain(domain);

        String searchString = "domain:\"" + domain + "\"";

        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setQuery(searchString);
        solrQuery.set("facet", "false");
        solrQuery.addFilterQuery("content_type_norm:html AND status_code:200");
        solrQuery.addFilterQuery("crawl_date:[" + startDate + "T00:00:00Z TO " + endDate + "T23:59:59Z]");
        solrQuery.setRows(0);
        solrQuery.add("fl", "id");
        solrQuery.add("stats", "true");
        solrQuery.add("stats.field", "{!count=true cardinality=true}url_norm"); // Important, use cardinality and not unique.
        solrQuery.add("stats.field", "{!sum=true}content_length");
        SolrUtils.setSolrParams(solrQuery);
        QueryResponse rsp = solrServer.query(solrQuery);

        Map<String, FieldStatsInfo> statsMap = rsp.getFieldStatsInfo();
        FieldStatsInfo statsUrl_norm = statsMap.get("url_norm");
        long url_norm_cardinality = statsUrl_norm.getCardinality();
        long url_norm_total = statsUrl_norm.getCount();

        FieldStatsInfo statsContent_length = statsMap.get("content_length");
        Double sum = (Double) statsContent_length.getSum();

        // estimate content_length for the uniqie pages by fraction of total.
        double size = sum * (url_norm_cardinality * 1d / url_norm_total) * 1d / 1024d;
        stats.setSizeInKb((int) size);
        stats.setTotalPages((int) url_norm_cardinality);

        // Links
        solrQuery = new SolrQuery();
        solrQuery.setQuery("links_domains:\"" + domain + "\" -" + searchString); // links to, but not from same domain
        solrQuery.addFilterQuery("content_type_norm:html AND status_code:200");
        solrQuery.addFilterQuery("crawl_date:[" + startDate + "T00:00:00Z TO " + endDate + "T23:59:59Z]");
        solrQuery.setRows(0);
        solrQuery.add("stats", "true");
        solrQuery.add("fl", "id");
        solrQuery.add("stats.field", "{!cardinality=true}domain"); // Important, use cardinality and not unique.

        rsp = solrServer.query(solrQuery);
        Map<String, FieldStatsInfo> stats2 = rsp.getFieldStatsInfo();

        FieldStatsInfo statsLinks = stats2.get("domain");
        long links_cardinality = statsLinks.getCardinality();
        stats.setIngoingLinks((int) links_cardinality);
        return stats;
    }

    /*
     * Domain statistics for query by year (not used anymore)
     */
    public String domainStatisticsForQuery(String query, List<String> fq) throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        solrQuery.set("facet", "false");
        
        // default scale (by year)
        int startYear = PropertiesLoaderWeb.ARCHIVE_START_YEAR;
        int endYear = LocalDate.now().getYear() + 1; // add one since it is not incluced

        solrQuery.setParam("json.facet",
            "{domains:{type:terms,field:domain,limit:30,facet:{years:{type:range,field:crawl_year,start:" + startYear + ",end:" + endYear + ",gap:1}}}}");

        for (String filter : fq) {
            solrQuery.addFilterQuery(filter);
        }
        SolrUtils.setSolrParams(solrQuery); //TODO not sure about this one
        NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
        rawJsonResponseParser.setWriterType("json");

        QueryRequest req = new QueryRequest(solrQuery);
        req.setResponseParser(rawJsonResponseParser);

        NamedList<Object> resp = solrServer.request(req);
        String jsonResponse = (String) resp.get("response");
        return jsonResponse;
    }

    // returns JSON. Response not supported by SolrJ
    /*
     * Example query:
     * by year
     * curl -s -d 'q=demokrati&rows=0&json.facet={domains:{type:terms,field:domain,limit:100 facet:{years:{type:range,field:crawl_year,start:2000,end:2020,gap:1}}}}' 'http://localhost:52300/solr/ns/select' > demokrati.json
     * by month
     * curl -s -d 'q=demokrati&rows=0&json.facet={domains:{type:terms,field:domain,limit:100 facet:{years:{type:range,field:crawl_date,start:'2000-01-01T00:00:00Z',end:'2001-01-01T23:59:59Z',gap:'+1MONTH'}}}}' 'http://localhost:52300/solr/ns/select' > demokrati.json
     */
    public String domainStatisticsForQuery(String query, List<String> fq, String startdate, String enddate, String scale) throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.setRows(0);
        solrQuery.set("facet", "false");

        // custom scale
        String start = startdate + "T00:00:00Z";
        String end = enddate + "T23:59:59Z";
        String gap = getGapFromScale(scale);
        solrQuery.setParam("json.facet",
            "{domains:{type:terms,field:domain,limit:30,facet:{years:{type:range,field:crawl_date,start:'"+ start + "',end:'"+ end + "',gap:'"+ gap + "'}}}}");
        solrQuery.addFilterQuery("crawl_date:[" + start + " TO " + end + "]");

        for (String filter : fq) {
            solrQuery.addFilterQuery(filter);
        }
        SolrUtils.setSolrParams(solrQuery); //TODO not sure about this one
        NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
        rawJsonResponseParser.setWriterType("json");

        QueryRequest req = new QueryRequest(solrQuery);
        req.setResponseParser(rawJsonResponseParser);

        NamedList<Object> resp = solrServer.request(req);
        String jsonResponse = (String) resp.get("response");
        return jsonResponse;
    }

    /**
     * Determine the gap for Solr from the time scale
     * 
     * @param scale the time scale
     * @return the gap
     */
    private static String getGapFromScale(String scale) {
        String gap = "";
        switch (scale) {
            case "MONTH" :
                gap = "+1MONTH";
                break;
            case "WEEK" :
                gap = "+7DAYS";
                break;
            case "DAY" :
                gap = "+1DAY";
                break;
            case "YEAR" :
            default :
                gap = "+1YEAR";
                break;
        }
        return gap;
    }

    // TO, remove method and inline
    public static long getOffset(SolrDocument doc) {
        return (Long) doc.get("source_file_offset");
    }

    private static String normalizeUrl(String url) {               
        return Normalisation.canonicaliseURL(url);
    }


    /**
     * Performs a Solr call, logging the time it took; both measured and reported
     * QTime.
     *
     * @param caller    the method or logical entity that issued the call. This will
     *                  be part of the log entry.
     * @param solrQuery the query to issue.
     * @return the result of the query.
     * @throws SolrServerException if the query failed.
     * @throws IOException         if the query failed.
     */
    private QueryResponse loggedSolrQuery(String caller, SolrQuery solrQuery) throws SolrServerException, IOException {
        long solrNS = -System.nanoTime();
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
        solrNS += System.nanoTime();
        String query = solrQuery.getQuery();
        query = query == null ? null : query.length() > 200 ? query.substring(0, 200) + "..." : query;
/*
        log.debug(String.format("%s Solr response in %d ms (qtime=%d ms) with %d hits for query %s", caller, solrNS / M, rsp.getQTime(),
                rsp.getResults().getNumFound(), query));
  */
        return rsp;
    }

    /**
     * Sets property defined query parameters with {@link SolrUtils#setSolrParams(SolrQuery)} and issues the query.
     * <p>
     * Exceptions are caught and re-thrown as {@link RuntimeException}s.
     * @param solrQuery any SolrQuery.
     * @param useCachingClient if true, client side caching is used. If false, no client side caching is done.
     * @return the result of issuing the query.
     */
    public static QueryResponse query(SolrQuery solrQuery, boolean useCachingClient) {
        SolrUtils.setSolrParams(solrQuery);
        try {
            return useCachingClient ?
                    solrServer.query(solrQuery, METHOD.POST) :
                    noCacheSolrServer.query(solrQuery, METHOD.POST);
        } catch (SolrServerException e) {
            throw new RuntimeException("SolrServerException issuing query", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException issuing query", e);
        }
    }

    /**
     * @return the number of attempts for resolving an URL leniently with extended argument query.
     */
    public long getLenientAttempts() {
        return lenientAttempts.get();
    }
    
    /**
     * @return the number of successful attempts for resolving an URL leniently with extended argument query.
     */
    public long getLenientSuccesses() {
        return lenientSuccesses.get();
    }
}
