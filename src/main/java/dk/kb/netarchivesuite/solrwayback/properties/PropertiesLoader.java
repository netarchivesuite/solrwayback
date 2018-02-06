package dk.kb.netarchivesuite.solrwayback.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesLoader {
	
	private static final Logger log = LoggerFactory.getLogger(PropertiesLoader.class);
	private static final String PROPERTY_FILE = "solrwayback.properties";
	 
	private static final String SOLR_SERVER_PROPERTY="solr.server";
	private static final String PROXY_PORT_PROPERTY="proxy.port";
	private static final String PROXY_ALLOW_HOSTS_PROPERTY="proxy.allow.hosts";
	
	private static final String WAYBACK_BASEURL_PROPERTY="wayback.baseurl";
	private static final String PHANTOMJS_RASTERIZE_FILE_PROPERTY="phantomjs.rasterize.file";
	private static final String PHANTOMJS_TEMP_IMAGEDIR_PROPERTY="phantomjs.temp.imagedir";
	
	public static String SOLR_SERVER = null;
	public static String WAYBACK_BASEURL = null;
	public static String PHANTOMJS_RASTERIZE_FILE = null;
	public static String PHANTOMJS_TEMP_IMAGEDIR = null;
	public static String PROXY_PORT= null;
	public static String PROXY_ALLOW_HOSTS= null;
	
	
	public static void initProperties()  throws Exception{
	    try {
	    
	    log.info("Initializing solrwayback-properties");
	    
		String user_home=System.getProperty("user.home");
		log.info("Load properties: Using user.home folder:" + user_home);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(user_home,PROPERTY_FILE)), "ISO-8859-1");

		Properties serviceProperties = new Properties();
		serviceProperties.load(isr);
		isr.close();

		SOLR_SERVER =serviceProperties.getProperty(SOLR_SERVER_PROPERTY);		
		WAYBACK_BASEURL = serviceProperties.getProperty(WAYBACK_BASEURL_PROPERTY);
		PHANTOMJS_RASTERIZE_FILE = serviceProperties.getProperty(PHANTOMJS_RASTERIZE_FILE_PROPERTY); 
		PHANTOMJS_TEMP_IMAGEDIR = serviceProperties.getProperty(PHANTOMJS_TEMP_IMAGEDIR_PROPERTY);
		PROXY_PORT = serviceProperties.getProperty(PROXY_PORT_PROPERTY);		
		PROXY_ALLOW_HOSTS = serviceProperties.getProperty(PROXY_ALLOW_HOSTS_PROPERTY);
		
		log.info("Property:"+ SOLR_SERVER_PROPERTY +" = " + SOLR_SERVER);
		log.info("Property:"+ WAYBACK_BASEURL_PROPERTY +" = " + WAYBACK_BASEURL);
		log.info("Property:"+ PROXY_PORT_PROPERTY +" = " + PROXY_PORT);
		log.info("Property:"+ PROXY_ALLOW_HOSTS_PROPERTY +" = " + PROXY_ALLOW_HOSTS);		
		log.info("Property:"+ PHANTOMJS_RASTERIZE_FILE_PROPERTY +" = " + PHANTOMJS_RASTERIZE_FILE);
		log.info("Property:"+ PHANTOMJS_TEMP_IMAGEDIR_PROPERTY +" = " + PHANTOMJS_TEMP_IMAGEDIR);
		
	    }
		catch (Exception e) {
            e.printStackTrace();
            log.error("Could not load property file:"+ PROPERTY_FILE);                  
        }
	}
	
}
