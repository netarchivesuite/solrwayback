package dk.kb.netarchivesuite.solrwayback.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.StringUtils;

public class PropertiesLoader {

    private static final Logger log = LoggerFactory.getLogger(PropertiesLoader.class);    
    
    private static final String DEFAULT_PROPERTY_FILE = "solrwayback.properties";
    private static final String SOLR_SERVER_PROPERTY="solr.server";
    private static final String PROXY_PORT_PROPERTY="proxy.port";
    private static final String PROXY_ALLOW_HOSTS_PROPERTY="proxy.allow.hosts";
    private static final String WARC_FILE_RESOLVER_CLASS_PROPERTY="warc.file.resolver.class";
    private static final String WAYBACK_BASEURL_PROPERTY="wayback.baseurl";
    private static final String CHROME_COMMAND_PROPERTY="chrome.command";
    private static final String SCREENSHOT_TEMP_IMAGEDIR_PROPERTY="screenshot.temp.imagedir";
    private static final String PID_COLLECTION_NAME_PROPERTY="pid.collection.name";
    private static final String SCREENSHOT_PREVIEW_TIMEOUT_PROPERTY="screenshot.preview.timeout";               
    private static Properties serviceProperties = null;

    public static String SOLR_SERVER = null;
    public static String WAYBACK_BASEURL = null;
    public static String WAYBACK_HOST = null; //Taken from WAYBACK_BASEURL
    public static int WAYBACK_SERVER_PORT = 0; //Taken from WAYBACK_BASEURL
    public static String CHROME_COMMAND= null;
    public static String SCREENSHOT_TEMP_IMAGEDIR = null;
    public static String PROXY_PORT= null;
    public static String PROXY_ALLOW_HOSTS= null;
    public static String WARC_FILE_RESOLVER_CLASS = null;
    public static String PID_COLLECTION_NAME = null;
    public static int SCREENSHOT_PREVIEW_TIMEOUT = 10;//default
    

    public static void initProperties() {
      initProperties(DEFAULT_PROPERTY_FILE);      
    }
    
    public static void initProperties(String propertyFile) {
        try {

            log.info("Initializing solrwayback-properties");
            String user_home=System.getProperty("user.home");
                        
            File f = new File(user_home,propertyFile);
             if (!f.exists()) {
               log.info("Could not find contextroot specific propertyfile:"+propertyFile +". Using default:"+DEFAULT_PROPERTY_FILE);
               propertyFile=DEFAULT_PROPERTY_FILE;                                 
             }                        
            log.info("Load backend-properties: Using user.home folder:" + user_home +" and propertyFile:"+propertyFile);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(user_home,propertyFile)), "ISO-8859-1");

            serviceProperties = new Properties();
            serviceProperties.load(isr);
            isr.close();

            SOLR_SERVER =serviceProperties.getProperty(SOLR_SERVER_PROPERTY);
            WAYBACK_BASEURL = serviceProperties.getProperty(WAYBACK_BASEURL_PROPERTY);
            CHROME_COMMAND = serviceProperties.getProperty(CHROME_COMMAND_PROPERTY);
            SCREENSHOT_TEMP_IMAGEDIR = serviceProperties.getProperty(SCREENSHOT_TEMP_IMAGEDIR_PROPERTY);
            PROXY_PORT = serviceProperties.getProperty(PROXY_PORT_PROPERTY);
            PROXY_ALLOW_HOSTS = serviceProperties.getProperty(PROXY_ALLOW_HOSTS_PROPERTY);
            WARC_FILE_RESOLVER_CLASS = serviceProperties.getProperty(WARC_FILE_RESOLVER_CLASS_PROPERTY);
            PID_COLLECTION_NAME = serviceProperties.getProperty(PID_COLLECTION_NAME_PROPERTY);
            String timeout  = serviceProperties.getProperty(SCREENSHOT_PREVIEW_TIMEOUT_PROPERTY);
            
            URL waybacksURL = new URL (WAYBACK_BASEURL);
            WAYBACK_SERVER_PORT =  waybacksURL.getPort();
            WAYBACK_HOST =  waybacksURL.getHost();    
            
            if (timeout != null){
              SCREENSHOT_PREVIEW_TIMEOUT = Integer.parseInt(timeout);
            }
                        
            log.info("Property:"+ SOLR_SERVER_PROPERTY +" = " + SOLR_SERVER);
            log.info("Property:"+ WAYBACK_BASEURL_PROPERTY +" = " + WAYBACK_BASEURL);           
            log.info("Property:"+ PROXY_PORT_PROPERTY +" = " + PROXY_PORT);
            log.info("Property:"+ PROXY_ALLOW_HOSTS_PROPERTY +" = " + PROXY_ALLOW_HOSTS);
            log.info("Property:"+ CHROME_COMMAND_PROPERTY +" = " + CHROME_COMMAND);
            log.info("Property:"+ SCREENSHOT_TEMP_IMAGEDIR_PROPERTY +" = " + SCREENSHOT_TEMP_IMAGEDIR);
            log.info("Property:"+ SCREENSHOT_PREVIEW_TIMEOUT_PROPERTY +" = " +  SCREENSHOT_PREVIEW_TIMEOUT);
            log.info("Property:"+ WARC_FILE_RESOLVER_CLASS_PROPERTY +" = " + WARC_FILE_RESOLVER_CLASS);            
            log.info("Property:"+ PID_COLLECTION_NAME_PROPERTY +" = " +  PID_COLLECTION_NAME);
            
        }
        catch (Exception e) {
            e.printStackTrace();
            log.error("Could not load property file:"+propertyFile);
        }
    }
    
}
