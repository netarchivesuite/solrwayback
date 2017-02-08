package dk.kb.netarchivesuite.solrwayback.listeners;

import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Log4JContextListener implements ServletContextListener {
    private static final String BUILD_PROPERTY_FILE = "/build.properties";
    private static final long MILLISECONDS_BETWEEN_SCANS = 60 * 1000;

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        InputStream is = getClass().getResourceAsStream(BUILD_PROPERTY_FILE);
        Properties properties = new Properties();

        try {
            properties.load(is);
            is.close();

            String applicationName = properties.getProperty("APPLICATION.NAME");

            ServletContext servletContext = contextEvent.getServletContext();
            String log4jFile = servletContext.getInitParameter(applicationName + ".log4jFileName");

            if (log4jFile != null) {
                if (log4jFile.matches(".*\\.xml")) {
                    DOMConfigurator.configureAndWatch(log4jFile, MILLISECONDS_BETWEEN_SCANS);
                } else if (log4jFile.matches(".*\\.properties")) {
                    PropertyConfigurator.configureAndWatch(log4jFile, MILLISECONDS_BETWEEN_SCANS);
                } else {
                    LogManager.getLogger(Log4JContextListener.class).warn("Unrecognized log4j configuration extension '" + log4jFile + "', using default log4j configuration");
                }
            } else {
                LogManager.getLogger(Log4JContextListener.class).warn("Context parameter '" + applicationName + ".log4jFileName' not found, using default log4j configuration");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogManager.getLogger(Log4JContextListener.class).error("Could not configure log4j", e);

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException dummy) {
                    // nothing to do here
                }
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        // Cleanup code goes here
    }
}
