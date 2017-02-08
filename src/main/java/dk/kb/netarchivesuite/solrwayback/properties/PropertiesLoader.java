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
	private static final String WAYBACK_BASEURL_PROPERTY="wayback.baseurl";
	
	
	public static String SOLR_SERVER = null;
	public static String WAYBACK_BASEURL = null;
		
	public static void initProperties()  throws Exception{
	    try {
	    
	    log.info("Initializing webarchiveminetypeservlet-properties");
	    
		String user_home=System.getProperty("user.home");
		log.info("Load properties: Using user.home folder:" + user_home);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(user_home,PROPERTY_FILE)), "ISO-8859-1");

		Properties serviceProperties = new Properties();
		serviceProperties.load(isr);
		isr.close();

		SOLR_SERVER =serviceProperties.getProperty(SOLR_SERVER_PROPERTY);		
		WAYBACK_BASEURL = serviceProperties.getProperty(WAYBACK_BASEURL_PROPERTY);
		
		log.info("Property:"+ SOLR_SERVER_PROPERTY +" = " + SOLR_SERVER);
		log.info("Property:"+ WAYBACK_BASEURL_PROPERTY +" = " + WAYBACK_BASEURL);
	    }
		catch (Exception e) {
            e.printStackTrace();
            log.error("Could not load property file:"+ PROPERTY_FILE);                  
        }
	}
	
}
