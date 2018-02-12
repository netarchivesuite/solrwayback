package dk.kb.netarchivesuite.solrwayback.listeners;

import java.lang.reflect.Constructor;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.interfaces.ArcFileLocationResolverInterface;
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
                        
            //Load the warcfilelocation resolver.                        
            String arcFileResolverClass = PropertiesLoader.WARC_FILE_RESOLVER_CLASS;
            if (arcFileResolverClass != null){            
            Class c = Class.forName(arcFileResolverClass);                               
            Constructor constructor = c.getConstructor(); //Default constructor, no arguments
            ArcFileLocationResolverInterface resolverImpl= (ArcFileLocationResolverInterface) constructor.newInstance();          
            Facade.setArcFileLocationResolver(resolverImpl); //Set this on the Facade
            log.info("Using warc-file-resolver implementation class:"+arcFileResolverClass);
            }
            else{
              log.info("Using default warc-file-resolver implementation");
            }
            
            
            //TODO Delete code later. this is just a backup implementation 
            /* This works with socks 5 
            new Thread(new Runnable() {
              public void run() {
                SOCKS.main(new String[]{"/home/teg/workspace/solrwayback/socks.properties"});
              }
             }).start();
            */
            
            //Starting up the socks proxy.            
            String proxy_port= PropertiesLoader.PROXY_PORT;
            String proxy_allow_hosts = PropertiesLoader.PROXY_ALLOW_HOSTS;
            
            if (proxy_port != null &&  proxy_allow_hosts != null){                        
              int port = Integer.parseInt(proxy_port);  
              String[] hosts= proxy_allow_hosts.replace(" ", "").split(","); //remove all white spaces and split by ,                             
              socksProxy = new SOCKSProxy(port,  hosts);              
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
