package dk.kb.netarchivesuite.solrwayback.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesLoaderWeb {

	private static final Logger log = LoggerFactory.getLogger(PropertiesLoaderWeb.class);
	private static final String PROPERTY_FILE = "solrwaybackweb.properties";

	public static final String WAYBACK_SERVER_PROPERTY="wayback.baseurl";
	public static String WAYBACK_SERVER = null;
	private static Properties serviceProperties = null;

	public static void initProperties() {
		try {

			log.info("Initializing solrwaybackweb-properties");

			String user_home=System.getProperty("user.home");
			log.info("Load properties: Using user.home folder:" + user_home);
			InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(user_home,PROPERTY_FILE)), "ISO-8859-1");

			serviceProperties = new Properties();
			serviceProperties.load(isr);
			isr.close();

			WAYBACK_SERVER =serviceProperties.getProperty(WAYBACK_SERVER_PROPERTY);

			log.info("Property:"+ WAYBACK_SERVER_PROPERTY +" = " + WAYBACK_SERVER);
		}
		catch (Exception e) {
			e.printStackTrace();
			log.error("Could not load property file:"+ PROPERTY_FILE);
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
