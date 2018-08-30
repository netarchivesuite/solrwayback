package dk.kb.netarchivesuite.solrwayback.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesLoaderWeb {

	private static final Logger log = LoggerFactory.getLogger(PropertiesLoaderWeb.class);
	private static final String DEFAULT_PROPERTY_WEB_FILE = "solrwaybackweb.properties";

	public static final String WAYBACK_SERVER_PROPERTY="wayback.baseurl";
	public static final String OPENWAYBACK_SERVER_PROPERTY="openwayback.baseurl";
	public static final String GOOGLE_API_KEY_PROPERTY="google.api.key";
	private static final String FACETS_PROPERTY = "facets";	
	public static final String GOOGLE_MAPS_LATITUDE_PROPERTY = "google.maps.latitude";
	public static final String GOOGLE_MAPS_LONGITUDE_PROPERTY = "google.maps.longitude";
	public static final String GOOGLE_MAPS_RADIUS_PROPERTY = "google.maps.radius";
	public static final String ALLOW_EXPORT_WARC_PROPERTY = "allow.export.warc";
	
	
	
	public static String OPENWAYBACK_SERVER;
	public static String GOOGLE_API_KEY=null;
	public static String WAYBACK_SERVER = null;
    public static String GOOGLE_MAPS_LATITUDE;
    public static String GOOGLE_MAPS_LONGITUDE;
    public static String GOOGLE_MAPS_RADIUS;
    public static boolean ALLOW_EXPORT_WARC;
    
		
	private static Properties serviceProperties = null;
	//Default values.
	public static List<String> FACETS = Arrays.asList("domain", "content_type_norm", "type", "crawl_year", "status_code", "public_suffix"); 

	  public static void initProperties() {
	      initProperties(DEFAULT_PROPERTY_WEB_FILE);      
	    }
	    
	
	public static void initProperties(String propertyFile) {
		try {

			log.info("Initializing solrwaybackweb-properties");
	        String user_home=System.getProperty("user.home");

			File f = new File(user_home,propertyFile);
            if (!f.exists()) {
              log.info("Could not find contextroot specific propertyfile:"+propertyFile +". Using default:"+DEFAULT_PROPERTY_WEB_FILE);
              propertyFile=DEFAULT_PROPERTY_WEB_FILE;                                 
            }                        
           log.info("Load web-properties: Using user.home folder:" + user_home +" and propertyFile:"+propertyFile);
			
			
			InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(user_home,propertyFile)), "ISO-8859-1");

			serviceProperties = new Properties();
			serviceProperties.load(isr);
			isr.close();

			WAYBACK_SERVER =serviceProperties.getProperty(WAYBACK_SERVER_PROPERTY);
		    FACETS = Arrays.asList(getProperty(FACETS_PROPERTY, StringUtils.join(FACETS, ",")).split(", *"));
		    GOOGLE_API_KEY =serviceProperties.getProperty(GOOGLE_API_KEY_PROPERTY);
		    OPENWAYBACK_SERVER = serviceProperties.getProperty(OPENWAYBACK_SERVER_PROPERTY);
		    GOOGLE_MAPS_LATITUDE = serviceProperties.getProperty(GOOGLE_MAPS_LATITUDE_PROPERTY);
		    GOOGLE_MAPS_LONGITUDE = serviceProperties.getProperty(GOOGLE_MAPS_LONGITUDE_PROPERTY);
		    GOOGLE_MAPS_RADIUS = serviceProperties.getProperty(GOOGLE_MAPS_RADIUS_PROPERTY);
		    ALLOW_EXPORT_WARC = Boolean.parseBoolean(serviceProperties.getProperty(ALLOW_EXPORT_WARC_PROPERTY));
		    
		    log.info("Property:"+ OPENWAYBACK_SERVER_PROPERTY +" = " + OPENWAYBACK_SERVER);
			log.info("Property:"+ ALLOW_EXPORT_WARC_PROPERTY +" = " + ALLOW_EXPORT_WARC);
			log.info("Property:"+ WAYBACK_SERVER_PROPERTY +" = " + WAYBACK_SERVER);
			log.info("Property:"+ GOOGLE_API_KEY_PROPERTY+" = " + GOOGLE_API_KEY);
			log.info("Property:"+ GOOGLE_MAPS_LATITUDE_PROPERTY+" = " +GOOGLE_MAPS_LATITUDE);
			log.info("Property:"+ GOOGLE_MAPS_LONGITUDE_PROPERTY+" = " +GOOGLE_MAPS_LONGITUDE);
			log.info("Property:"+ GOOGLE_MAPS_RADIUS_PROPERTY+" = " + GOOGLE_MAPS_RADIUS);
			log.info("Property:"+ FACETS_PROPERTY +" = " + FACETS);
		
		}
		catch (Exception e) {
			e.printStackTrace();
			log.error("Could not load property file:"+ propertyFile);
		}
	}

	public static String getProperty(String key, String defaultValue) {
	    if (serviceProperties == null) {
	        initProperties();
        }
        Object o = serviceProperties.getProperty(key);
	    return o == null ? defaultValue : o.toString();
    }

}
