package dk.kb.netarchivesuite.solrwayback.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesLoaderWeb {

    private static final Logger log = LoggerFactory.getLogger(PropertiesLoaderWeb.class);
    private static final String DEFAULT_PROPERTY_WEB_FILE = "solrwaybackweb.properties";

    
    public static final String WAYBACK_SERVER_PROPERTY="wayback.baseurl";
    public static final String WEBAPP_BASEURL_PROPERTY="webapp.baseurl";  //TODO DELETE WHEN FRONTEND HAS CHANGED NAME
    public static final String WEBAPP_PREFIX_PROPERTY="webapp.prefix";
    
    public static final String OPENWAYBACK_SERVER_PROPERTY="openwayback.baseurl";	
    public static final String ALTERNATIVE_PLAYBACK_COLLECTION_MAPPING_PROPERTY="alternative.playback.collection.mapping";
    
    public static final String FACETS_PROPERTY = "facets";	
    public static final String FIELDS_PROPERTY = "fields";
    public static final String MAPS_LATITUDE_PROPERTY = "maps.latitude";
    public static final String MAPS_LONGITUDE_PROPERTY = "maps.longitude";
    public static final String MAPS_RADIUS_PROPERTY = "maps.radius";
    public static final String ALLOW_EXPORT_WARC_PROPERTY = "allow.export.warc";
    public static final String ALLOW_EXPORT_CSV_PROPERTY = "allow.export.csv";
    public static final String WORDCLOUD_STOPWORDS_PROPERTY="wordcloud.stopwords";    
    public static final String SEARCH_UPLOADED_FILE_DISABLED_PROPERTY="search.uploaded.file.disabled";
    public static final String SEARCH_PAGINATION_PROPERTY = "search.pagination";
    
    public static final String EXPORT_WARC_MAXRESULTS_PROPERTY = "export.warc.maxresults";
    public static final String EXPORT_CSV_MAXRESULTS_PROPERTY = "export.csv.maxresults";
    public static final String EXPORT_WARC_EXPANDED_MAXRESULTS_PROPERTY = "export.warc.expanded.maxresults";

    public static final String EXPORT_CSV_FIELDS_PROPERTY = "export.csv.fields";
    public static final String ABOUT_TEXT_FILE_PROPERTY = "about.text.file";
    public static final String SEARCH_HELP_FILE_PROPERTY = "search.help.text.file";
    public static final String COLLECTION_TEXT_FILE_PROPERTY = "collection.text.file";
    public static final String ARCHIVE_START_YEAR_PROPERTY = "archive.start.year";

    public static final String LEAFLET_SOURCE_PROPERTY = "leaflet.source";
    public static final String LEAFLET_ATTRIBUTION_PROPERTY = "leaflet.attribution";
    public static final String TOP_LEFT_LOGO_IMAGE_PROPERTY = "top.left.logo.image";
    public static final String TOP_LEFT_LOGO_IMAGE_LINK_PROPERTY = "top.left.logo.image.link";
 
    
    
    public static LinkedHashMap<String,String> ALTERNATIVE_PLAYBACK_COLLECTION_MAPPING= new LinkedHashMap<String,String>(); 
    public static String SOLRWAYBACK_VERSION; //Will be set from initialcontext-listener
    public static String OPENWAYBACK_SERVER;
    public static int ARCHIVE_START_YEAR;
    public static String WAYBACK_SERVER = null;
    
    public static String WEBAPP_PREFIX = null;
    public static String MAPS_LATITUDE;
    public static String MAPS_LONGITUDE;
    public static String MAPS_RADIUS;
    
    public static long EXPORT_CSV_MAXRESULTS=10000000;// 10M default
    public static long EXPORT_WARC_MAXRESULTS=1000000; // 1M default
    public static long EXPORT_WARC_EXPANDED_MAXRESULTS=100000; // 500K default   
    public static boolean ALLOW_EXPORT_WARC;
    public static boolean ALLOW_EXPORT_CSV;
    public static String  EXPORT_CSV_FIELDS;;
    public static boolean SEARCH_UPLOADED_FILE_DISABLED;
    public static Long SEARCH_PAGINATION = 20L; // 20 default
    
    public static String LEAFLET_SOURCE;
    public static String LEAFLET_ATTRIBUTION;
    public static String ABOUT_TEXT_FILE;
    public static String SEARCH_HELP_TEXT_FILE;
    public static String COLLECTION_TEXT_FILE;
    public static String TOP_LEFT_LOGO_IMAGE;
    public static String TOP_LEFT_LOGO_IMAGE_LINK;

    private static Properties serviceProperties = null;
    //Default values.
    public static List<String> FACETS = Arrays.asList("domain", "content_type_norm", "type", "crawl_year", "status_code", "public_suffix"); 
    public static String FIELDS=null;
    
    //Default empty if not defined in properties
    public static  List<String> WORDCLOUD_STOPWORDS = new ArrayList<String>();
    
    public static void initProperties() {
        initProperties(DEFAULT_PROPERTY_WEB_FILE);      
    }        

    public static void initProperties(String propertyFile) {
        try {

            log.info("Initializing solrwaybackweb-properties using property file '" + propertyFile + "'");
            String user_home=System.getProperty("user.home");

            File f = new File(propertyFile);
            if (!f.exists()) { // Fallback to looking in the user home folder
                f = new File(user_home, propertyFile);
            }
            if (!f.exists()) {
                log.info("Could not find contextroot specific propertyfile:"+propertyFile +". Using default:"+DEFAULT_PROPERTY_WEB_FILE);
                f = new File(user_home, DEFAULT_PROPERTY_WEB_FILE);
            }
            log.info("Load web-properties: Using user.home folder:" + user_home +" and propertyFile:"+propertyFile);


            InputStreamReader isr = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);

            serviceProperties = new Properties();
            serviceProperties.load(isr);
            isr.close();

            WAYBACK_SERVER =serviceProperties.getProperty(WAYBACK_SERVER_PROPERTY);
            FACETS = Arrays.asList(getProperty(FACETS_PROPERTY, StringUtils.join(FACETS, ",")).split(", *"));		
            WORDCLOUD_STOPWORDS = Arrays.asList(getProperty(WORDCLOUD_STOPWORDS_PROPERTY, StringUtils.join(WORDCLOUD_STOPWORDS, ",")).split(", *"));
            WEBAPP_PREFIX = serviceProperties.getProperty(WEBAPP_PREFIX_PROPERTY,"/solrwayback/"); //Default to /solrwayback/ if not defined
            OPENWAYBACK_SERVER = serviceProperties.getProperty(OPENWAYBACK_SERVER_PROPERTY);
            MAPS_LATITUDE = serviceProperties.getProperty(MAPS_LATITUDE_PROPERTY);
            MAPS_LONGITUDE = serviceProperties.getProperty(MAPS_LONGITUDE_PROPERTY);
            MAPS_RADIUS = serviceProperties.getProperty(MAPS_RADIUS_PROPERTY);
            ALLOW_EXPORT_WARC = Boolean.parseBoolean(serviceProperties.getProperty(ALLOW_EXPORT_WARC_PROPERTY));
            ALLOW_EXPORT_CSV = Boolean.parseBoolean(serviceProperties.getProperty(ALLOW_EXPORT_CSV_PROPERTY));
            
            SEARCH_UPLOADED_FILE_DISABLED = Boolean.parseBoolean(serviceProperties.getProperty(SEARCH_UPLOADED_FILE_DISABLED_PROPERTY));
            EXPORT_CSV_FIELDS = serviceProperties.getProperty(EXPORT_CSV_FIELDS_PROPERTY);
            
            ABOUT_TEXT_FILE = serviceProperties.getProperty(ABOUT_TEXT_FILE_PROPERTY);
            SEARCH_HELP_TEXT_FILE = serviceProperties.getProperty(SEARCH_HELP_FILE_PROPERTY);
            COLLECTION_TEXT_FILE = serviceProperties.getProperty(COLLECTION_TEXT_FILE_PROPERTY);

            LEAFLET_SOURCE = serviceProperties.getProperty(LEAFLET_SOURCE_PROPERTY);
            LEAFLET_ATTRIBUTION = serviceProperties.getProperty(LEAFLET_ATTRIBUTION_PROPERTY);
            TOP_LEFT_LOGO_IMAGE = serviceProperties.getProperty(TOP_LEFT_LOGO_IMAGE_PROPERTY);
            TOP_LEFT_LOGO_IMAGE_LINK = serviceProperties.getProperty(TOP_LEFT_LOGO_IMAGE_LINK_PROPERTY);
                        
            String csv_max_results= serviceProperties.getProperty(EXPORT_CSV_MAXRESULTS_PROPERTY);
            String warc_max_results= serviceProperties.getProperty(EXPORT_WARC_MAXRESULTS_PROPERTY);
            String warc_expanded_max_results= serviceProperties.getProperty(EXPORT_WARC_EXPANDED_MAXRESULTS_PROPERTY);
            String search_pagination= serviceProperties.getProperty(SEARCH_PAGINATION_PROPERTY);

            if (csv_max_results != null) {
                EXPORT_CSV_MAXRESULTS  = Long.parseLong(csv_max_results.trim());                
            }
            
            if (warc_max_results != null) {
                EXPORT_WARC_MAXRESULTS  = Long.parseLong(warc_max_results.trim());               
            }

            if ( warc_expanded_max_results != null) {                
                EXPORT_WARC_EXPANDED_MAXRESULTS  = Long.parseLong( warc_expanded_max_results.trim());               
            }

            if ( search_pagination != null) {
                SEARCH_PAGINATION  = Long.parseLong(search_pagination.trim());
            }

            if (ABOUT_TEXT_FILE == null || (ABOUT_TEXT_FILE = ABOUT_TEXT_FILE.trim()).isEmpty()) {
                ABOUT_TEXT_FILE = "/about_this_archive.txt";
                log.warn("about.text.file in solrwaybackweb.properties is not set. Using default: /about_this_archive.txt");
            }

            if (SEARCH_HELP_TEXT_FILE == null || (SEARCH_HELP_TEXT_FILE = SEARCH_HELP_TEXT_FILE.trim()).isEmpty()) {
                SEARCH_HELP_TEXT_FILE = "/search_help.txt";
                log.warn("search.help.text.file in solrwaybackweb.properties is not set. Using default: /search_help.txt");
            }

            if (COLLECTION_TEXT_FILE == null || (COLLECTION_TEXT_FILE = COLLECTION_TEXT_FILE.trim()).isEmpty()) {
                COLLECTION_TEXT_FILE = "/about_collection.txt";
                log.warn("collection.text.file in solrwaybackweb.properties is not set. Using default: /about_collection.txt");
            }
                        
            // start year
            String startYearStr = serviceProperties.getProperty(ARCHIVE_START_YEAR_PROPERTY);
            if (startYearStr != null) {
                int startYear = Integer.parseInt(startYearStr);
                if (startYear<=1980 || startYear >= YearMonth.now().getYear() ) {
                    log.warn("Default start year (archive.start.year) for archive in solrwaybackweb.properties is outside expected range. Value:"+startYear);            		            
                }
                else {
                    ARCHIVE_START_YEAR=Integer.parseInt(startYearStr);
                }
            }
            else{
                log.warn("Default start year (archive.start.year) for archive not define in solrwaybackweb.properties. Default value is 1998.");
                ARCHIVE_START_YEAR=1998;
            }

            String fieldsStr = serviceProperties.getProperty(FIELDS_PROPERTY);
            if (fieldsStr != null) {
                FIELDS=fieldsStr;                
            }
                        
            
            //Format is key1=value1,key2=value2
            String mapping= serviceProperties.getProperty(ALTERNATIVE_PLAYBACK_COLLECTION_MAPPING_PROPERTY);
            if (mapping != null) {
              buildPlaybackCollectionMapping(mapping);
            }
            else {
             log.info("No collection playback mapping loaded.");   
            }
            //Set max export sizes                                   
            log.info("Property:"+ WEBAPP_PREFIX_PROPERTY +" = " + WEBAPP_PREFIX);
            log.info("Property:"+ OPENWAYBACK_SERVER_PROPERTY +" = " + OPENWAYBACK_SERVER);
            log.info("Property:"+ ALLOW_EXPORT_WARC_PROPERTY +" = " + ALLOW_EXPORT_WARC);
            log.info("Property:"+ ALLOW_EXPORT_CSV_PROPERTY +" = " + ALLOW_EXPORT_CSV);
            log.info("Property:"+ EXPORT_CSV_MAXRESULTS_PROPERTY +" = " + EXPORT_CSV_MAXRESULTS);
            log.info("Property:"+ EXPORT_WARC_MAXRESULTS_PROPERTY +" = " + EXPORT_WARC_MAXRESULTS);
            log.info("Property:"+ EXPORT_WARC_EXPANDED_MAXRESULTS_PROPERTY +" = " + EXPORT_WARC_EXPANDED_MAXRESULTS);
            log.info("Property:"+ EXPORT_CSV_FIELDS_PROPERTY +" = " + EXPORT_CSV_FIELDS);
            log.info("Property:"+ WAYBACK_SERVER_PROPERTY +" = " + WAYBACK_SERVER);			
            log.info("Property:"+ MAPS_LATITUDE_PROPERTY+" = " +MAPS_LATITUDE);
            log.info("Property:"+ MAPS_LONGITUDE_PROPERTY+" = " +MAPS_LONGITUDE);
            log.info("Property:"+ MAPS_RADIUS_PROPERTY+" = " + MAPS_RADIUS);
            log.info("Property:"+ FACETS_PROPERTY +" = " + FACETS);
            log.info("Property:"+ WORDCLOUD_STOPWORDS_PROPERTY +" = " + WORDCLOUD_STOPWORDS);        
            log.info("Property:"+ SEARCH_UPLOADED_FILE_DISABLED_PROPERTY+" = " +SEARCH_UPLOADED_FILE_DISABLED);            
            log.info("Property:"+ SEARCH_PAGINATION_PROPERTY + " = " + SEARCH_PAGINATION);
            log.info("Property:"+ ABOUT_TEXT_FILE_PROPERTY +" = " + ABOUT_TEXT_FILE);
            log.info("Property:"+ SEARCH_HELP_FILE_PROPERTY +" = " + SEARCH_HELP_TEXT_FILE );
            log.info("Property:"+ COLLECTION_TEXT_FILE_PROPERTY +" = " + COLLECTION_TEXT_FILE );
            log.info("Property:"+ ARCHIVE_START_YEAR_PROPERTY +" = " + ARCHIVE_START_YEAR);			
            log.info("Property:"+ LEAFLET_SOURCE_PROPERTY +" = " + LEAFLET_SOURCE);
            log.info("Property:"+ LEAFLET_ATTRIBUTION_PROPERTY +" = " + LEAFLET_ATTRIBUTION);
            log.info("Property:"+ TOP_LEFT_LOGO_IMAGE_PROPERTY +" = " + TOP_LEFT_LOGO_IMAGE);
            log.info("Property:"+ TOP_LEFT_LOGO_IMAGE_LINK_PROPERTY +" = " + TOP_LEFT_LOGO_IMAGE_LINK);
            
            if (ALTERNATIVE_PLAYBACK_COLLECTION_MAPPING.size() >0) {
                for (String key : ALTERNATIVE_PLAYBACK_COLLECTION_MAPPING.keySet())
                {
                    log.info("Collection playback mapping:"+ key +" = " + ALTERNATIVE_PLAYBACK_COLLECTION_MAPPING.get(key));                
                }                                
            }
            
        }
        catch (Exception e) {
            e.printStackTrace();
            log.error("Could not load property file:"+ propertyFile);
            // TODO: This should be a catastrophic failure as the properties contains security oriented settings
        }
        
    }

    public static String getProperty(String key, String defaultValue) {
        if (serviceProperties == null) {
            initProperties();
        }
        Object o = serviceProperties.getProperty(key);
        return o == null ? defaultValue : o.toString();
    }
    
    //Format is key1=value1,key2=value2
    private static void buildPlaybackCollectionMapping(String mapping) {
    
        String[] params = mapping.split(";");
        
        for (String param : params) {
            String[] keyVal=param.split("=");            
            ALTERNATIVE_PLAYBACK_COLLECTION_MAPPING.put(keyVal[0],keyVal[1]);            
        }        
    
    }
    
    
}