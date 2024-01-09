package dk.kb.netarchivesuite.solrwayback.properties;

import dk.kb.netarchivesuite.solrwayback.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;


public class PropertiesLoader {

    private static final Logger log = LoggerFactory.getLogger(PropertiesLoader.class);

    /**
     * If no name is given to a WARC file resolver param, it will be assigned this name.
     * Example: {@code warc.file.resolver.parameters=foo} will result in {@code _unqualified_=foo} in the
     * {@link #WARC_FILE_RESOLVER_PARAMETERS} map, while {@code warc.file.resolver.parameters.myparam=foo} will
     * result in {@code myparam=foo}.
     */
    public static final String WARC_FILE_RESOLVER_UNQUALIFIED = "_unqualified_";

    private static final String DEFAULT_PROPERTY_FILE = "solrwayback.properties";
    private static final String SOLR_SERVER_PROPERTY="solr.server";
    private static final String WARC_FILE_RESOLVER_CLASS_PROPERTY="warc.file.resolver.class";
    private static final String WARC_FILE_RESOLVER_PARAMETERS_PROPERTY="warc.file.resolver.parameters";
    private static final String WARC_SOURCE_HTTP_FALLBACK_PROPERTY = "warc.file.resolver.source.http.readfallback";
    // The now deprecated ArcHTTPResolver used this property to specify readfallback
    private static final String WARC_SOURCE_HTTP_FALLBACK_LEGACY_PROPERTY = "warc.file.resolver.parameters.readfallback";
    private static final String WAYBACK_BASEURL_PROPERTY="wayback.baseurl";
    private static final String CHROME_COMMAND_PROPERTY="chrome.command";
    private static final String SCREENSHOT_TEMP_IMAGEDIR_PROPERTY="screenshot.temp.imagedir";
    private static final String PID_COLLECTION_NAME_PROPERTY="pid.collection.name";
    private static final String SCREENSHOT_PREVIEW_TIMEOUT_PROPERTY="screenshot.preview.timeout";
    private static final String WARC_FILES_VERIFY_COLLECTION_PROPERTY  ="warc.files.verify.collection";
    
    private static final String SOLR_SERVER_CACHING_PROPERTY="solr.server.caching";
    private static final String SOLR_SERVER_CACHING_MAX_ENTRIES_PROPERTY="solr.server.caching.max.entries";
    private static final String SOLR_SERVER_CACHING_AGE_SECONDS_PROPERTY="solr.server.caching.age.seconds";
    public static final String SOLR_SERVER_CHECK_INTERVAL_PROPERTY = "solr.server.check.interval.seconds";

    // Used by SolrStreamShard
    public static final String SOLR_STREAM_SHARD_DIVIDE_PROPERTY = "solr.export.sharddivide.default";
    public static final String SOLR_STREAM_SHARD_AUTO_MIN_SHARDS_PROPERTY = "solr.export.sharddivide.autolimit.shards.default";
    public static final String SOLR_STREAM_SHARD_AUTO_MIN_HITS_PROPERTY = "solr.export.sharddivide.autolimit.hits.default";
    public static final String SOLR_STREAM_SHARD_DIVIDE_CONCURRENT_MAX_PROPERTY = "solr.export.sharddivide.concurrent.max";

    private static final String URL_NORMALISER_PROPERTY="url.normaliser";
    
    public static final String PLAYBACK_DISABLED_PROPERTY="playback.disabled";
    private static final String SOLR_SEARCH_PARAMS_PROPERTY="solr.search.params";

    private static Properties serviceProperties = null;
    public static boolean PLAYBACK_DISABLED = false;
    public static String SOLR_SERVER = null;
    public static String WAYBACK_BASEURL = null;
    public static String WAYBACK_HOST = null; //Taken from WAYBACK_BASEURL
    public static int WAYBACK_SERVER_PORT = 0; //Taken from WAYBACK_BASEURL
    public static String CHROME_COMMAND= null;
    public static String SCREENSHOT_TEMP_IMAGEDIR = null;
    public static String WARC_FILE_RESOLVER_CLASS = null;
    public static Map<String, String> WARC_FILE_RESOLVER_PARAMETERS= new HashMap<>();
    public static boolean WARC_SOURCE_HTTP_FALLBACK = false;
    public static String PID_COLLECTION_NAME = null;
    public static String WORDCLOUD_STOPWORDS;
    public static LinkedHashMap<String,String> SOLR_PARAMS_MAP= new LinkedHashMap<String,String>(); 

    public static boolean SOLR_SERVER_CACHING=false;
    public static boolean WARC_FILES_VERIFY_COLLECTION=false;
    public static int SOLR_SERVER_CACHING_MAX_ENTRIES=1000; //default value
    public static int SOLR_SERVER_CACHING_AGE_SECONDS=36584600; //default value 1 year (effectively disabled)
    /**
     * How often the status (available, unavailable, changed) of the backing Solr is checked.
     *
     * Set this to -1 or 0 to disable the running check.
     *
     * Used by {@link dk.kb.netarchivesuite.solrwayback.solr.IndexWatcher}
     * through {@link dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient}.
     */
    public static int SOLR_SERVER_CHECK_INTERVAL = 60*60; //default value every hour
    public static String URL_NORMALISER="normal";

    // Used by SolrStreamShard
    public static String SOLR_STREAM_SHARD_DIVIDE = "auto";
    public static long SOLR_STREAM_SHARD_AUTO_MIN_SHARDS = 2;
    public static long SOLR_STREAM_SHARD_AUTO_MIN_HITS = 5000L;
    // Maximum number of concurrent shard divided connections, shared between all shard divided calls
    public static int SOLR_STREAM_SHARD_DIVIDE_CONCURRENT_MAX = 20;

    public static int SCREENSHOT_PREVIEW_TIMEOUT = 10;//default

    public static void initProperties() {
        initProperties(DEFAULT_PROPERTY_FILE);
    }

    public static void initProperties(String propertyFile) {
        Path propertyPath = null;
        try {
            log.info("Initializing solrwayback-properties using property resource '" + propertyFile + "'");

            try {
                propertyPath = FileUtil.resolveContainerResource(propertyFile, DEFAULT_PROPERTY_FILE);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(
                        "Unable to resolve both primary '" + propertyFile +
                                "' and secondary '" + DEFAULT_PROPERTY_FILE + "' property");
            }
            log.info("Loading backend-properties '" + propertyPath + "'");
            InputStreamReader isr = new InputStreamReader(Files.newInputStream(propertyPath), StandardCharsets.UTF_8);

            serviceProperties = new Properties();
            serviceProperties.load(isr);
            isr.close();

            SOLR_SERVER =serviceProperties.getProperty(SOLR_SERVER_PROPERTY);
            WAYBACK_BASEURL = serviceProperties.getProperty(WAYBACK_BASEURL_PROPERTY);
            CHROME_COMMAND = serviceProperties.getProperty(CHROME_COMMAND_PROPERTY);
            SCREENSHOT_TEMP_IMAGEDIR = serviceProperties.getProperty(SCREENSHOT_TEMP_IMAGEDIR_PROPERTY);
            WARC_FILE_RESOLVER_CLASS = serviceProperties.getProperty(WARC_FILE_RESOLVER_CLASS_PROPERTY);
            // Legacy support
            WARC_SOURCE_HTTP_FALLBACK = Boolean.parseBoolean(serviceProperties.getProperty(WARC_SOURCE_HTTP_FALLBACK_LEGACY_PROPERTY, "false"));
            WARC_SOURCE_HTTP_FALLBACK = Boolean.parseBoolean(serviceProperties.getProperty(WARC_SOURCE_HTTP_FALLBACK_PROPERTY, Boolean.toString(WARC_SOURCE_HTTP_FALLBACK)));
            PID_COLLECTION_NAME = serviceProperties.getProperty(PID_COLLECTION_NAME_PROPERTY);
            loadArcResolverParameters(serviceProperties);
            String timeout  = serviceProperties.getProperty(SCREENSHOT_PREVIEW_TIMEOUT_PROPERTY);
            URL_NORMALISER  = serviceProperties.getProperty(URL_NORMALISER_PROPERTY,"normal");
            SOLR_STREAM_SHARD_DIVIDE = serviceProperties.getProperty(SOLR_STREAM_SHARD_DIVIDE_PROPERTY, SOLR_STREAM_SHARD_DIVIDE);
            SOLR_STREAM_SHARD_AUTO_MIN_SHARDS = Long.parseLong(serviceProperties.getProperty(SOLR_STREAM_SHARD_AUTO_MIN_SHARDS_PROPERTY, Long.toString(SOLR_STREAM_SHARD_AUTO_MIN_SHARDS)));
            SOLR_STREAM_SHARD_AUTO_MIN_HITS = Long.parseLong(serviceProperties.getProperty(SOLR_STREAM_SHARD_AUTO_MIN_HITS_PROPERTY, Long.toString(SOLR_STREAM_SHARD_AUTO_MIN_HITS)));
            SOLR_STREAM_SHARD_DIVIDE_CONCURRENT_MAX = Integer.parseInt(serviceProperties.getProperty(SOLR_STREAM_SHARD_DIVIDE_CONCURRENT_MAX_PROPERTY, Integer.toString(SOLR_STREAM_SHARD_DIVIDE_CONCURRENT_MAX)));

            URL waybacksURL = new URL (WAYBACK_BASEURL);
            WAYBACK_SERVER_PORT =  waybacksURL.getPort();
            WAYBACK_HOST =  waybacksURL.getHost();

            if (timeout != null){
                SCREENSHOT_PREVIEW_TIMEOUT = Integer.parseInt(timeout);
            }
           
            String cachingStr= serviceProperties.getProperty(SOLR_SERVER_CACHING_PROPERTY);

            if (cachingStr != null && cachingStr.equalsIgnoreCase("true")) {
                SOLR_SERVER_CACHING=true;
                if (serviceProperties.containsKey(SOLR_SERVER_CACHING_AGE_SECONDS_PROPERTY)) {
                    SOLR_SERVER_CACHING_AGE_SECONDS = Integer.parseInt(serviceProperties.getProperty(SOLR_SERVER_CACHING_AGE_SECONDS_PROPERTY).trim());
                }
                if (serviceProperties.containsKey(SOLR_SERVER_CACHING_MAX_ENTRIES_PROPERTY)) {
                    SOLR_SERVER_CACHING_MAX_ENTRIES = Integer.parseInt(serviceProperties.getProperty(SOLR_SERVER_CACHING_MAX_ENTRIES_PROPERTY).trim());
                }
            }

            SOLR_SERVER_CHECK_INTERVAL = Integer.parseInt(serviceProperties.getProperty(
                    SOLR_SERVER_CHECK_INTERVAL_PROPERTY, Integer.toString(SOLR_SERVER_CHECK_INTERVAL)));

            String verifyCollectionString = serviceProperties.getProperty(WARC_FILES_VERIFY_COLLECTION_PROPERTY,"false");            
            WARC_FILES_VERIFY_COLLECTION = Boolean.valueOf(verifyCollectionString);
            
            //Format is key1=value1,key2=value2
            String solrParamsStr = serviceProperties.getProperty( SOLR_SEARCH_PARAMS_PROPERTY);
            if (solrParamsStr != null) {
              buildSolrParams(solrParamsStr);
            }
            else {
             log.info("no solrParams loaded");   
            }
            PLAYBACK_DISABLED = Boolean.parseBoolean(serviceProperties.getProperty(PLAYBACK_DISABLED_PROPERTY));
            
            log.info("Property:"+ PLAYBACK_DISABLED_PROPERTY +" = " + PLAYBACK_DISABLED);
            log.info("Property:"+ SOLR_SERVER_PROPERTY +" = " + SOLR_SERVER);
            log.info("Property:"+ WAYBACK_BASEURL_PROPERTY +" = " + WAYBACK_BASEURL);
            log.info("Property:"+ CHROME_COMMAND_PROPERTY +" = " + CHROME_COMMAND);
            log.info("Property:"+ SCREENSHOT_TEMP_IMAGEDIR_PROPERTY +" = " + SCREENSHOT_TEMP_IMAGEDIR);
            log.info("Property:"+ SCREENSHOT_PREVIEW_TIMEOUT_PROPERTY +" = " +  SCREENSHOT_PREVIEW_TIMEOUT);
            log.info("Property:"+ WARC_FILE_RESOLVER_CLASS_PROPERTY +" = " + WARC_FILE_RESOLVER_CLASS);
            log.info("Property:"+ WARC_FILE_RESOLVER_PARAMETERS_PROPERTY +" = " + WARC_FILE_RESOLVER_PARAMETERS);
            log.info("Property:"+ WARC_SOURCE_HTTP_FALLBACK_PROPERTY + " = " + WARC_SOURCE_HTTP_FALLBACK);
            log.info("Property:"+ URL_NORMALISER_PROPERTY +" = " +  URL_NORMALISER);
            log.info("Property:"+ PID_COLLECTION_NAME_PROPERTY +" = " +  PID_COLLECTION_NAME);
            log.info("Property:"+ WARC_FILES_VERIFY_COLLECTION_PROPERTY  +" = " + WARC_FILES_VERIFY_COLLECTION);
            log.info("Property:"+ SOLR_SERVER_CACHING_PROPERTY +" = " +  SOLR_SERVER_CACHING);
            log.info("Property:"+ SOLR_SERVER_CACHING_AGE_SECONDS_PROPERTY +" = " +  SOLR_SERVER_CACHING_AGE_SECONDS);
            log.info("Property:"+ SOLR_SERVER_CACHING_MAX_ENTRIES_PROPERTY +" = " +  SOLR_SERVER_CACHING_MAX_ENTRIES);
            log.info("Property:"+ SOLR_SERVER_CHECK_INTERVAL_PROPERTY +" = " +  SOLR_SERVER_CHECK_INTERVAL);
            log.info("Property:"+ SOLR_SEARCH_PARAMS_PROPERTY+" loaded map: " +  SOLR_PARAMS_MAP);
            log.info("Property:"+ SOLR_STREAM_SHARD_DIVIDE_PROPERTY + " = " + SOLR_STREAM_SHARD_DIVIDE);
            log.info("Property:" + SOLR_STREAM_SHARD_AUTO_MIN_SHARDS_PROPERTY + " = " + SOLR_STREAM_SHARD_AUTO_MIN_SHARDS);
            log.info("Property:" + SOLR_STREAM_SHARD_AUTO_MIN_HITS_PROPERTY + " = " + SOLR_STREAM_SHARD_AUTO_MIN_HITS);
            log.info("Property:" + SOLR_STREAM_SHARD_DIVIDE_CONCURRENT_MAX_PROPERTY + " = " + SOLR_STREAM_SHARD_DIVIDE_CONCURRENT_MAX);
        } catch (Exception e) {
            e.printStackTrace(); // Acceptable as this is catastrophic
            log.error("Could not load property file '" + propertyPath + "'",e);
        }
    }

    //Format is key1=value1,key2=value2
    private static void buildSolrParams(String solrParams) {
    
        String[] params = solrParams.split(";");
        
        for (String param : params) {
            String[] keyVal=param.split("=");            
            SOLR_PARAMS_MAP.put(keyVal[0],keyVal[1]);            
        }        
    
    }
           
    /**
     * Add all properties that starts with {@link #WARC_FILE_RESOLVER_PARAMETERS_PROPERTY} to
     * {@link #WARC_FILE_RESOLVER_PARAMETERS}, with {@link #WARC_FILE_RESOLVER_PARAMETERS_PROPERTY} removed from
     * the key.
     */
    private static void loadArcResolverParameters(Properties serviceProperties) {
        for (String key: serviceProperties.stringPropertyNames()) {
            if (WARC_FILE_RESOLVER_PARAMETERS_PROPERTY.equals(key)) {
                WARC_FILE_RESOLVER_PARAMETERS.put(WARC_FILE_RESOLVER_UNQUALIFIED, serviceProperties.getProperty(key));
            } else if (key.startsWith(WARC_FILE_RESOLVER_PARAMETERS_PROPERTY + ".")) {
                String subKey = key.substring((WARC_FILE_RESOLVER_PARAMETERS_PROPERTY + ".").length());
                if (subKey.isEmpty()) {
                    log.error("Got empty subkey for property key '" + key + "'. Storing as _blank_");
                    subKey = "_blank_";
                }
                WARC_FILE_RESOLVER_PARAMETERS.put(subKey, serviceProperties.getProperty(key));
            }
        }
    }

}
