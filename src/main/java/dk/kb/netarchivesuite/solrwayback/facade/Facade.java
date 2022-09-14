package dk.kb.netarchivesuite.solrwayback.facade;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.IDN;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.ws.rs.QueryParam;

import dk.kb.netarchivesuite.solrwayback.util.SolrQueryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.parsers.*;
import dk.kb.netarchivesuite.solrwayback.playback.CssPlayback;
import dk.kb.netarchivesuite.solrwayback.playback.HtmlPlayback;
import dk.kb.netarchivesuite.solrwayback.playback.JavascriptPlayback;
import dk.kb.netarchivesuite.solrwayback.playback.JodelPlayback;
import dk.kb.netarchivesuite.solrwayback.playback.TwitterPlayback;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.*;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.D3Graph;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.Link;
import dk.kb.netarchivesuite.solrwayback.service.dto.graph.Node;
import dk.kb.netarchivesuite.solrwayback.service.dto.smurf.SmurfYearBuckets;
import dk.kb.netarchivesuite.solrwayback.service.dto.statistics.DomainYearStatistics;
import dk.kb.netarchivesuite.solrwayback.service.exception.InvalidArgumentServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.NotFoundServiceException;
import dk.kb.netarchivesuite.solrwayback.smurf.NetarchiveYearCountCache;
import dk.kb.netarchivesuite.solrwayback.smurf.SmurfUtil;
import dk.kb.netarchivesuite.solrwayback.solr.*;
import dk.kb.netarchivesuite.solrwayback.util.FileUtil;
import dk.kb.netarchivesuite.solrwayback.util.UrlUtils;
import dk.kb.netarchivesuite.solrwayback.wordcloud.WordCloudImageGenerator;
import dk.kb.netarchivesuite.solrwayback.concurrency.ImageSearchExecutor;
import dk.kb.netarchivesuite.solrwayback.export.StreamingSolrExportBufferedInputStream;
import dk.kb.netarchivesuite.solrwayback.export.StreamingSolrWarcExportBufferedInputStream;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;

public class Facade {
    private static final Logger log = LoggerFactory.getLogger(Facade.class);

    public static SearchResult search(String searchText, String filterQuery) throws Exception {
        SearchResult result = NetarchiveSolrClient.getInstance().search(searchText, filterQuery);
        return result;
    }

    public static String solrSearchNoFacets(String query, List<String> filterQueries, boolean grouping, boolean revisits, int start) throws Exception {
        if (start >= 1001) {
            throw new InvalidArgumentServiceException("Pagination (start) must be less than 1001");
        }
        return proxySolrNoFacets(query, filterQueries, grouping, revisits, start);
    }

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

    public static String getAboutText() throws Exception {
        String aboutFile = PropertiesLoaderWeb.ABOUT_TEXT_FILE;
        String aboutText = FileUtil.fetchUTF8(aboutFile);
        return aboutText;
    }

    public static String getSearchHelpText() throws Exception {
        String searchHelpFile = PropertiesLoaderWeb.SEARCH_HELP_TEXT_FILE;
        String searchHelpText = FileUtil.fetchUTF8(searchHelpFile);
        return searchHelpText;
    }

    public static String getCollectionText() throws Exception {
        String collectionFile = PropertiesLoaderWeb.COLLECTION_TEXT_FILE;
        String collectionText = FileUtil.fetchUTF8(collectionFile);
        return collectionText;
    }

    public static String generateDomainResultGraph(@QueryParam("q") String q, @QueryParam("fq") List<String> fq) throws Exception {
        String jsonStr = NetarchiveSolrClient.getInstance().domainStatisticsForQuery(q, fq);
        HashMap<Integer, List<FacetCount>> domainStatisticsForQuery = DomainStatisticsForDomainParser.parseDomainStatisticsJson(jsonStr);
        String matrix = DomainStatisticsForDomainParser.generateDomainQueryStatisticsString(domainStatisticsForQuery);
        return matrix;
    }

    public static ArrayList<ArcEntryDescriptor> findImages(String searchText) throws Exception {
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

    public static ArrayList<DomainYearStatistics> statisticsDomain(String domain) throws Exception {
        ArrayList<DomainYearStatistics> stats = new ArrayList<DomainYearStatistics>();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int startYear=PropertiesLoaderWeb.ARCHIVE_START_YEAR;

        for (int i = startYear; i <= year; i++) {
            DomainYearStatistics yearStat = NetarchiveSolrClient.getInstance().domainStatistics(domain, i);
            stats.add(yearStat);
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

    public static SmurfYearBuckets generateNetarchiveSmurfData(String tag, String filterQuery, int startyear) throws Exception {

        if (tag == null || tag.length() == 0) {
            throw new InvalidArgumentServiceException("tag must not be empty");
        }

        log.info("netarchive smurf tag query:" + tag + " for startyear:" + startyear);
        try {

            HashMap<Integer, Long> yearFacetsQuery = NetarchiveSolrClient.getInstance().getYearHtmlFacets(tag);
            HashMap<Integer, Long> yearFacetsAll = NetarchiveYearCountCache.getYearFacetsAllQuery();

            SmurfYearBuckets buckets = SmurfUtil.generateYearBuckets(yearFacetsQuery, yearFacetsAll, startyear, null);
            return buckets;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    public static SmurfYearBuckets generateNetarchiveTextSmurfData(String query, String filterQuery, int startyear) throws Exception {

        if (query == null || query.length() == 0) {
            throw new InvalidArgumentServiceException("query must not be empty");
        }

        log.info("netarchive content smurf query:" + query + " for startyear:" + startyear);
        try {

            HashMap<Integer, Long> yearContentQuery = NetarchiveSolrClient.getInstance().getYearTextHtmlFacets(query);
            HashMap<Integer, Long> yearFacetsAll = NetarchiveYearCountCache.getYearFacetsAllQuery();

            SmurfYearBuckets buckets = SmurfUtil.generateYearBuckets(yearContentQuery, yearFacetsAll, startyear, null);
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

    public static String punyCodeAndNormaliseUrl(String url) throws Exception {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new Exception("Url not starting with http:// or https://");
        }

        URL uri = new URL(url);
        String hostName = uri.getHost();
        String hostNameEncoded = IDN.toASCII(hostName);
        
        String path = uri.getPath();
        if ("".equals(path)) {
            path = "/";
        }
        String urlQueryPath = uri.getQuery();
        String urlPunied = null;
        if (urlQueryPath == null) {
             urlPunied = "http://" + hostNameEncoded + path;
        }
        else {
            urlPunied = "http://" + hostNameEncoded + path +"?"+ urlQueryPath;            
        }
        String urlPuniedAndNormalized = Normalisation.canonicaliseURL(urlPunied);       
        return urlPuniedAndNormalized;
    }

    /*
     * Find images on a HTML page. 1) Find the doc in solr from source_file_path and
     * offset. (fast) 2) Get image links field 3) For each images try to find that
     * url_norm in solr with harvest time closest to the harvesttime for the HTML
     * page.
     */
    public static ArrayList<ArcEntryDescriptor> getImagesForHtmlPageNewThreaded(String source_file_path, long offset) throws Exception {
        IndexDoc doc = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);
        ArrayList<String> imageLinks = doc.getImageUrls();
        if (imageLinks.size() == 0) {
            return new ArrayList<>();
        }
        String queryStr = SolrQueryUtils.createQueryStringForUrls(imageLinks);
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
    public static String getEncoding(String source_file_path, String offset) throws Exception {

        SearchResult search = NetarchiveSolrClient.getInstance().search("source_file_path:\"" + source_file_path + "\" AND source_file_offset:" + offset, 1);
        if (search.getNumberOfResults() == 0) {
            log.warn("No content encoding found for:" + source_file_path + " and offset:" + offset);
            return "UTF-8";
        } else {
            return search.getResults().get(0).getContentEncoding(); // Can still be null.
        }
    }

    /**
    * Important to set the load binary flag to false if not used    
    */    
    public static ArcEntry getArcEntry(String source_file_path, long offset, boolean loadBinary) throws Exception {      

       //Validate WARC+offset has been indexed and in the collection.
       //This will prevent url hacking and accessing other WARC-files if you know location on filesystem.
       //Minor performance impact
       //Define property to make it active.
       
       boolean validateWARCFileInCollection=PropertiesLoader.WARC_FILES_VERIFY_COLLECTION;        
        if (validateWARCFileInCollection) { 
            NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset); //Call Solr. Correct exception already thrown if not found
        }        
        
        return ArcParserFileResolver.getArcEntry(source_file_path, offset, loadBinary);
    }

    public static InputStream exportWarcStreaming(boolean expandResources, boolean avoidDuplicates, boolean gzip, String query, String... filterqueries)  throws Exception{

        long max=0;
        //Check size
        long results = NetarchiveSolrClient.getInstance().countResults(query, filterqueries);
        if (!expandResources) {
            max= PropertiesLoaderWeb.EXPORT_WARC_MAXRESULTS;
            if (results > PropertiesLoaderWeb.EXPORT_WARC_MAXRESULTS) {
                throw new InvalidArgumentServiceException("Number of results for warc export exceeds the configured limit: "+PropertiesLoaderWeb.EXPORT_WARC_MAXRESULTS);
            }
        }
        else {
            max= PropertiesLoaderWeb.EXPORT_WARC_EXPANDED_MAXRESULTS;;
            if (results > PropertiesLoaderWeb.EXPORT_WARC_EXPANDED_MAXRESULTS) {
                throw new InvalidArgumentServiceException("Number of results for warc expanded  export exceeds the configured limit: "+PropertiesLoaderWeb.EXPORT_WARC_EXPANDED_MAXRESULTS);
            }
        }
        SolrGenericStreaming solr = new SolrGenericStreaming(PropertiesLoader.SOLR_SERVER, 100, Arrays.asList("source_file_path", "source_file_offset"),
                expandResources, avoidDuplicates, query, filterqueries);

        return new StreamingSolrWarcExportBufferedInputStream(solr, max, gzip); // Use maximum export results from property-file
    }

    public static InputStream exportLinkGraphStreaming(String q) {
        SolrStreamingLinkGraphCSVExportClient solr = SolrStreamingLinkGraphCSVExportClient.createExporter(PropertiesLoader.SOLR_SERVER, q);
        return new StreamingSolrExportBufferedInputStream(solr, 1000000); // 1 MIL
    }

    public static InputStream exportCvsStreaming(String q, String fq, String fields) throws Exception {
        // TODO test only allowed fields are selected!

        //Check size
        long results = NetarchiveSolrClient.getInstance().countResults(q,new String[] {fq});
        if (results > PropertiesLoaderWeb.EXPORT_CSV_MAXRESULTS) {
            throw new InvalidArgumentServiceException("Number of results for csv export exceeds the configured limit: "+PropertiesLoaderWeb.EXPORT_CSV_MAXRESULTS);
        }

        SolrStreamingExportClient solr = SolrStreamingExportClient.createCvsExporter(PropertiesLoader.SOLR_SERVER, q, fields, fq);
        return new StreamingSolrExportBufferedInputStream(solr, PropertiesLoaderWeb.EXPORT_CSV_MAXRESULTS);
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

        HashSet<String> resources = HtmlParserUrlRewriter.getResourceLinksForHtmlFromArc(arc);

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


    public static ArcEntry viewResource(String source_file_path, long offset, IndexDoc doc, Boolean showToolbar) throws Exception {
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
            return twitterPlayback.playback();
        } else if (doc.getType().equals("Jodel Post") || doc.getType().equals("Jodel Thread")) {
            JodelPlayback jodelPlayback = new JodelPlayback(arc, doc, showToolbar);
            return jodelPlayback.playback();
        } else if ("Web Page".equals(doc.getType())|| ((300 <= doc.getStatusCode() && arc.getContentType() != null && arc.getContentType().equals("text/html")))) {

            // We still want the toolbar to show for http moved (302 etc.)
            HtmlPlayback htmlPlayback = new HtmlPlayback(arc, doc, showToolbar);
            return htmlPlayback.playback();
        } else if ("text/css".equals(arc.getContentType()) ) {
            CssPlayback cssPlayback = new CssPlayback(arc, doc, showToolbar); // toolbar is never shown anyway.
            return cssPlayback.playback();
        }
        else if ("text/javascript".equals(arc.getContentType()) ) {
            JavascriptPlayback javascriptPlayback = new JavascriptPlayback(arc, doc, showToolbar); // toolbar is never shown anyway.
            return javascriptPlayback.playback();
        }

        else { // Serve as it is. (Javascript, images, pdfs etc.)

            return arc; // dont parse
        }
    }

    // For fronted
    public static HashMap<String, String> getPropertiesWeb() throws Exception {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(PropertiesLoaderWeb.WEBAPP_BASEURL_PROPERTY,PropertiesLoaderWeb.WEBAPP_PREFIX); //TODO change value name when frontend also switch
        props.put(PropertiesLoaderWeb.WAYBACK_SERVER_PROPERTY, PropertiesLoaderWeb.WAYBACK_SERVER);
        props.put(PropertiesLoaderWeb.OPENWAYBACK_SERVER_PROPERTY, PropertiesLoaderWeb.OPENWAYBACK_SERVER);
        props.put(PropertiesLoaderWeb.ALLOW_EXPORT_WARC_PROPERTY, "" + PropertiesLoaderWeb.ALLOW_EXPORT_WARC);
        props.put(PropertiesLoaderWeb.ALLOW_EXPORT_CSV_PROPERTY, "" + PropertiesLoaderWeb.ALLOW_EXPORT_CSV);
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
        props.put(PropertiesLoader.PLAYBACK_DISABLED_PROPERTY, ""+""+PropertiesLoader.PLAYBACK_DISABLED);
        props.put("solrwayback.version",PropertiesLoaderWeb.SOLRWAYBACK_VERSION);

        if (PropertiesLoaderWeb.TOP_LEFT_LOGO_IMAGE != null && !"".equals(PropertiesLoaderWeb.TOP_LEFT_LOGO_IMAGE.trim())) {
            props.put(PropertiesLoaderWeb.TOP_LEFT_LOGO_IMAGE_PROPERTY,PropertiesLoader.WAYBACK_BASEURL + "services/frontend/images/logo");
            props.put(PropertiesLoaderWeb.TOP_LEFT_LOGO_IMAGE_LINK_PROPERTY,PropertiesLoaderWeb.TOP_LEFT_LOGO_IMAGE_LINK);
        }

        return props;
    }

    public static String proxySolrNoFacets(String query, List<String> fq, boolean grouping, boolean revisits, Integer start) throws Exception {
        return NetarchiveSolrClient.getInstance().searchJsonResponseNoFacets(query, fq, grouping, revisits, start);
    }

    public static String proxySolrOnlyFacets(String query, List<String> fq, boolean revisits) throws Exception {
        return NetarchiveSolrClient.getInstance().searchJsonResponseOnlyFacets(query, fq, revisits);
    }

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

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }

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

}
