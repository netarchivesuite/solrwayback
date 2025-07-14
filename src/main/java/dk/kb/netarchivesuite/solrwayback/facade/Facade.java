package dk.kb.netarchivesuite.solrwayback.facade;

import dk.kb.netarchivesuite.solrwayback.concurrency.ImageSearchExecutor;
import dk.kb.netarchivesuite.solrwayback.export.ContentStreams;
import dk.kb.netarchivesuite.solrwayback.export.StreamingRawZipExport;
import dk.kb.netarchivesuite.solrwayback.export.StreamingSolrExportBufferedInputStream;
import dk.kb.netarchivesuite.solrwayback.export.StreamingSolrWarcExportBufferedInputStream;
import dk.kb.netarchivesuite.solrwayback.parsers.ArcParserFileResolver;
import dk.kb.netarchivesuite.solrwayback.parsers.DomainStatisticsForDomainParser;
import dk.kb.netarchivesuite.solrwayback.parsers.HtmlParserUrlRewriter;
import dk.kb.netarchivesuite.solrwayback.playback.CssPlayback;
import dk.kb.netarchivesuite.solrwayback.playback.HtmlPlayback;
import dk.kb.netarchivesuite.solrwayback.playback.JavascriptPlayback;
import dk.kb.netarchivesuite.solrwayback.playback.JodelPlayback;
import dk.kb.netarchivesuite.solrwayback.playback.TwitterPlayback;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.FacetCount;
import dk.kb.netarchivesuite.solrwayback.service.dto.HarvestDates;
import dk.kb.netarchivesuite.solrwayback.service.dto.ImageUrl;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.PagePreview;
import dk.kb.netarchivesuite.solrwayback.service.dto.PageResource;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.service.dto.TimestampsForPage;
import dk.kb.netarchivesuite.solrwayback.service.dto.WordCloudWordAndCount;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.D3Graph;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.Link;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.Node;
import dk.kb.netarchivesuite.solrwayback.service.dto.smurf.SmurfBuckets;
import dk.kb.netarchivesuite.solrwayback.service.dto.statistics.DomainStatistics;
import dk.kb.netarchivesuite.solrwayback.service.dto.statistics.QueryPercentilesStatistics;
import dk.kb.netarchivesuite.solrwayback.service.dto.statistics.QueryStatistics;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.NotFoundServiceException;
import dk.kb.netarchivesuite.solrwayback.smurf.SmurfUtil;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamDirect;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStats;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamDecorators;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamingExportClient;
import dk.kb.netarchivesuite.solrwayback.solr.SolrStreamingLinkGraphCSVExportClient;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import dk.kb.netarchivesuite.solrwayback.util.FileUtil;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import dk.kb.netarchivesuite.solrwayback.util.UrlUtils;
import dk.kb.netarchivesuite.solrwayback.wordcloud.WordCloudImageGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.ws.rs.core.StreamingOutput;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.kb.netarchivesuite.solrwayback.util.InputStreamUtils.getStringFromInputStream;

public class Facade {
    private static final Logger log = LoggerFactory.getLogger(Facade.class);

    /**
     * Search the Solr index for the given text with the filter query applied.
     * @param searchText the text to search for.
     * @param filterQuery the filter query to apply, e.g. "content_type_norm:html".
     * @return a SearchResult DTO containing the results of the search.
     */
    public static SearchResult search(String searchText, String filterQuery) throws Exception {
        SearchResult result = NetarchiveSolrClient.getInstance().search(searchText, filterQuery);
        return result;
    }

    /**
     * Search the Solr index for the given query with the filter queries applied and posibility to define
     * other parameters such as start, sort and grouping.
     * @param query the query to search for.
     * @param filterQueries the filter queries to apply, e.g. "content_type_norm:html".
     * @param grouping if true, grouping will be applied to the search.
     * @param revisits if true, revisits will be included in the search.
     * @param start the starting point for the search results, used for pagination. Cannot be greater than 1000.
     * @param sort the sort order for the search results.
     * @return a JSON formatted solr response.
     */
    public static String solrSearchNoFacets(String query, List<String> filterQueries, boolean grouping, boolean revisits, int start, String sort) throws Exception {
        if (start >= 1001) {
            throw new InvalidArgumentServiceException("Pagination (start) must be less than 1001");
        }
        return proxySolrNoFacets(query, filterQueries, grouping, revisits, start, sort);
    }

    /**
     * Search the Solr index for the given query and only return facets.
     * @param query the query to search for.
     * @param filterQueries the filter queries to apply, e.g. "content_type_norm:html".
     * @param revisits if true, revisits will be included in the search.
     * @return a JSON formatted solr response containing only facets.
     */
    public static String solrSearchFacetsOnly(String query, List<String> filterQueries, boolean revisits) throws Exception {
        return proxySolrOnlyFacets(query, filterQueries, revisits);
    }

    public static String solrSearchFacetsOnlyLoadMore(String query, List<String> filterQueries, String facetField, boolean revisits) throws Exception {
        return proxySolrOnlyFacetsLoadMore(query, filterQueries, facetField, revisits);
    }

    /*
     * If fieldList is null all fields will be loaded
     * returns json
     */
    public static String solrIdLookup(String id) throws Exception {
        return NetarchiveSolrClient.getInstance().idLookupResponse(id,PropertiesLoaderWeb.FIELDS);
    }

    /*
    //TODO limit fields allowed 
    public  static String getRawSolrQuery(String query,List<String> fq,String fieldList, int rows, int startRow, HashMap<String,String> rawQueryParams)  throws Exception{     
        return NetarchiveSolrClient.getInstance().getRawSolrQuery(query, fq, fieldList, rows, startRow, rawQueryParams);            
    }
    */
    
    public static IndexDoc findExactMatchPWID(String url, String utc) throws Exception {
        IndexDoc doc = NetarchiveSolrClient.getInstance().findExactMatchPWID(url, utc);
        return doc;
    }

    /**
     * Get the "AboutText" for the SolrWayback service.
     * This message is what is shown in the bottom "About" section of the web interface.
     * @return the "AboutText" as a String.
     * @throws Exception if there is an error reading the file.
     */
    public static String getAboutText() throws Exception {
        String aboutFile = PropertiesLoaderWeb.ABOUT_TEXT_FILE;
        return FileUtil.fetchUTF8(aboutFile);
    }

    /**
     * Get the "SearchHelpText" for the SolrWayback service.
     * This message is what is shown in the "Search Help" section of the web interface represented by a magnifying glass
     * besides the query input field.
     * @return the "SearchHelpText" as a String.
     * @throws Exception if there is an error reading the file.
     */
    public static String getSearchHelpText() throws Exception {
        String searchHelpFile = PropertiesLoaderWeb.SEARCH_HELP_TEXT_FILE;
        return FileUtil.fetchUTF8(searchHelpFile);
    }

    /**
     * Get the "CollectionText" for the SolrWayback service.
     * This message is what is shown in the "About this collection" section of the web interface where users can get
     * information on how the collection was created, what it contains, etc.
     * @return the "CollectionText" as a String.
     * @throws Exception if there is an error reading the file.
     */
    public static String getCollectionText() throws Exception {
        String collectionFile = PropertiesLoaderWeb.COLLECTION_TEXT_FILE;
        return FileUtil.fetchUTF8(collectionFile);
    }

    public static String generateDomainResultGraph(String q, List<String> fq, String startdate, String enddate, String scale) throws Exception {
        Map<String, List<FacetCount>> domainStatisticsForQuery;
        LocalDate start = LocalDate.parse(startdate, DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse(enddate, DateTimeFormatter.ISO_DATE);
        LocalDate endOfFirstPeriod = DateUtils.getEndOfFirstPeriod(start, scale);
        if (!endOfFirstPeriod.isBefore(end)) {
            // only one query is needed
            String json = NetarchiveSolrClient.getInstance().domainStatisticsForQuery(q, fq, startdate, end.toString(), scale);
            domainStatisticsForQuery = DomainStatisticsForDomainParser.parseDomainStatisticsJson(json);
        } else {
            // 2 queries : the first until the end of the month/year, the second for the rest of the period
            // first period
            String jsonFirstPeriod = NetarchiveSolrClient.getInstance().domainStatisticsForQuery(q, fq, startdate, endOfFirstPeriod.minusDays(1).toString(), scale);
            Map<String, List<FacetCount>> firstPeriodMap = DomainStatisticsForDomainParser.parseDomainStatisticsJson(jsonFirstPeriod);
            // rest of the periods
            String jsonStrRestOfPeriods = NetarchiveSolrClient.getInstance().domainStatisticsForQuery(q, fq, endOfFirstPeriod.toString(), enddate, scale);
            Map<String, List<FacetCount>> restOfPeriodsMap = DomainStatisticsForDomainParser.parseDomainStatisticsJson(jsonStrRestOfPeriods);
            
            // merge the two stats
            domainStatisticsForQuery = restOfPeriodsMap;
            // firstPeriodMap has only one entry max
            if (!firstPeriodMap.isEmpty()){
                domainStatisticsForQuery.put(start.toString(), firstPeriodMap.get(startdate));
            }
        }
        List<Pair<LocalDate, LocalDate>> periods = DateUtils.calculatePeriods(start, end, scale);
        List<String> dates = periods.stream().map(p -> p.first().toString()).collect(Collectors.toList());
        String matrix = DomainStatisticsForDomainParser.generateDomainQueryStatisticsString(domainStatisticsForQuery, dates);
        return matrix;
    }

    /**
     * Search images both directly and through webpages.
     * Delegates to {@link ContentStreams#findImages(boolean, boolean, int, String, String...)}.
     * @param query Solr query.
     * @param filterQueries 0 or more Solr filter queries.
     * @return up to 500 images matching the searchText.
     * @see Facade#exportImages(boolean, boolean, String, String...)
     */
    public static ArrayList<ArcEntryDescriptor> findImages(String query, String... filterQueries) {
        long searchTimeMS = -System.currentTimeMillis();
        // TODO: This has goFast and will not be accurate. Should it be an option?
        ArrayList<ArcEntryDescriptor> images = ContentStreams.findImages(true, true, 50, query, filterQueries).
                        limit(500).
                        map(SolrUtils::solrDocument2ArcEntryDescriptor).
                        collect(Collectors.toCollection(ArrayList::new));
        searchTimeMS += System.currentTimeMillis();
        log.debug("Found at least {} images in {}ms ({} images/second), searching for '{}'",
                  images.size(), searchTimeMS, searchTimeMS == 0 ? "N/A" : (images.size()*1000/searchTimeMS), query);
        return images;
    }
    
    public static ArrayList<ArcEntryDescriptor> oldfindImages(String searchText) throws Exception {
        long start=System.currentTimeMillis();
        // only search these two types
        //Since the HTML boost split up in two searches to also get image hits. The image hits is a very fast search.

        //Images search are fast and also very accurate. But also search html pages for text and extract images. 400/100 seems a good split.
        SearchResult result1 = NetarchiveSolrClient.getInstance().search(searchText, "content_type_norm:image", 400); // only images.                 
        SearchResult result2 = NetarchiveSolrClient.getInstance().search(searchText, "content_type_norm:html", 100);  // Find images on page where text is found
        // multithreaded call solr to find arc file and offset        
        List<IndexDoc> bothResults = result1.getResults();
        bothResults.addAll(result2.getResults());
        ArrayList<ArcEntryDescriptor> extractImages = ImageSearchExecutor.extractImages(bothResults);
        log.info("Image search for query: "+searchText +" took "+(System.currentTimeMillis()-start) +"millis)");
        return extractImages;
    }

    /**
     * Get statistics for a specific domain.
     * @param domain the domain to get statistics for, e.g. "example.com".
     * @param start the start date for the statistics, in ISO format (YYYY-MM-DD).
     * @param end the end date for the statistics, in ISO format (YYYY-MM-DD).
     * @param scale the time scale for the statistics, e.g. "day", "month", "year".
     * @return a List of DomainStatistics objects containing the statistics for the domain.
     */
    public static List<DomainStatistics> statisticsDomain(String domain, LocalDate start, LocalDate end, String scale) throws Exception {
        log.info("Statistics for domain: " + domain + ", startdate:" + start.toString() + ", enddate:" + end.toString() + ", timescale:" + scale);

        List<DomainStatistics> stats = new ArrayList<>();
        String dateStr = "";
        String nextDateStr = "";
        List<Pair<LocalDate, LocalDate>> periods = DateUtils.calculatePeriods(start, end, scale);
        for (Pair<LocalDate, LocalDate> period : periods) {
            dateStr = period.first().format(DateTimeFormatter.ISO_DATE);
            nextDateStr = period.second().format(DateTimeFormatter.ISO_DATE);
            
            DomainStatistics stat = NetarchiveSolrClient.getInstance().domainStatistics(domain, dateStr, nextDateStr);
            stats.add(stat);
        }
        return stats;
    }

    public static ArrayList<ImageUrl> imagesLocationSearch(String searchText, String filter, String results, double latitude, double longitude, double radius,
                                                           String sort) throws Exception {
        int resultInt = 500;
        if (results != null) {
            resultInt = Integer.parseInt(results);
        }
        ArrayList<IndexDoc> docs = NetarchiveSolrClient.getInstance().imagesLocationSearchWithSort(searchText, filter, resultInt, latitude, longitude, radius,
                sort); // only search these two types
        return indexDoc2Images(docs);
    }

    public static SmurfBuckets generateNetarchiveSmurfData(String tag, LocalDate start, LocalDate end, String scale) throws Exception {

        if (tag == null || tag.length() == 0) {
            throw new InvalidArgumentServiceException("tag must not be empty");
        }

        log.info("netarchive smurf tag query:" + tag + " for startdate:" + start.toString() + ", enddate:" + end.toString() + " timescale:" + scale);
        try {

            Map<LocalDate, Long> contentQuery = new HashMap<>();
            Map<LocalDate, Long> facetsAll = new HashMap<>();
            String dateStr = "";
            String nextDateStr = "";
            List<Pair<LocalDate, LocalDate>> periods = DateUtils.calculatePeriods(start, end, scale);
            for (Pair<LocalDate, LocalDate> period : periods) {
                LocalDate startP = period.first();
                LocalDate endP = period.second();
                dateStr = startP.format(DateTimeFormatter.ISO_DATE);
                nextDateStr = endP.format(DateTimeFormatter.ISO_DATE);
                
                Long count = NetarchiveSolrClient.getInstance().countTagHtmlForPeriod(tag, dateStr, nextDateStr);
                Long countAll = NetarchiveSolrClient.getInstance().countTextHtmlForPeriod("*:*", dateStr, nextDateStr);
                contentQuery.put(startP, count);
                facetsAll.put(startP, countAll);
            }

            SmurfBuckets buckets = SmurfUtil.generateBuckets(contentQuery, facetsAll, periods);
            return buckets;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    public static SmurfBuckets generateNetarchiveTextSmurfData(String query, LocalDate start, LocalDate end, String scale) throws Exception {

        if (query == null || query.length() == 0) {
            throw new InvalidArgumentServiceException("query must not be empty");
        }

        log.info("netarchive content smurf query:" + query + " for startdate:" + start.toString() + ", enddate:" + end.toString() + " timescale:" + scale);
        try {

            Map<LocalDate, Long> contentQuery = new HashMap<>();
            Map<LocalDate, Long> facetsAll = new HashMap<>();
            String dateStr = "";
            String nextDateStr = "";
            List<Pair<LocalDate, LocalDate>> periods = DateUtils.calculatePeriods(start, end, scale);
            for (Pair<LocalDate, LocalDate> period : periods) {
                LocalDate startP = period.first();
                LocalDate endP = period.second();
                dateStr = startP.format(DateTimeFormatter.ISO_DATE);
                nextDateStr = endP.format(DateTimeFormatter.ISO_DATE);
                
                Long count = NetarchiveSolrClient.getInstance().countTextHtmlForPeriod(query, dateStr, nextDateStr);
                Long countAll = NetarchiveSolrClient.getInstance().countTextHtmlForPeriod("*:*", dateStr, nextDateStr);
                contentQuery.put(startP, count);
                facetsAll.put(startP, countAll);
            }

            SmurfBuckets buckets = SmurfUtil.generateBuckets(contentQuery, facetsAll, periods);
            return buckets;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    public static BufferedImage getHtmlPagePreview(String source_file_path, long offset) throws Exception {

        String url = PropertiesLoader.WAYBACK_BASEURL + "services/view?source_file_path=" + source_file_path + "&offset=" + offset + "&showToolbar=false";
        long now = System.currentTimeMillis();

        // String filename =
        // PropertiesLoader.SCREENSHOT_TEMP_IMAGEDIR+source_file_path+"@"+offset+".png";
        // //Does not work since subfolders must be created before.
        // TODO implement caching for images?
        String filename = PropertiesLoader.SCREENSHOT_TEMP_IMAGEDIR + now + "_" + offset + ".png"; // Include offset to avoid hitting same time.
        String chromeCommand = PropertiesLoader.CHROME_COMMAND;

        log.info("Generating preview-image for url:" + url);

        ProcessBuilder pb = null;


        int timeoutMillis = PropertiesLoader.SCREENSHOT_PREVIEW_TIMEOUT * 1000;

//     pb = new ProcessBuilder(chromeCommand, "--headless" ,"--disable-gpu" ,"--ipc-connection-timeout=10000","--timeout="+timeoutMillis,"--screenshot="+filename,"--window-size=1280,1024","--proxy-server="+proxyUrl,  url);
        // no socks proxy
        pb = new ProcessBuilder(chromeCommand, "--headless", "--disable-gpu", "--ipc-connection-timeout=10000", "--timeout=" + timeoutMillis,
                "--screenshot=" + filename, "--window-size=1280,1024", url);

        log.info("Screenshot native command:"+chromeCommand + " --headless --disable-gpu --ipc-connection-timeout=10000 --timeout=" + timeoutMillis + " --screenshot=" + filename
                + " --window-size=1280,1024 " + url);
        // example: chromium-browser --headless --disable-gpu --ipc-connection-timeout=3000 --screenshot=test.png --window-size=1280,1024 http://google.com

        Process start = pb.start();
        // Due to a bug in chromium, the process can hang and never terminate. The
        // timeout is not working.. Also the screenshot will not be written to file.
        if (!start.waitFor(timeoutMillis + 5000, TimeUnit.MILLISECONDS)) { // timeout + 5 second before killing.
            // timeout - kill the process.
            log.info("Timeout generating preview. Due to bug chromium can hang.");
            start.destroyForcibly();
            throw new NotFoundServiceException("Timeout generating page preview"); // Just give a nice 404.
        }

        // return image even if timeout.
        InputStream is = start.getInputStream();
        String conlog = getStringFromInputStream(is);
//        log.debug("conlog:" + conlog); // No need to log this, can be spammy. But usefull when debugging
        BufferedImage image = ImageIO.read(new File(filename));
        return image;

    }

    /**
     * Get all harvest times for a specific URL.
     * @param url the URL to get harvest times for, e.g. "http://example.com".
     * @return a HarvestDates object containing the harvest times for the provided URL.
     */
    public static HarvestDates getHarvestTimesForUrl(String url) throws Exception {
        log.info("getting harvesttimes for url:" + url);
        HarvestDates datesVO = new HarvestDates();
        datesVO.setUrl(url);
        ArrayList<Date> dates = NetarchiveSolrClient.getInstance().getHarvestTimesForUrl(url);

        ArrayList<Long> crawltimes = new ArrayList<Long>(); // only YYYYMMDD part of day

        for (Date d : dates) {
            crawltimes.add(d.getTime());
        }

        datesVO.setDates(crawltimes);
        Collections.sort(crawltimes);

        datesVO.setNumberOfHarvests(crawltimes.size());
        return datesVO;
    }

    public static ArrayList<PagePreview> getPagePreviewsForUrl(int year,String url) throws Exception {
        log.info("getting pagePreview for year:"+year +" and url:" + url);

        ArrayList<IndexDoc> indexDocs = NetarchiveSolrClient.getInstance().getHarvestPreviewsForUrl(year,url); // Only contains the required fields for this method
        // Convert to PagePreview
        ArrayList<PagePreview> previews = new ArrayList<>();

        for (IndexDoc doc : indexDocs) {
            PagePreview pp = new PagePreview();
            pp.setCrawlDate(doc.getCrawlDateLong());
            String source_file_path = doc.getSource_file_path();
            long offset = doc.getOffset();
            String previewUrl = PropertiesLoader.WAYBACK_BASEURL + "services/image/pagepreview?source_file_path=" + source_file_path + "&offset=" + offset
                    + "&showToolbar=false";
            String solrWaybackUrl = PropertiesLoader.WAYBACK_BASEURL + "services/view?source_file_path=" + source_file_path + "&offset=" + offset;
            pp.setPagePreviewUrl(previewUrl);
            pp.setSolrWaybackUrl(solrWaybackUrl);
            previews.add(pp);
        }

        return previews;
    }


    public static ArrayList<FacetCount> getPagePreviewsYearInfo(String url) throws Exception {
        log.info("getting pagePreviewsinfo for url:" + url);

        ArrayList<FacetCount> facetCounts = NetarchiveSolrClient.getInstance().getPagePreviewsYearInfo(url);

        return facetCounts;
    }

    /*
     * Can be deleted when frontend has switched
     */
    public static BufferedImage wordCloudForDomain(String domain) throws Exception {
        log.info("getting wordcloud for url:" + domain);
        String query = "domain:\"" + domain + "\"";
        String text = NetarchiveSolrClient.getInstance().getConcatedTextFromHtmlForQuery(query,null); // Only contains the required fields for this method
        BufferedImage bufferedImage = WordCloudImageGenerator.wordCloudForDomain(text);

        return bufferedImage;
    }

    /**
     * Generate an image of a word cloud for the given query and filter query.
     * @param query the query to search for.
     * @param filterQuery the filter query to apply, e.g. "content_type_norm:html".
     * @return a BufferedImage containing the word cloud.
     */
    public static BufferedImage wordCloudForQuery(String query, String filterQuery) throws Exception {
        log.info("getting wordcloud for query:" + query +" filter query:"+filterQuery);
        String text = NetarchiveSolrClient.getInstance().getConcatedTextFromHtmlForQuery(query,filterQuery); // Only contains the required fields for this method
        BufferedImage bufferedImage = WordCloudImageGenerator.wordCloudForDomain(text);
        return bufferedImage;
    }

    public static  List<WordCloudWordAndCount> wordCloudWordFrequency(String query, String filterQuery) throws Exception {
        log.info("getting wordcloud frequency for query:" + query +" filterquery:"+filterQuery);
        String text = NetarchiveSolrClient.getInstance().getConcatedTextFromHtmlForQuery(query,filterQuery); // Only contains the required fields for this method

        List<WordCloudWordAndCount> wordCloudWordWithCount = WordCloudImageGenerator.wordCloudWordWithCount(text);
        return  wordCloudWordWithCount;
    }


    public static ArrayList<ImageUrl> getImagesForHtmlPageNew(String source_file_path, long offset) throws Exception {
        ArrayList<ArcEntryDescriptor> arcs = getImagesForHtmlPageNewThreaded(source_file_path, offset);

        return arcEntrys2Images(arcs);
    }

    /**
     * Find images on an HTML page through the following steps:
     * <ol>
     *     <li>Find the doc in solr from source_file_path and offset. (fast) </li>
     *     <li>Get image links field.</li>
     *     <li>For each images try to find that url_norm in solr with harvest time closest to the harvest time for the HTML page.</li>
     * </ol>
     *
     * @param source_file_path the path to the source file in the WARC collection, e.g. "crawl1/20100101000000/warc/part-00001.warc.gz".
     * @param offset the offset in the WARC file where the HTML page starts, e.g. 123456.
     * @return an ArrayList of ArcEntryDescriptor objects representing the location of the images found on the HTML page in the WARC collection.
     */
    public static ArrayList<ArcEntryDescriptor> getImagesForHtmlPageNewThreaded(String source_file_path, long offset) throws Exception {
        IndexDoc doc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);
        ArrayList<String> imageLinks = doc.getImageUrls();
        if (imageLinks.size() == 0) {
            return new ArrayList<>();
        }
        String queryStr = SolrUtils.createQueryStringForUrls(imageLinks);
        ArrayList<ArcEntryDescriptor> imagesFromHtmlPage = NetarchiveSolrClient.getInstance().findImagesForTimestamp(queryStr, doc.getCrawlDate());
        return imagesFromHtmlPage;
    }

    /*
     * Find images on a HTML page. THIS IS NOT WORKING REALLY. To many searches
     * before enough images with exif loc is found. TODO: Use graph search 1) Find
     * the doc in solr from source_file_path and offset. (fast) 2) Get image links
     * field 3) For each images see if we have the image in index and it has exif
     * location data
     *
     */
    /*
    public static ArrayList<ArcEntryDescriptor> getImagesWithExifLocationForHtmlPageNewThreaded(String source_file_path, long offset) throws Exception {

        IndexDoc arcEntry = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);
        ArrayList<String> imageLinks = arcEntry.getImageUrls();
        if (imageLinks.size() == 0) {
            return new ArrayList<ArcEntryDescriptor>();
        }
        StringBuilder query = new StringBuilder();
        query.append("(");
        for (String imageUrl : imageLinks) {
            // fix https!
            String fixedUrl = imageUrl;
            if (imageUrl.startsWith("https:")) {
                fixedUrl = "http:" + imageUrl.substring(6); // because image_links are not normlized as url_norm
            }
            query.append(" url_norm:\"" + fixedUrl + "\" OR");
        }
        query.append(" url_norm:none) AND exif_location_0_coordinate:*"); // just close last OR, and must have gps data
        String queryStr = query.toString();
        ArrayList<ArcEntryDescriptor> imagesFromHtmlPage = NetarchiveSolrClient.getInstance().findImagesForTimestamp(queryStr, arcEntry.getCrawlDate());

        return imagesFromHtmlPage;
    }
*/

    /**
     * Get the content encoding for a specific resource in the input WARC file.
     * @param sourceFilePath the path to the source WARC file in the WARC collection, e.g. "crawl1/20100101000000/warc/part-00001.warc.gz".
     * @param offset the offset in the WARC file where the resource starts, e.g. 123456.
     * @return the content encoding as a String, e.g. "UTF-8". If not found, returns "UTF-8" as default.
     */
    public static String getEncoding(String sourceFilePath, String offset) throws Exception {

        SearchResult search = NetarchiveSolrClient.getInstance().search("source_file_path:\"" + sourceFilePath + "\" AND source_file_offset:" + offset, 1);
        if (search.getNumberOfResults() == 0) {
            log.warn("No content encoding found for:" + sourceFilePath + " and offset:" + offset);
            return "UTF-8";
        } else {
            return search.getResults().get(0).getContentEncoding(); // Can still be null.
        }
    }

    /**
     * Get the ArcEntry for a specific resource in the WARC file at sourceFilePath.
     * It is important to set the load binary flag to false if  it is not used.
     * @param sourceFilePath the path to the source WARC file in the WARC collection, e.g. "crawl1/20100101000000/warc/part-00001.warc.gz".
     * @param offset the offset in the WARC file where the resource starts, e.g. 123456.
     * @return the ArcEntry object containing the resource.
     */
    public static ArcEntry getArcEntry(String sourceFilePath, long offset) throws Exception {

       //Validate WARC+offset has been indexed and in the collection.
       //This will prevent url hacking and accessing other WARC-files if you know location on filesystem.
       //Minor performance impact
       //Define property to make it active.
       
       boolean validateWARCFileInCollection=PropertiesLoader.WARC_FILES_VERIFY_COLLECTION;        
        if (validateWARCFileInCollection) { 
            NetarchiveSolrClient.getInstance().getArcEntry(sourceFilePath, offset); //Call Solr. Correct exception already thrown if not found
        }        
        
        return ArcParserFileResolver.getArcEntry(sourceFilePath, offset);
    }

    /**
     * Search images both directly and through webpages. Export the result as WARC entries.
     * @param avoidDuplicates if true, duplicates are removed, based on image hash.
     *                        This requires holding a Set with all hashes from the result set in memory.
     * @param gzip if true each entry in the WARC stream is GZIPped.
     * @param query image search query.
     * @param filterqueries Solr filter queries.
     * @return an InputStream where the product is a WARC.
     * @see ContentStreams#findImages(boolean, boolean, int, String, String...)
     * @see Facade#findImages(String, String...)
     */
    public static InputStream exportImages(boolean avoidDuplicates, boolean gzip, String query, String... filterqueries) {
        // TODO: This has goFast==false and will be accurate but slow. Should it be an option?
        Stream<SolrDocument> imageDocs = ContentStreams.findImages(avoidDuplicates, true,50, query, filterqueries);

        return new StreamingSolrWarcExportBufferedInputStream(imageDocs, Integer.MAX_VALUE, gzip);
    }

    /**
     * Export the search result for the given query and filterQuery as WARC entries.
     * This method makes use of a streaming export, which means that the results are processed
     * as they are fetched from Solr, allowing for large result sets to be handled without
     * loading everything into memory at once.
     *
     */
    public static InputStream exportWarcStreaming(boolean expandResources, boolean ensureUnique, boolean gzip, String query, String... filterqueries)  throws Exception{

        long max=0;
        //Check size
        long results = NetarchiveSolrClient.getInstance().countResults(query, filterqueries);
        if (!expandResources) {
            max= PropertiesLoaderWeb.EXPORT_WARC_MAXRESULTS;
            if (results > PropertiesLoaderWeb.EXPORT_WARC_MAXRESULTS) {
                throw new InvalidArgumentServiceException("Number of results("+results+") for warc export exceeds the configured limit: "+PropertiesLoaderWeb.EXPORT_WARC_MAXRESULTS);
            }
        }
        else {
            max= PropertiesLoaderWeb.EXPORT_WARC_EXPANDED_MAXRESULTS;
            if (results > PropertiesLoaderWeb.EXPORT_WARC_EXPANDED_MAXRESULTS) {
                throw new InvalidArgumentServiceException("Number of results("+results+") for warc expanded export exceeds the configured limit: "+PropertiesLoaderWeb.EXPORT_WARC_EXPANDED_MAXRESULTS);
            }
        }
        Iterator<SolrDocument> solrDocs = SolrStreamDirect.iterate(
                SRequest.builder()
                                .query(query)
                                .filterQueries(filterqueries)
                                .fields("source_file_path", "source_file_offset")
                                .pageSize(100). // TODO: Why so low? The two fields are tiny and single-valued
                        expandResources(expandResources).
                        ensureUnique(ensureUnique));

        return new StreamingSolrWarcExportBufferedInputStream(solrDocs, max, gzip); // Use maximum export results from property-file
    }

    /**
     * <p>
     * Query will have filter: content_type_norm:html AND links_domains:* AND url_type:slashpage"
     * </p>
     * <p>
     * The same domains will appear many time in the solr result set, but only first one will be
     * added the the csv file. The extraction uses a HashMap to remember what domains has been added.
     * </p>
     * <p>
     *  The 100M limit of solr documents will most likely result in a final CSV file with  less than 1M documents.
     *  And this should be enough since Gephi can not handle more than 1M nodes.
     *  Split extraction into crawl_year if the 100M limit is not enough.    
     * </p>
     *  @param q The query
     * 
     */
    public static InputStream exportLinkGraphStreaming(String q) {
        SolrStreamingLinkGraphCSVExportClient solr = SolrStreamingLinkGraphCSVExportClient.createExporter(null, q);
        return new StreamingSolrExportBufferedInputStream(solr, 100000000);  //100M limit, the CSV streaming extractor needs a limit.
    }

    /**
     * @deprecated use {@link #exportFields(String, Boolean, Boolean, String, Boolean, String, Boolean, String, String...)}.
     */
    public static InputStream exportCvsStreaming(String q, String fq, String fields) throws Exception {
        // TODO test only allowed fields are selected!

        //Check size
        long results = NetarchiveSolrClient.getInstance().countResults(q,new String[] {fq});
        if (results > PropertiesLoaderWeb.EXPORT_CSV_MAXRESULTS) {
            throw new InvalidArgumentServiceException("Number of results"+results+") for csv export exceeds the configured limit: "+PropertiesLoaderWeb.EXPORT_CSV_MAXRESULTS);
        }

        SolrStreamingExportClient solr = SolrStreamingExportClient.createCvsExporter(null, q, fields, fq);
        return new StreamingSolrExportBufferedInputStream(solr, PropertiesLoaderWeb.EXPORT_CSV_MAXRESULTS);
    }

    /**
     * Export the search result for the given query and filterQuery as content for the requested fields.
     * @param fields        comma separated list of fields to export.
     * @param expandResources if true, resources used on webpages are resolved relative to the timestamp for the webpage.
     *                        If false, no resource resolving is performed.
     *                        Note: Expanded resources are not part of deduplication with groupField.
     *                        Note 2: ensureUnique works fine with expandResources.
     * @param ensureUnique  if true, uniqueness of the produced documents is ensured by tracking all documents.
     *                      Note: This imposes a memory overhead and should not be used for result sets above 5 million.
     *                      Note 2: This also works with expandResources.
     * @param groupField    if not null, documents will be grouped on the given field and only the first document
     *                      will be exported in each group. This will change document order from score to groupField.
     *                      This is implemented using {@link SRequest#deduplicateFields(String)}.
     * @param flatten       if true, {@link SolrStreamDecorators#flatten(SolrDocument)} will be called on each
     *                      SolrDocument to ensure that no field holds multiple values.
     *                      Note: If there are multiple multi-value fields, this can result in a large amount of
     *                            flattened documents, as all permutations of values will be present.
     * @param format        valid formats are {@code json}, {@code jsonl} and {@code csv}.
     * @param gzip          if true, the output is GZIPpped.
     * @param query         a Solr query.
     * @param filterQueries optional Solr filter queries.
     * @return an InputStream delivering the result.
     * @throws InvalidArgumentServiceException if the request was invalid.
     * @throws IOException if something went wrong during search or delivery.
     * @throws SolrServerException if the number of matches could not be requested from Solr.
     */
    public static InputStream exportFields(
            String fields, Boolean expandResources, Boolean ensureUnique,
            String groupField, Boolean flatten, String format, Boolean gzip,
            String query, String... filterQueries)
            throws IOException, InvalidArgumentServiceException, SolrServerException {
        // TODO check that only allowed fields are selected!

        // Validate result set size
        long results = NetarchiveSolrClient.getInstance().countResults(query, filterQueries);
        if (results > PropertiesLoaderWeb.EXPORT_CSV_MAXRESULTS) {
            throw new InvalidArgumentServiceException(
                    "Number of results("+results+") for " + format + " export exceeds the configured limit: " +
                    PropertiesLoaderWeb.EXPORT_CSV_MAXRESULTS);
        }
        // Setup request
        SRequest request = SRequest.builder().
                query(query).
                filterQueries(filterQueries).
                fields(fields).
                expandResources(expandResources).
                deduplicateFields(groupField).
                ensureUnique(ensureUnique);

        // Create stream
        //Stream<SolrDocument> docs = SolrGenericStreaming.stream(request);
        // TODO: Figure out how to handle the CloseableStream-problem
        Stream<SolrDocument> docs = request.stream();
        if (Boolean.TRUE.equals(flatten)) {
            docs = docs.flatMap(SolrStreamDecorators::flatten);
        }

        return ContentStreams.deliver(docs, fields, format, gzip);
    }

    /**
     * Export content from WARC files to zip that are present in solr query. Can be used to extract files such as HTML, images or PDFs.
     * @param query         used to query solr for warc entries to export.
     * @param filterQueries appended to query.
     * @return              a streaming output containing a zip of all exported files.
     */
    public static StreamingOutput exportZipContent(String query, String... filterQueries)
            throws SolrServerException, IOException, InvalidArgumentServiceException {
        if (!PropertiesLoaderWeb.ALLOW_EXPORT_ZIP){
            throw new InvalidArgumentServiceException("Zip export is not allowed.");
        }

        // Add filter for content length < 0 to filter out redirects.
        String combinedFilters = SolrUtils.combineFilterQueries("content_length", "[1 TO *]", filterQueries);
        // Validate result set size
        long results = NetarchiveSolrClient.getInstance().countResults(query, combinedFilters);
        log.info("Started Zip Content Export for query: '{}', with the following filter queries: '{}'. Found '{}' entries for export.",
                query, combinedFilters, results);
        if (results > PropertiesLoaderWeb.EXPORT_ZIP_MAXRESULTS) {
            throw new InvalidArgumentServiceException(
                    "Number of ("+results+") for zip export exceeds the configured limit: " +
                            PropertiesLoaderWeb.EXPORT_ZIP_MAXRESULTS);
        }

        StreamingRawZipExport zipExporter = new StreamingRawZipExport();

        return output -> zipExporter.getStreamingOutputWithZipOfContent(query, output, filterQueries);
    }


    public static D3Graph waybackgraph(String domain, int facetLimit, boolean ingoing, String dateStart, String dateEnd) throws Exception {


        Date start = new Date(Long.valueOf(dateStart));
        Date end = new Date(Long.valueOf(dateEnd));

        log.info("Creating graph for domain:" + domain + " ingoing:" + ingoing + " and facetLimit:" + facetLimit +" start:"+start +" end:"+end);

        List<FacetCount> facets = NetarchiveSolrClient.getInstance().getDomainFacets(domain, facetLimit, ingoing, start, end);


        HashMap<String, List<FacetCount>> domainFacetMap = new HashMap<String, List<FacetCount>>();
        // Also find facet for all facets from first call.
        domainFacetMap.put(domain, facets); // add this center domain

        // Do all queries
        for (FacetCount f : facets) {
            String facetDomain = f.getValue();
            List<FacetCount> fc = NetarchiveSolrClient.getInstance().getDomainFacets(facetDomain, facetLimit, ingoing, start, end);
            domainFacetMap.put(f.getValue(), fc);
        }

        // Just build a HashSet with all domains
        HashSet<String> allDomains = new HashSet<String>(); // Same domain can be from different queries, but must be same node.
        for (String current : domainFacetMap.keySet()) {
            allDomains.add(current);
            List<FacetCount> list = domainFacetMap.get(current);
            for (FacetCount f : list) {
                allDomains.add(f.getValue());
            }
        }


        D3Graph g = mapDomainsToD3LinkGraph(domain, ingoing, domainFacetMap, allDomains);
        return g;
    }

    private static D3Graph mapDomainsToD3LinkGraph(String domain, boolean ingoing, HashMap<String, List<FacetCount>> domainFacetMap, HashSet<String> allDomains) {

        // First map all urls to a number due to the graph id naming contraints.
        HashMap<String, Integer> domainNumberMap = new HashMap<String, Integer>();
        int number = 0; // start number

        for (String d : allDomains) {
            domainNumberMap.put(d, number++);
        }


        D3Graph g = new D3Graph();
        List<Node> nodes = new ArrayList<Node>();
        g.setNodes(nodes);
        List<Link> links = new ArrayList<Link>();
        g.setLinks(links);

        // All all nodes
        for (String d : allDomains) {
            if (d.equals(domain)) { // Center node
                nodes.add(new Node(d, domainNumberMap.get(d), 16, "red")); // size 16 and red
            } else {
                nodes.add(new Node(d, domainNumberMap.get(d), 5)); // black default color
            }

        }

        // All all edges (links)
        for (String c : domainFacetMap.keySet()) {
            List<FacetCount> list = domainFacetMap.get(c);

            for (FacetCount f : list) {
                if (ingoing) {
                    links.add(new Link(domainNumberMap.get(f.getValue()), domainNumberMap.get(c), 5)); // Link from input url to all facets
                } else {
                    links.add(new Link(domainNumberMap.get(c), domainNumberMap.get(f.getValue()), 5)); // Link from input url to all facets

                }
            }

        }
        return g;
    }

    public static String generatePid(String source_file_path, long offset) throws Exception {
        ArcEntry arc = ArcParserFileResolver.getArcEntry(source_file_path, offset);
        arc.setContentEncoding(Facade.getEncoding(source_file_path, "" + offset));
        String collectionName = PropertiesLoader.PID_COLLECTION_NAME;
        StringBuffer parts = new StringBuffer();
        // the original page
        parts.append("<part>\n");
        parts.append("urn:pwid:" + collectionName + ":" + arc.getCrawlDate() + ":part:" + arc.getUrl() + "\n");
        parts.append("</part>\n");
        String xmlIncludes = HtmlParserUrlRewriter.generatePwid(arc);// all sub elements
        parts.append(xmlIncludes);
        return parts.toString();
    }

    /*
     * This method does something similar to the new feature from archive.org. See:
     * http://blog.archive.org/2017/10/05/wayback-machine-playback-now-with-
     * timestamps/
     *
     * Returns information about the harvested HTML page. List all resources on the
     * page and when they were harvested. Calcuate time difference between html page
     * and each resource. Preview link to html page.
     *
     */

    public static TimestampsForPage timestampsForPage(String source_file_path, long offset) throws Exception {
        TimestampsForPage ts = new TimestampsForPage();
        ArrayList<PageResource> pageResources = new ArrayList<PageResource>();
        ts.setResources(pageResources);

        ArcEntry arc = ArcParserFileResolver.getArcEntry(source_file_path, offset);
        // TODO WHY ? arc.setContentEncoding(Facade.getEncoding(source_file_path,
        // ""+offset));

        IndexDoc docPage = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);

        Date pageCrawlDate = new Date(docPage.getCrawlDateLong());
        ts.setPageCrawlDate(pageCrawlDate);
        ts.setPageUrl(arc.getUrl());

        String previewUrl = PropertiesLoader.WAYBACK_BASEURL + "services/image/pagepreview?source_file_path=" + source_file_path + "&offset=" + offset
                + "&showToolbar=false";
        ts.setPagePreviewUrl(previewUrl);

        // the original page REMEMBER

        // Get all resources that does not start with "data:"
        HashSet<String> resources = HtmlParserUrlRewriter.getResourceLinksForHtmlFromArc(arc).stream().
                filter(url -> !url.startsWith("data:")).
                collect(Collectors.toCollection(HashSet::new));

        ArrayList<IndexDoc> docs = NetarchiveSolrClient.getInstance().findNearestHarvestTimeForMultipleUrlsFullFields(resources, arc.getCrawlDate());


        long maximumTimeDifferenceBackward=0; //Will be negative
        long maximumTimeDifferenceForward=0;//Will be posive

        for (IndexDoc doc : docs) { // These are the resources found
            String docUrl = doc.getUrl_norm();
            PageResource pageResource = new PageResource();

            Date resourceDate = new Date(doc.getCrawlDateLong());
            pageResource.setCrawlTime(resourceDate);
            pageResource.setUrl(doc.getUrl());
            pageResource.setContentType(doc.getContentTypeNorm());
            String downloadUrl = PropertiesLoader.WAYBACK_BASEURL + "services/downloadRaw?source_file_path=" + doc.getSource_file_path() + "&offset="
                    + doc.getOffset();
            pageResource.setDownloadUrl(downloadUrl);

            long timeDif = resourceDate.getTime() - pageCrawlDate.getTime();

            if (timeDif <= maximumTimeDifferenceBackward){
                maximumTimeDifferenceBackward=timeDif;
            }
            if (timeDif >= maximumTimeDifferenceForward){
                maximumTimeDifferenceForward=timeDif;
            }


            pageResource.setTimeDifference(millisToDuration(timeDif));

            pageResources.add(pageResource);
            resources.remove(docUrl);
        }

        ts.setMaximumTimeDifferenceBackward(millisToDuration(-maximumTimeDifferenceBackward));  //Remove the minus
        ts.setMaximumTimeDifferenceForward(millisToDuration(maximumTimeDifferenceForward));

        ts.setNotHarvested(new ArrayList<String>(resources));

        return ts;
    }

    public static IndexDoc resolveRelativUrlForResource(String source_file_path, long offset, String leakUrl) throws Exception {
        if (!leakUrl.startsWith("/solrwayback")) {
            log.warn("resolveRelativeLeak does not start with /solrwayback:" + leakUrl);
            throw new InvalidArgumentServiceException("resolveRelativeLeak does not start with: /solrwayback");
        }
        // remove the start, and everyting until second /
        leakUrl = leakUrl.substring(12);
        leakUrl = leakUrl.substring(leakUrl.indexOf("/") + 1);

        IndexDoc doc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);
        URL originalURL = new URL(doc.getUrl());
        String resolvedUrl = new URL(originalURL, leakUrl).toString();

        log.debug("stripped leakUrl:" + leakUrl);
        log.debug("url origin:" + doc.getUrl());
        log.debug("resolved URL:" + resolvedUrl);

        // First see if we have the given URL as excact match.
        IndexDoc docFound = NetarchiveSolrClient.getInstance().findClosestHarvestTimeForUrl(resolvedUrl, doc.getCrawlDate());
        if (docFound != null) {
            return docFound;
        }
        String[] tokens = leakUrl.split("/");
        String leakResourceName = tokens[tokens.length - 1];

        // else just try to lookup resourcename (last part of the url) for that domain.
        ArrayList<IndexDoc> matches = NetarchiveSolrClient.getInstance().findNearestForResourceNameAndDomain(doc.getDomain(), leakResourceName,
                doc.getCrawlDate());
        for (IndexDoc m : matches) {
            if (m.getUrl().endsWith(leakUrl)) {
                log.info("found leak url:" + m.getUrl());
                return m;
            }
        }
        log.info("Could not find relative resource:" + leakUrl);
        throw new NotFoundServiceException("Could not find relative resource:" + leakUrl);
    }

    /*
     * This is called then a relative url fails. Try to match the relative url for
     * that domain. (qualified guessing...)
     *
     */
    public static IndexDoc matchRelativeUrlForDomain(String refererUrl, String url, String solrDate) throws Exception {

        log.info("url not with domain:" + url + " referer:" + refererUrl);
        String orgDomain = UrlUtils.getDomainFromWebApiParameters(refererUrl);
        String resourceName = UrlUtils.getResourceNameFromWebApiParameters(url);
        // resourceLeaked
        // TODO use crawltime from refererUrl
        ArrayList<IndexDoc> matches = NetarchiveSolrClient.getInstance().findNearestForResourceNameAndDomain(orgDomain, resourceName, solrDate);
        // log.info("********* org domain:"+orgDomain);
        // log.info("********* resourceLeaked "+resourceName );
        // log.info("********* matches:"+matches.size());
        for (IndexDoc m : matches) {
            if (m.getUrl().endsWith(resourceName)) {
                log.info("found leaked resource for:" + url);
                return m;
            }
        }
        throw new NotFoundServiceException("Could not find resource for leak:" + url);
    }


    public static ArcEntry viewResource(
            String source_file_path, long offset, IndexDoc doc, Boolean showToolbar, Boolean lenient) throws Exception {
        lenient = Boolean.TRUE.equals(lenient);
        if (showToolbar == null) {
            showToolbar = false;
        }
        ArcEntry arc = ArcParserFileResolver.getArcEntry(source_file_path, offset);

        //log.debug("View html Warc content-type:" + arc.getContentType());

        String encoding = arc.getContentCharset();

        if (encoding == null) {
            encoding = Facade.getEncoding(source_file_path, "" + offset); // Ask the index
        }
        if (encoding == null) {
            encoding = "ISO-8859-1"; // Is UTF-8 a better default?
        }

        arc.setContentCharset(encoding); // Need to help read the binary.

        if (doc.getType().equals("Twitter Tweet")) {
            TwitterPlayback twitterPlayback = new TwitterPlayback(arc, doc, showToolbar);
            return twitterPlayback.playback(lenient);
        } else if (doc.getType().equals("Jodel Post") || doc.getType().equals("Jodel Thread")) {
            JodelPlayback jodelPlayback = new JodelPlayback(arc, doc, showToolbar);
            return jodelPlayback.playback(lenient);
        } else if ("Web Page".equals(doc.getType())|| ((300 <= doc.getStatusCode() && arc.getContentType() != null && arc.getContentType().equals("text/html")))) {

            // We still want the toolbar to show for http moved (302 etc.)
            HtmlPlayback htmlPlayback = new HtmlPlayback(arc, doc, showToolbar);
            return htmlPlayback.playback(lenient);
        } else if ("text/css".equals(arc.getContentType()) ) {
            CssPlayback cssPlayback = new CssPlayback(arc, doc, showToolbar); // toolbar is never shown anyway.
            return cssPlayback.playback(lenient);
        }
        else if ("text/javascript".equals(arc.getContentType()) ) {
            JavascriptPlayback javascriptPlayback = new JavascriptPlayback(arc, doc, showToolbar); // toolbar is never shown anyway.
            return javascriptPlayback.playback(lenient);
        }

        else { // Serve as it is. (Javascript, images, pdfs etc.)

            return arc; // dont parse
        }
    }

    /**
     * Get the properties for the frontend web application.
     * @return a HashMap with the properties used by the frontend.
     */
    public static HashMap<String, String> getPropertiesWeb() throws Exception {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertiesLoaderWeb.WEBAPP_BASEURL_PROPERTY,PropertiesLoaderWeb.WEBAPP_PREFIX); //TODO change value name when frontend also switch
        props.put(PropertiesLoaderWeb.PLAYBACK_PRIMARY_ENGINE_PROPERTY, PropertiesLoaderWeb.PLAYBACK_PRIMARY_ENGINE);        
        props.put(PropertiesLoaderWeb.WAYBACK_SERVER_PROPERTY, PropertiesLoaderWeb.WAYBACK_SERVER);
        props.put(PropertiesLoaderWeb.PLAYBACK_ALTERNATIVE_ENGINE_PROPERTY, PropertiesLoaderWeb.PLAYBACK_ALTERNATIVE_ENGINE);
        props.put(PropertiesLoaderWeb.ALLOW_EXPORT_WARC_PROPERTY, "" + PropertiesLoaderWeb.ALLOW_EXPORT_WARC);
        props.put(PropertiesLoaderWeb.ALLOW_EXPORT_CSV_PROPERTY, "" + PropertiesLoaderWeb.ALLOW_EXPORT_CSV);
        props.put(PropertiesLoaderWeb.ALLOW_EXPORT_ZIP_PROPERTY, "" + PropertiesLoaderWeb.ALLOW_EXPORT_ZIP);
        props.put(PropertiesLoaderWeb.EXPORT_CSV_FIELDS_PROPERTY, PropertiesLoaderWeb.EXPORT_CSV_FIELDS);
        props.put(PropertiesLoaderWeb.MAPS_LATITUDE_PROPERTY, PropertiesLoaderWeb.MAPS_LATITUDE);
        props.put(PropertiesLoaderWeb.MAPS_LONGITUDE_PROPERTY, PropertiesLoaderWeb.MAPS_LONGITUDE);
        props.put(PropertiesLoaderWeb.MAPS_RADIUS_PROPERTY, PropertiesLoaderWeb.MAPS_RADIUS);
        props.put(PropertiesLoaderWeb.LEAFLET_SOURCE_PROPERTY, PropertiesLoaderWeb.LEAFLET_SOURCE);
        props.put(PropertiesLoaderWeb.LEAFLET_ATTRIBUTION_PROPERTY, PropertiesLoaderWeb.LEAFLET_ATTRIBUTION);
        props.put(PropertiesLoaderWeb.ARCHIVE_START_YEAR_PROPERTY, ""+PropertiesLoaderWeb.ARCHIVE_START_YEAR);
        props.put(PropertiesLoaderWeb.WORDCLOUD_STOPWORDS_PROPERTY, ""+PropertiesLoaderWeb.WORDCLOUD_STOPWORDS);
        props.put(PropertiesLoaderWeb.FACETS_PROPERTY, ""+PropertiesLoaderWeb.FACETS);
        props.put(PropertiesLoaderWeb.SEARCH_UPLOADED_FILE_DISABLED_PROPERTY, ""+PropertiesLoaderWeb.SEARCH_UPLOADED_FILE_DISABLED);
        props.put(PropertiesLoaderWeb.SEARCH_PAGINATION_PROPERTY, "" + PropertiesLoaderWeb.SEARCH_PAGINATION);
        props.put(PropertiesLoader.PLAYBACK_DISABLED_PROPERTY, ""+""+PropertiesLoader.PLAYBACK_DISABLED);
        props.put("solrwayback.version",PropertiesLoaderWeb.SOLRWAYBACK_VERSION);
        props.put(PropertiesLoaderWeb.TEXT_STATS_PROPERTY, ""+PropertiesLoaderWeb.STATS_ALL_FIELDS);
        props.put(PropertiesLoaderWeb.NUMERIC_STATS_PROPERTY, ""+PropertiesLoaderWeb.STATS_ALL_FIELDS);

        if (PropertiesLoaderWeb.TOP_LEFT_LOGO_IMAGE != null && !"".equals(PropertiesLoaderWeb.TOP_LEFT_LOGO_IMAGE.trim())) {
            props.put(PropertiesLoaderWeb.TOP_LEFT_LOGO_IMAGE_PROPERTY,PropertiesLoader.WAYBACK_BASEURL + "services/frontend/images/logo");
            props.put(PropertiesLoaderWeb.TOP_LEFT_LOGO_IMAGE_LINK_PROPERTY,PropertiesLoaderWeb.TOP_LEFT_LOGO_IMAGE_LINK);
        }

        if (PropertiesLoaderWeb.ALTERNATIVE_PLAYBACK_COLLECTION_MAPPING.size() >0) {
            props.put("COLLECTION_PLAYBACK","true");
            for (String mapping: PropertiesLoaderWeb.ALTERNATIVE_PLAYBACK_COLLECTION_MAPPING.keySet()) {
               props.put("PLAYBACK_"+mapping,PropertiesLoaderWeb.ALTERNATIVE_PLAYBACK_COLLECTION_MAPPING.get(mapping));                        
            }            
        }

        return props;
    }

    /**
     * Proxy a Solr query and return the JSON response without facets.
     * @param query the Solr query to execute.
     * @param fq filter queries to apply to the Solr query.
     * @param grouping if true, grouping will be applied to the Solr query.
     * @param revisits if true, revisits will be included in the Solr query.
     * @param start the starting point for the results (pagination).
     * @param sort the sort order for the results.
     * @return the JSON response from the Solr query as a String.
     */
    public static String proxySolrNoFacets(String query, List<String> fq, boolean grouping, boolean revisits, Integer start, String sort) throws Exception {
        return NetarchiveSolrClient.getInstance().searchJsonResponseNoFacets(query, fq, grouping, revisits, start, sort);
    }

    /**
     * Proxy a Solr query and return the JSON response containing facets only.
     * @param query the Solr query to execute.
     * @param fq filter queries to apply to the Solr query.
     * @param revisits if true, revisits will be included in the Solr query.
     * @return the JSON response from the Solr query containing only facets as a String.
     */
    public static String proxySolrOnlyFacets(String query, List<String> fq, boolean revisits) throws Exception {
        return NetarchiveSolrClient.getInstance().searchJsonResponseOnlyFacets(query, fq, revisits);
    }

    //TODO: Describe what this method does.
    public static String proxySolrOnlyFacetsLoadMore( String query, List<String> fq, String facetField, boolean revisits) throws Exception {
        return NetarchiveSolrClient.getInstance().searchJsonResponseOnlyFacetsLoadMore(query, fq, facetField, revisits);
    }

    /*
     * Temp solution, make generic query properties
     *
     */

    /*
     * public static String proxyBackendResources(String source_file_path, String
     * offset, String serviceName) throws Exception{
     *
     * String backendServer= PropertiesLoaderWeb.WAYBACK_SERVER;
     *
     *
     * ClientConfig config = new DefaultClientConfig(); Client client =
     * Client.create(config); WebResource service =
     * client.resource(UriBuilder.fromUri(backendServer).build()); WebResource
     * queryWs= service.path("services") .path(serviceName)
     * .queryParam("source_file_path", source_file_path) .queryParam("offset",
     * offset);
     *
     *
     * ClientResponse response =
     * queryWs.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class); String
     * responseStr= response.getEntity(String.class);
     *
     * return responseStr;
     *
     * }
     */


    public static ArrayList<ImageUrl> indexDoc2Images(ArrayList<IndexDoc> docs) {
        ArrayList<ImageUrl> imageUrls = new ArrayList<ImageUrl>();
        for (IndexDoc entry : docs) {
            ImageUrl imageUrl = new ImageUrl();
            String imageLink = PropertiesLoader.WAYBACK_BASEURL + "services/image?source_file_path=" + entry.getSource_file_path() + "&offset="
                    + entry.getOffset();
            String downloadLink = PropertiesLoader.WAYBACK_BASEURL + "services/downloadRaw?source_file_path=" + entry.getSource_file_path() + "&offset="
                    + entry.getOffset();
            imageUrl.setImageUrl(imageLink);
            imageUrl.setDownloadUrl(downloadLink);
            imageUrl.setHash(entry.getHash());
            imageUrl.setUrlNorm(entry.getUrl_norm());

            imageUrl.setLastModified(entry.getLastModifiedLong());
            String exifLocation = entry.getExifLocation();
            if (exifLocation != null) {
                String[] split = exifLocation.split(",");
                double lat = Double.parseDouble(split[0]);
                imageUrl.setLatitude(lat);
                double lon = Double.parseDouble(split[1]);
                imageUrl.setLongitude(lon);
                imageUrl.setResourceName(entry.getResourceName());
            }
            imageUrls.add(imageUrl);
        }
        return imageUrls;
    }

    public static ArrayList<ImageUrl> arcEntrys2Images(ArrayList<ArcEntryDescriptor> arcs) {
        ArrayList<ImageUrl> imageUrls = new ArrayList<ImageUrl>();
        for (ArcEntryDescriptor entry : arcs) {
            ImageUrl imageUrl = new ImageUrl();
            String imageLink = PropertiesLoader.WAYBACK_BASEURL + "services/image?source_file_path=" + entry.getSource_file_path() + "&offset="
                    + entry.getOffset();
            String downloadLink = PropertiesLoader.WAYBACK_BASEURL + "services/downloadRaw?source_file_path=" + entry.getSource_file_path() + "&offset="
                    + entry.getOffset();
            imageUrl.setImageUrl(imageLink);
            imageUrl.setDownloadUrl(downloadLink);
            imageUrl.setHash(entry.getHash());
            imageUrl.setUrlNorm(entry.getUrl_norm());
            imageUrls.add(imageUrl);
        }
        return imageUrls;
    }

    /**
     * Get standard solr stats for all fields given.
     * @param query     to generate stats for.
     * @param filters   that are to be added to solr query.
     * @param fields    to return stats for.
     * @return all standard stats for all fields from query.
     */
    public static ArrayList<QueryStatistics> getQueryStats(String query, List<String> filters, List<String> fields) throws InvalidArgumentServiceException {
        if (fields.isEmpty()){
            throw new InvalidArgumentServiceException("The fields parameter has to be set.");
        }
        if (!checkListValues(fields, PropertiesLoaderWeb.STATS_ALL_FIELDS)) {
            throw new InvalidArgumentServiceException("One or more of the values: '" + StringUtils.join(fields, ", ") + "' in parameter fields are not allowed.");
        }
        ArrayList<QueryStatistics> queryStats = SolrStats.getStatsForFields(query, filters, fields);
        return queryStats;
    }

    /**
     * Get percentiles for numeric fields
     * @param query to generate stats for.
     * @param percentiles to extract values for.
     * @param fields to return percentiles for.
     * @return percentiles for specified fields.
     */
    public static ArrayList<QueryPercentilesStatistics> getPercentileStatsForFields(String query, List<String> percentiles, List<String> fields) throws InvalidArgumentServiceException {
        if (percentiles.isEmpty()){
            throw new InvalidArgumentServiceException("The percentiles parameter has to be set.");
        }
        if (fields.isEmpty()){
            throw new InvalidArgumentServiceException("The fields parameter has to be set.");
        }
        if (!checkListValues(fields, PropertiesLoaderWeb.STATS_NUMERIC_FIELDS)) {
            throw new InvalidArgumentServiceException("One or more of the values: '" + StringUtils.join(fields, ", ") + "' in parameter fields are not allowed.");
        }
        List<Double> truePercentiles = new ArrayList<>();
        try {
            for (String percentile: percentiles) {
                double dbl = Double.parseDouble(percentile);
                if ( !(dbl >= 0 && dbl <= 100)){
                    throw new InvalidArgumentServiceException("Percentiles needs to be in range 0-100.");
                }
                truePercentiles.add(dbl);
            }
        } catch (NumberFormatException e){
            throw new InvalidArgumentServiceException("Percentiles needs to be numbers");
        }
        ArrayList<QueryPercentilesStatistics> percentileStats = SolrStats.getPercentilesForFields(query, truePercentiles, fields);
        return percentileStats;
    }


    /*
     * Just show the most important
     */
    private static String millisToDuration(long millis) { // TODO better... fast impl for demo
        String sign = "";
        if (millis < 0) {
            sign = "-";
            millis = -millis;
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        if (days > 0) {
            return sign + days + " days";
        }
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        if (hours > 0) {
            return sign + hours + " hours";
        }
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        if (minutes > 0) {
            return sign + minutes + " minutes";
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        return sign + seconds + " seconds";
    }

    /**
     * Check that input values from list are present in controllist.
     * @param list to check for values in.
     * @param controlList to check values against.
     * @return a boolean.
     */
    private static boolean checkListValues(List<String> list, List<String> controlList){
        boolean result = true;
        for (String value: list) {
            result = controlList.contains(value);
            if(!result){
                break;
            }
        }
        return result;
    }

}
