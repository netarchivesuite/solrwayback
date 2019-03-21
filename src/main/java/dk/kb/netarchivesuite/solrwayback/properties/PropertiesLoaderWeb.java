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
	private static final String FACETS_PROPERTY = "facets";	
	public static final String MAPS_LATITUDE_PROPERTY = "maps.latitude";
	public static final String MAPS_LONGITUDE_PROPERTY = "maps.longitude";
	public static final String MAPS_RADIUS_PROPERTY = "maps.radius";
	public static final String ALLOW_EXPORT_WARC_PROPERTY = "allow.export.warc";
	public static final String ALLOW_EXPORT_CSV_PROPERTY = "allow.export.csv";
	
	
	public static final String LEAFLET_SOURCE_PROPERTY = "leaflet.source";
	public static final String LEAFLET_ATTRIBUTION_PROPERTY = "leaflet.attribution";
	
	
	
	public static String OPENWAYBACK_SERVER;
	public static String WAYBACK_SERVER = null;
    public static String MAPS_LATITUDE;
    public static String MAPS_LONGITUDE;
    public static String MAPS_RADIUS;
    
    public static boolean ALLOW_EXPORT_WARC;
    public static boolean ALLOW_EXPORT_CSV;

    public static String LEAFLET_SOURCE;
    public static String LEAFLET_ATTRIBUTION;
    
		
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
		    OPENWAYBACK_SERVER = serviceProperties.getProperty(OPENWAYBACK_SERVER_PROPERTY);
		    MAPS_LATITUDE = serviceProperties.getProperty(MAPS_LATITUDE_PROPERTY);
		    MAPS_LONGITUDE = serviceProperties.getProperty(MAPS_LONGITUDE_PROPERTY);
		    MAPS_RADIUS = serviceProperties.getProperty(MAPS_RADIUS_PROPERTY);
		    ALLOW_EXPORT_WARC = Boolean.parseBoolean(serviceProperties.getProperty(ALLOW_EXPORT_WARC_PROPERTY));
	        ALLOW_EXPORT_CSV = Boolean.parseBoolean(serviceProperties.getProperty(ALLOW_EXPORT_CSV_PROPERTY));
		    
		    
            LEAFLET_SOURCE = serviceProperties.getProperty(LEAFLET_SOURCE_PROPERTY);
		    LEAFLET_ATTRIBUTION = serviceProperties.getProperty(LEAFLET_ATTRIBUTION_PROPERTY);
		    
		    log.info("Property:"+ OPENWAYBACK_SERVER_PROPERTY +" = " + OPENWAYBACK_SERVER);
			log.info("Property:"+ ALLOW_EXPORT_WARC_PROPERTY +" = " + ALLOW_EXPORT_WARC);
			log.info("Property:"+ ALLOW_EXPORT_CSV_PROPERTY +" = " + ALLOW_EXPORT_CSV);
			log.info("Property:"+ WAYBACK_SERVER_PROPERTY +" = " + WAYBACK_SERVER);			
			log.info("Property:"+ MAPS_LATITUDE_PROPERTY+" = " +MAPS_LATITUDE);
			log.info("Property:"+ MAPS_LONGITUDE_PROPERTY+" = " +MAPS_LONGITUDE);
			log.info("Property:"+ MAPS_RADIUS_PROPERTY+" = " + MAPS_RADIUS);
			log.info("Property:"+ FACETS_PROPERTY +" = " + FACETS);

			log.info("Property:"+ LEAFLET_SOURCE_PROPERTY +" = " + LEAFLET_SOURCE);
			log.info("Property:"+ LEAFLET_ATTRIBUTION_PROPERTY +" = " + LEAFLET_ATTRIBUTION);
		
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
