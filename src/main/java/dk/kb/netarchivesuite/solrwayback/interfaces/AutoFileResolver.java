package dk.kb.netarchivesuite.solrwayback.interfaces;

import dk.kb.netarchivesuite.solrwayback.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Optional FileLocationResolver interface implementation.
 *
 * This class scans one or more {@link #roots} folders for (W)ARC files and maintains a map.
 * from files to locations. This requires WARC filenames to be unique.
 *
 * To activate it, set this in solrwayback.properties:
 * <pre>
 warc.file.resolver.class=AutoFileResolver
 warc.file.resolver.parameters.autoresolver.roots=/home/sw/warcs1,/netmounts/colfoo
 warc.file.resolver.parameters.autoresolver.pattern=.*[.]w?arc([.]gz)?
 warc.file.resolver.parameters.autoresolver.rescan.enabled=false
 warc.file.resolver.parameters.autoresolver.rescan.seconds=1200
 </pre>
 * Only the {@code roots} parameter is mandatory.
 * {@code pattern}, {@code rescan.enabled} and {@code rescan.seconds} had the
 * defaults shown above
 */
// TODO: Optional "scan on unknown file" trigger
// TODO: Unit test (use the WARCs in test/resources)
@SuppressWarnings("unused")
public class AutoFileResolver implements ArcFileLocationResolverInterface, Runnable {
    private static final Logger log = LoggerFactory.getLogger(AutoFileResolver.class);

    // Prefixed warc.file.resolver.parameters
    public static final String  ROOTS_KEY = "autoresolver.roots";
    public static final String  PATTERN_KEY = "autoresolver.pattern";
    public static final String  PATTERN_DEFAULT = ".*[.]w?arc([.]gz)?";
    public static final String  RESCAN_ENABLED_KEY = "autoresolver.rescan.enabled";
    public static final boolean RESCAN_ENABLED_DEFAULT = false;
    public static final String  RESCAN_SECONDS_KEY = "autoresolver.rescan.seconds";
    public static final long    RESCAN_SECONDS_DEFAULT = 60;

    public enum STATE { initializing, scanning, dormant }

    /**
     * Map from filename to path: {@code /a/b/c/test.warc} becomes {@code test.warc} -> {@code /a/b/c}.
     */
    private Map<String, String> WARCS = new HashMap<>();
    private final List<Path> roots = new ArrayList<>();
    private Pattern filePattern;
    private boolean rescanEnabled;
    private long rescanSeconds;
    private Thread scanThread;
    private STATE state = STATE.initializing;

    /**
     * Constructs an AutoFileResolver in its uni-initialized state.
     *
     * The caller should ensure that {@link #setParameters(Map)} and {@link #initialize()} is subsequently called.
     */
    @SuppressWarnings("unused")
    public AutoFileResolver() { }

    /**
     *
     * @param parameters a parameter map as resolved by {@code PropertiesLoader#loadArcResolverParameters(Properties)}.
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

        filePattern = parameters.containsKey(PATTERN_KEY) ?
                Pattern.compile(parameters.get(PATTERN_KEY)):
                Pattern.compile(PATTERN_DEFAULT);
        rescanEnabled = parameters.containsKey(RESCAN_ENABLED_KEY) ?
                Boolean.parseBoolean(parameters.get(RESCAN_ENABLED_KEY)) :
                RESCAN_ENABLED_DEFAULT;
        rescanSeconds = parameters.containsKey(RESCAN_SECONDS_KEY) ?
                Long.parseLong(parameters.get(RESCAN_SECONDS_KEY)) :
                RESCAN_SECONDS_DEFAULT;

        log.info("Assigned parameters for {}", this);
    }

    @Override
    public void initialize() {
        log.info("Creating and activating scan thread");
        scanThread = new Thread(this, "AutoFileResolverThread");
        scanThread.setDaemon(true); // Shut down the Thread automatically on war redeploy
        scanThread.start();
    }

    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        long scanCount = 0;
        do { // 1 scan is guaranteed, even if rescanEnabled is false
            state = scanCount++ == 0 ? STATE.initializing : STATE.scanning;
            scanFull();
            state = STATE.dormant;

            if (rescanEnabled) {
                try {
                    Thread.sleep(rescanSeconds*1000);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while sleeping. This should not happen but is not fatal");
                }
            }
        } while (rescanEnabled);
    }

    /**
     * Perform a full scan for WARCs from all roots.
     * The collected mappings only takes effect when the scan has fully completed.
     */
    private synchronized void scanFull() {
        final long startTime = System.currentTimeMillis();
        log.info("Starting scan for (W)ARC from roots {}. This might take a while", roots);

        Map<String, String> newWARCs = new HashMap<>();
        for (Path root : roots) {
            scanRoot(root, newWARCs);
        }
        WARCS = newWARCs;

        log.info("Finished scan for WARCs from {} roots in {} seconds. Number of registered files: {}",
                 roots.size(), (System.currentTimeMillis() - startTime) / 1000, WARCS.size());
    }

    /**
     * Scan recursively from the given {@code path} and add WARC files to the {@code warcs} map.
     * @param path  folder to scan from.
     * @param warcs map for collecting WARC to folder mappings.
     */
    private void scanRoot(Path path, Map<String, String> warcs) {
        final String location = path.toString(); // TODO: Check that it does not end in '/'
        try (DirectoryStream<Path> pathEntries = Files.newDirectoryStream(path)) {
            pathEntries.forEach(pathEntry -> {
                if (Files.isDirectory(pathEntry)) {
                    scanRoot(pathEntry, warcs);
                } else {
                    String filename = pathEntry.getFileName().toString();
                    if (filePattern.matcher(filename).matches()) {
                        if (warcs.containsKey(filename)) {
                            log.warn("The WARC name '{}' in folder '{}' is already present in folder '{}'",
                                     filename, location, warcs.get(filename));
                        }
                        warcs.put(filename, location);
                    } else {
                        log.debug("Scanner encountered non-matching file '{}'", filename);
                    }
                }
            });
        } catch (AccessDeniedException e) {
            log.debug("AccessDeniedException for path '{}'", path);
        } catch (IOException e) {
            log.warn("Exception while scanning the content of folder '" + path + "'", e);
        }
    }

    @Override
    public ArcSource resolveArcFileLocation(String source_file_path){
        while (state == STATE.initializing) {
            try {
                // Busy wait is not the clean way, but this is only relevant during startup
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log.debug("Interrupted while waiting for state change. This is not problematic");
            }
        }

        String fileName = new File(source_file_path).getName();
        String value = WARCS.get(fileName);

        String finalPath = value == null ? source_file_path : value + "/" + fileName;

        return ArcSource.fromFile(finalPath);
    }

    /**
     * @return the state of the resolver: Initializing, scanning or dormant.
     */
    public STATE getState() {
        return state;
    }

    @Override
    public String toString() {
        return "AutoFileResolver(" +
               "roots='" + roots + "'" +
               ", filePattern=" + filePattern +  "'" +
               ", rescanEnabled=" + rescanEnabled +
               ", rescanSeconds=" + rescanSeconds +
               ", state=" + state +
               ", #WARCS=" + WARCS.size() +
               '}';
    }
}
