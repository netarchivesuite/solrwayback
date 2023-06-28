package dk.kb.netarchivesuite.solrwayback.interfaces;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Optional FileLocationResolver interface implementation.
 *  
 * This class scans one or more {@link #roots} folders for (W)ARC files and maintains a map.
 * from files to locations. This requires WARC filenames to be unique.
 *
 * To activate it, set this in solrwayback.properties:
 *   warc.file.resolver.class=AutoFileResolver
 *   warc.file.resolver.parameters.autoresolver.roots=/home/sw/warcs1,/netmounts/colfoo
 *
 * TODO: Optional rescan interval
 * TODO: Optional "scan on unknown file" trigger
 */
public class AutoFileResolver implements ArcFileLocationResolverInterface, Runnable {
    private static final Logger log = LoggerFactory.getLogger(AutoFileResolver.class);

    public static final String ROOTS_KEY = "autoresolver.roots"; // Prefixed warc.file.resolver.parameters

    public enum STATE { initializing, initial_scan, scanning, dormant }

    /**
     * Map from filename to path: {@code /a/b/c/test.warc} becomes {@code test.warc} -> {@code /a/b/c}.
     */
    private HashMap<String,String> WARCS = new HashMap<String,String>();
    private final List<Path> roots = new ArrayList<>();
    private Thread scanThread = null;
    private STATE state = STATE.initializing;

    /**
     * Constructs an AutoFileResolver in its uni-initialized state.
     *
     * The caller should ensure that {@link #setParameters(Map)} and {@link #initialize()} is subsequently called.
     */
    public AutoFileResolver() { }

    /**
     *
     * @param parameters a parameter map as resolved by {@link PropertiesLoader#loadArcResolverParameters(Properties)}.
     */
    @Override
    public void setParameters(Map<String, String> parameters) {
        if (!parameters.containsKey(ROOTS_KEY)) {
            String message =
                    "The property warc.file.resolver.parameters." + ROOTS_KEY + " was not present in properties. " +
                    "This property is needed for AutoFileResolver to work. Please add the property with one or more " +
                    "paths to WARCs as key. Use comma to separate paths";
            log.error(message);
            throw new IllegalStateException(message);
        }
        Arrays.stream(parameters.get(ROOTS_KEY).split(" *, *")).
                map(String::trim).
                filter(path -> !path.isEmpty()).
                map(FileUtil::toExistingFolder).
                forEach(roots::add);
        if (roots.isEmpty()) {
            String message = "The property warc.file.resolver.parameters." + ROOTS_KEY + " contained no folders. " +
                             "At least 1 folder is needed AutoFileResolver to work.";
            log.error(message);
            throw new IllegalStateException(message);
        }
        log.info("Assigned root folders " + roots);
    }

    @Override
    public void initialize() {
        final long startTime = System.currentTimeMillis();
        log.info("Starting initial scan from roots {}. This might take a while", roots);
        startScan();
        log.info("Finished scan for WARCs in {} seconds. Number of registered files: {}",
                 (System.currentTimeMillis()-startTime)/1000, WARCS.size());
    }

    /**
     * Starts background scanning for (W)ARCs.
     */
    private void startScan() {
        // TODO: Implement this
    }


    /**
     *
     */
    private synchronized void scan() {
        final long startTime = System.currentTimeMillis();
        log.info("Starting scan for (W)ARC from roots {}. This might take a while", roots);
        // TODO Scan here
        log.info("Finished scan for WARCs in {} seconds. Number of registered files: {}",
                 (System.currentTimeMillis()-startTime)/1000, WARCS.size());
    }

    //Return the filelocation if filename is found in the mapping file.
      //If the filename is not found in the mapping, return the input back.
    
    @Override
    public ArcSource resolveArcFileLocation(String source_file_path){
        String fileName = new File(source_file_path).getName();
        String value = WARCS.get(fileName);

        String finalPath = value == null ? source_file_path : value + "/" + fileName;

        return ArcSource.fromFile(finalPath);
      }

    @Override
    public void run() {

    }
}
