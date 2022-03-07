package dk.kb.netarchivesuite.solrwayback.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    private static final String WAYBACK_BASEURL_PROPERTY="wayback.baseurl";
    private static final String CHROME_COMMAND_PROPERTY="chrome.command";
    private static final String SCREENSHOT_TEMP_IMAGEDIR_PROPERTY="screenshot.temp.imagedir";
    private static final String PID_COLLECTION_NAME_PROPERTY="pid.collection.name";
    private static final String SCREENSHOT_PREVIEW_TIMEOUT_PROPERTY="screenshot.preview.timeout";

    private static final String SOLR_SERVER_CACHING_PROPERTY="solr.server.caching";
    private static final String SOLR_SERVER_CACHING_MAX_ENTRIES_PROPERTY="solr.server.caching.max.entries";
    private static final String SOLR_SERVER_CACHING_AGE_SECONDS_PROPERTY="solr.server.caching.age.seconds";

    private static final String WARC_INDEXER_URL_NORMALIZER_LEGACY_PROPERTY="warcindexer.urlnormaliser.legacy";
    
    private static final String SOLR_SEARCH_PARAMS_PROPERTY="solr.search.params";
    private static final String NORMALISE_URLS_PROPERTY="normalise.urls";
    
    private static Properties serviceProperties = null;

    public static String SOLR_SERVER = null;
    public static String WAYBACK_BASEURL = null;
    public static String WAYBACK_HOST = null; //Taken from WAYBACK_BASEURL
    public static int WAYBACK_SERVER_PORT = 0; //Taken from WAYBACK_BASEURL
    public static String CHROME_COMMAND= null;
    public static String SCREENSHOT_TEMP_IMAGEDIR = null;
    public static String WARC_FILE_RESOLVER_CLASS = null;
    public static Map<String, String> WARC_FILE_RESOLVER_PARAMETERS= new HashMap<>();
    public static String PID_COLLECTION_NAME = null;
    public static String WORDCLOUD_STOPWORDS;
    public static HashMap<String,String> SOLR_PARAMS_MAP= new HashMap<String,String>(); 

    public static boolean SOLR_SERVER_CACHING=false;
    public static int SOLR_SERVER_CACHING_MAX_ENTRIES=1000; //default value
    public static int SOLR_SERVER_CACHING_AGE_SECONDS=84600; //default value 1 day
 

    public static int SCREENSHOT_PREVIEW_TIMEOUT = 10;//default
    public static boolean WARC_INDEXER_URL_NORMALIZER_LEGACY=false; //default
    public static boolean NORMALISE_URLS=true; //default
    public static void initProperties() {
        initProperties(DEFAULT_PROPERTY_FILE);
    }

    public static void initProperties(String propertyFile) {
        try {

            log.info("Initializing solrwayback-properties using property file '" + propertyFile + "'");
            String user_home=System.getProperty("user.home");

            File f = new File(propertyFile);
            if (!f.exists()) { // Fallback to looking in the user home folder
                f = new File(user_home, propertyFile);
            }
            if (!f.exists()) {
                log.info("Could not find contextroot specific propertyfile:"+propertyFile +". Using default:"+DEFAULT_PROPERTY_FILE);
                f = new File(user_home, DEFAULT_PROPERTY_FILE);
            }
            log.info("Load backend-properties: Using user.home folder:" + user_home +" and propertyFile:"+propertyFile);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);

            serviceProperties = new Properties();
            serviceProperties.load(isr);
            isr.close();

            SOLR_SERVER =serviceProperties.getProperty(SOLR_SERVER_PROPERTY);
            WAYBACK_BASEURL = serviceProperties.getProperty(WAYBACK_BASEURL_PROPERTY);
            CHROME_COMMAND = serviceProperties.getProperty(CHROME_COMMAND_PROPERTY);
            SCREENSHOT_TEMP_IMAGEDIR = serviceProperties.getProperty(SCREENSHOT_TEMP_IMAGEDIR_PROPERTY);
            WARC_FILE_RESOLVER_CLASS = serviceProperties.getProperty(WARC_FILE_RESOLVER_CLASS_PROPERTY);
            PID_COLLECTION_NAME = serviceProperties.getProperty(PID_COLLECTION_NAME_PROPERTY);
            loadArcResolverParameters(serviceProperties);
            String timeout  = serviceProperties.getProperty(SCREENSHOT_PREVIEW_TIMEOUT_PROPERTY);
            String legacyUrlNormalizer  = serviceProperties.getProperty(WARC_INDEXER_URL_NORMALIZER_LEGACY_PROPERTY);

            URL waybacksURL = new URL (WAYBACK_BASEURL);
            WAYBACK_SERVER_PORT =  waybacksURL.getPort();
            WAYBACK_HOST =  waybacksURL.getHost();

            if (timeout != null){
                SCREENSHOT_PREVIEW_TIMEOUT = Integer.parseInt(timeout);
            }
            if (legacyUrlNormalizer != null){
                WARC_INDEXER_URL_NORMALIZER_LEGACY= Boolean.valueOf(legacyUrlNormalizer);
            }

            String cachingStr= serviceProperties.getProperty(SOLR_SERVER_CACHING_PROPERTY);

            if (cachingStr != null && cachingStr.equalsIgnoreCase("true")) {
                SOLR_SERVER_CACHING=true;
                SOLR_SERVER_CACHING_AGE_SECONDS=Integer.parseInt(serviceProperties.getProperty(SOLR_SERVER_CACHING_AGE_SECONDS_PROPERTY).trim());
                SOLR_SERVER_CACHING_MAX_ENTRIES=Integer.parseInt(serviceProperties.getProperty(SOLR_SERVER_CACHING_MAX_ENTRIES_PROPERTY).trim());
            }

            //Format is key1=value1,key2=value2
            String solrParamsStr = serviceProperties.getProperty( SOLR_SEARCH_PARAMS_PROPERTY);
            if (solrParamsStr != null) {
              buildSolrParams(solrParamsStr);
            }
            else {
             log.info("no solrParams loaded");   
            }
            
            String normaliseStr=serviceProperties.getProperty(NORMALISE_URLS_PROPERTY);
            if (normaliseStr != null) {
                NORMALISE_URLS = !"false".equalsIgnoreCase(normaliseStr); 
            }

            log.info("Property:"+ SOLR_SERVER_PROPERTY +" = " + SOLR_SERVER);
            log.info("Property:"+ WAYBACK_BASEURL_PROPERTY +" = " + WAYBACK_BASEURL);
            log.info("Property:"+ CHROME_COMMAND_PROPERTY +" = " + CHROME_COMMAND);
            log.info("Property:"+ SCREENSHOT_TEMP_IMAGEDIR_PROPERTY +" = " + SCREENSHOT_TEMP_IMAGEDIR);
            log.info("Property:"+ SCREENSHOT_PREVIEW_TIMEOUT_PROPERTY +" = " +  SCREENSHOT_PREVIEW_TIMEOUT);
            log.info("Property:"+ WARC_FILE_RESOLVER_CLASS_PROPERTY +" = " + WARC_FILE_RESOLVER_CLASS);
            log.info("Property:"+ WARC_FILE_RESOLVER_PARAMETERS_PROPERTY +" = " + WARC_FILE_RESOLVER_PARAMETERS);
            log.info("Property:"+ WARC_INDEXER_URL_NORMALIZER_LEGACY_PROPERTY +" = " +  WARC_INDEXER_URL_NORMALIZER_LEGACY);
            log.info("Property:"+ PID_COLLECTION_NAME_PROPERTY +" = " +  PID_COLLECTION_NAME);

            log.info("Property:"+ SOLR_SERVER_CACHING_PROPERTY +" = " +  SOLR_SERVER_CACHING);
            log.info("Property:"+ SOLR_SERVER_CACHING_AGE_SECONDS_PROPERTY +" = " +  SOLR_SERVER_CACHING_AGE_SECONDS);
            log.info("Property:"+ SOLR_SERVER_CACHING_MAX_ENTRIES_PROPERTY +" = " +  SOLR_SERVER_CACHING_MAX_ENTRIES);
            log.info("Property:"+ SOLR_SEARCH_PARAMS_PROPERTY+" loaded map: " +  SOLR_PARAMS_MAP);
            log.info("Property:"+ NORMALISE_URLS_PROPERTY+" = " +  NORMALISE_URLS);
            
        }
        catch (Exception e) {
            e.printStackTrace();
            log.error("Could not load property file:"+propertyFile,e);
            e.printStackTrace();
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
