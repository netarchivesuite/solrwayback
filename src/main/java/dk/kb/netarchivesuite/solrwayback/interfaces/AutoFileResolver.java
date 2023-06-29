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
 </pre>
 *
 */
// TODO: Optional rescan interval
// TODO: Optional "scan on unknown file" trigger
// TODO: Adjustable pattern for matching WARCs to avoid garbage in the map
// TODO: Unit test (use the WARCs in test/resources)
@SuppressWarnings("unused")
public class AutoFileResolver implements ArcFileLocationResolverInterface {
    private static final Logger log = LoggerFactory.getLogger(AutoFileResolver.class);

    public static final String ROOTS_KEY = "autoresolver.roots"; // Prefixed warc.file.resolver.parameters

    public enum STATE { initializing, scanning, dormant }

    /**
     * Map from filename to path: {@code /a/b/c/test.warc} becomes {@code test.warc} -> {@code /a/b/c}.
     */
    private Map<String, String> WARCS = new HashMap<>();
    private final List<Path> roots = new ArrayList<>();
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
        log.info("Assigned root folders " + roots);
    }

    @Override
    public void initialize() {
        log.info("Starting initial scan. This might take a while");
        scanFull(); // TODO: Should probably be in a background thread
    }

    /**
     * Perform a full scan for WARCs from all roots.
     * The collected mappings only takes effect when the scan has fully completed.
     */
    private synchronized void scanFull() {
        state = STATE.scanning;
        final long startTime = System.currentTimeMillis();
        log.info("Starting scan for (W)ARC from roots {}. This might take a while", roots);

        Map<String, String> newWARCs = new HashMap<>();
        for (Path root : roots) {
            scanRoot(root, newWARCs);
        }
        WARCS = newWARCs;
        state = STATE.dormant;

        log.info("Finished scan for WARCs from {} roots in {} seconds. Number of registered files: {}",
                 roots.size(), (System.currentTimeMillis() - startTime) / 1000, WARCS.size());
    }

    private void scanRoot(Path path, Map<String, String> warcs) {
        final String location = path.toString(); // TODO: Check that it does not end in '/'
        try (DirectoryStream<Path> pathEntries = Files.newDirectoryStream(path)) {
            pathEntries.forEach(pathEntry -> {
                if (Files.isDirectory(pathEntry)) {
                    scanRoot(pathEntry, warcs);
                } else {
                    String filename = pathEntry.getFileName().toString();
                    if (warcs.containsKey(filename)) {
                        log.warn("The WARC name '{}' in folder '{}' is already present in folder '{}'",
                                 filename, location, warcs.get(filename));
                    }
                    warcs.put(filename, location);
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
}
