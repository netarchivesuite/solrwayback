package dk.kb.netarchivesuite.solrwayback.listeners;

import java.lang.reflect.Constructor;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.interfaces.ArcFileLocationResolverInterface;
import dk.kb.netarchivesuite.solrwayback.parsers.ArcParserFileResolver;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class InitializationContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(InitializationContextListener.class);
    private static String version;
    
    // this is called by the web-container before opening up for requests.(defined in web.xml)
    public void contextInitialized(ServletContextEvent event) {

        log.info("solrwayback starting up...");
        Properties props = new Properties();
        try {
          
            String webAppContext = event.getServletContext().getContextPath();                    
            props.load(InitializationContextListener.class.getResourceAsStream("/build.properties"));
            version = props.getProperty("APPLICATION.VERSION");
            PropertiesLoaderWeb.SOLRWAYBACK_VERSION = version;

            // Remove leading "/"
            webAppContext = webAppContext.startsWith("/") && !webAppContext.substring(1).contains("/") ?
                    webAppContext.substring(1) :
                    webAppContext;
            // Resolve property locations
            // Properties are either explicitly set using the web app Environment or taken from user home
            String backendConfig = webAppContext + ".properties"; // If contextroot is not solrwayback, it will first look for that context specific propertyfile
            String frontendConfig = webAppContext + "web.properties";
            try {
                InitialContext ctx = new InitialContext();

                try {
                    backendConfig = (String) ctx.lookup("java:/comp/env/solrwayback-config");
                } catch (NamingException e) {
                    log.info("Exception attempting to resolve configuration locations using web app environment " +
                             "'solrwayback-config'. This is most likely because the WAR was deployed without a " +
                             "context. This is not a problem: Using default config location '" + backendConfig + "'" +
                             ". Exception message was '" + e.getMessage() + "'");
                }

                try {
                    frontendConfig = (String) ctx.lookup("java:/comp/env/solrwaybackweb-config");
                } catch (NamingException e) {
                    log.info("Exception attempting to resolve configuration locations using web app environment " +
                             "'solrwaybackweb-config'. This is most likely because the WAR was deployed without a " +
                             "context. This is not a problem: Using default config location '" + frontendConfig + "'" +
                             ". Exception message was '" + e.getMessage() + "'");
                }
            } catch (NamingException e) {
                log.warn("Unable to create new InitialContext used for property location resolving", e);
            }
            
            PropertiesLoader.initProperties(backendConfig); //backend.
            PropertiesLoaderWeb.initProperties(frontendConfig); //frontend

            // initialise the solrclient
            NetarchiveSolrClient.initialize(PropertiesLoader.SOLR_SERVER);
            
            //Load the warcfilelocation resolver, set optional parameter and initialize                       
            String arcFileResolverClass = PropertiesLoader.WARC_FILE_RESOLVER_CLASS;
            if (arcFileResolverClass != null){            
              Class c = Class.forName(arcFileResolverClass);                               
              Constructor constructor = c.getConstructor(); //Default constructor, no arguments
              ArcFileLocationResolverInterface resolverImpl= (ArcFileLocationResolverInterface) constructor.newInstance();          
              resolverImpl.setParameters(PropertiesLoader.WARC_FILE_RESOLVER_PARAMETERS); //Interfaces can have custom parameter
              resolverImpl.initialize();        
              ArcParserFileResolver.setArcFileLocationResolver(resolverImpl); //Set this on the Facade
              log.info("Using warc-file-resolver implementation class:"+arcFileResolverClass);
            }
            else{
              log.info("Using default warc-file-resolver implementation");
            }
                                               
            log.info("solrwayback version " + version + " started successfully");

        } catch (Exception e) {
            log.error("failed to initialize service", e);
            e.printStackTrace();
            throw new RuntimeException("failed to initialize service", e);
        }
    }

    // this is called by the web-container at shutdown. (defined in web.xml)
    public void contextDestroyed(ServletContextEvent sce) {
        try {        
          log.info("solrwayback shutting down...");
        } catch (Exception e) {
            log.error("failed to shutdown solrwayback", e);
        }

    }

}
