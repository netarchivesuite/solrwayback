package dk.kb.netarchivesuite.solrwayback.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileUtil {

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);


    /**
     * Retrieve the given resource as an UTF-8 String.
     * @param resource a class path entry or a file path.
     * @return the content of the resource as a String.
     * @throws IOException if the resource could not be fetch or UTF-8 parsed.
     */
    public static String fetchUTF8(String resource) throws IOException {
        return IOUtils.toString(resolve(resource).toUri().toURL(), StandardCharsets.UTF_8);
    }

    /**
     * 
     * @param resource a class path entry or a file path.
     * @return File if found
     * @throws Exception if the resource could not be fetch
     */
    public static File fetchFile(String resource) throws Exception {
        return new File(resolve(resource).toUri());
    }

    /**
     * Locates a file designated by {@code resource} by using the class loader primarily and direct checking of
     * the file system secondarily.
     * @param resource a resource available on the file system.
     * @return the path to the {@code resource}.
     * @throws IOException if {@code resource} could not be located.
     */
    public static Path resolve(String resource) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url == null) {
            Path path = Paths.get(resource);
            if (!Files.exists(path)) {
                throw new FileNotFoundException("Unable to locate '" + resource + "'");
            }
            return path;
        }
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Unable to convert URL '" + url + "' to URI", e);
        }
    }

    /**
     * Specialized resolver that operates on the file system. Attempts to locate the given resources in order
     * <ul>
     *     <li>Directly as file</li>
     *     <li>As {@code env:catalina.base/resource}, {@code env:catalina.base/../resource} etc</li>
     *     <li>As {@code env:catalina.home/resource}, {@code env:catalina.home/../resource} etc</li>
     *     <li>As {@code env:user.dir/resource}, {@code env:user.dir/../resource} etc</li>
     *     <li>As {@code env:user.home/resource}</li>
     * </ul>
     * When running under Tomcat, {@code catalina.home} is guaranteed to be set and {@code catalina.home} might be set.
     * @param resources one or more resources to look for. First file system match is returned.
     * @return a Path to an existing file, found using the priorities stated above.
     * @throws FileNotFoundException if none of the resources could be found.
     */
    public static Path resolveContainerResource(String... resources) throws FileNotFoundException {
        Set<Path> candidates = Arrays.stream(resources).
                flatMap(FileUtil::getContainerCandidates).
                collect(Collectors.toCollection(LinkedHashSet::new));

        // We print all potential paths to show people reading logs where the resources can be
        log.info("Resolving resource in order {}", candidates);

        return candidates.stream().
                filter(Files::exists).
                findFirst().
                orElseThrow(() -> new FileNotFoundException("Unable to locate any of " + Arrays.toString(resources)));
    }

    /**
     * Generates candidate Paths for the given resource in order
     * <ul>
     *     <li>Directly as file</li>
     *     <li>As {@code env:catalina.base/resource}, {@code env:catalina.base/../resource} etc</li>
     *     <li>As {@code env:catalina.home/resource}, {@code env:catalina.home/../resource} etc</li>
     *     <li>As {@code env:user.dir/resource}, {@code env:user.dir/../resource} etc</li>
     *     <li>As {@code env:user.home/resource}</li>
     * </ul>
     * When running under Tomcat, {@code catalina.home} is guaranteed to be set and {@code catalina.home} might be set.
     * @param resource a resource to look for.
     * @return a stream with Paths to check for existence of the given resource, in order of priority as listed above.
     */
    static Stream<Path> getContainerCandidates(String resource) {
        return Stream.of("catalina.base", "catalina.home", "user.dir", "user.home").
                map(System::getProperty).            // Get the property value
                filter(Objects::nonNull).            // Ignore the non-defined properties
                map(Paths::get).                     // Convert to Path
                flatMap(FileUtil::allLevels).        // All folders
                map(path -> path.resolve(resource)); // All full paths
    }

    /**
     * Expand the given path to a Stream of the path itself, followed by all its parents.
     * @param path any path.
     * @return a stream of the path followed by all parents.
     */
    private static Stream<Path> allLevels(Path path) {
        List<Path> levels = new ArrayList<>();
        levels.add(path);
        while ((path = path.getParent()) != null) {
            levels.add(path);
        }
        return levels.stream();
    }

    /**
     * Converts sPath to a {@link Path} and checks that it is an accessible folder.
     * @param sPath a file system path.
     * @return a {@link Path} guaranteed to point to an accessible folder.
     */
    public static Path toExistingFolder(String sPath) {
        Path path = Paths.get(sPath);
        if (!Files.isReadable(path)) {
            throw new IllegalStateException("Unable to access folder '" + sPath + "'");
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException("The path '" + sPath + "' is not a folder");
        }
        return path;
    }
    
   /**
    * 
    * @param resource a class path entry or a file path.
    * @return true if file exist and not directory
    */
    public static boolean validateFileExist(String file) {
    
        if (file == null) {
          log.error("Can not validate file, file is null");
          return false; 
        }
        
        Path path = Paths.get(file);

        boolean exists = Files.exists(path);

        if (!exists) {
            log.error("File does not exist:"+file);
            return false;   
        }

        boolean directory = Files.isDirectory(path);        
        if (directory) {
            log.error("Expected file is a directory:"+file);
            return false; 
        }                
        return true;        
    }

}
