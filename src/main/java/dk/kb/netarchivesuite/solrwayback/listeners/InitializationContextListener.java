package dk.kb.netarchivesuite.solrwayback.listeners;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.proxy.SOCKSProxy;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class InitializationContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(InitializationContextListener.class);
    private static String version;
    private Thread proxyThread = null;
    private SOCKSProxy socksProxy  = null;
    
    // this is called by the web-container before opening up for requests.(defined in web.xml)
    public void contextInitialized(ServletContextEvent event) {

        log.info("solrwayback starting up...");
        Properties props = new Properties();
        try {
            props.load(InitializationContextListener.class.getResourceAsStream("/build.properties"));

            version = props.getProperty("APPLICATION.VERSION");
            PropertiesLoader.initProperties(); //backend
            PropertiesLoaderWeb.initProperties(); //frontend

            //Starting up the socks proxy.
            
            String proxy_port= PropertiesLoader.PROXY_PORT;
            String proxy_allow_host = PropertiesLoader.PROXY_ALLOW_HOST;
            
            if (proxy_port != null &&  proxy_allow_host != null){                        
              int port = Integer.parseInt(proxy_port);  
              socksProxy = new SOCKSProxy(port, proxy_allow_host);              
              proxyThread = new Thread(socksProxy);                                    
              proxyThread.setDaemon(true); //exit when tomcat stops
              proxyThread.start();                       
            }
            else{
              log.info("no proxy server configured in property file.");
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
          if ( socksProxy != null){
            socksProxy.stopProxy();
          }
          log.info("solrwayback shutting down...");
        } catch (Exception e) {
            log.error("failed to shutdown solrwayback", e);
        }

    }

}
