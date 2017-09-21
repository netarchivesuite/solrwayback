package dk.kb.netarchivesuite.solrwayback.listeners;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;

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
            props.load(InitializationContextListener.class.getResourceAsStream("/build.properties"));

            version = props.getProperty("APPLICATION.VERSION");
            PropertiesLoader.initProperties(); //backend
            PropertiesLoaderWeb.initProperties(); //frontend
            
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
