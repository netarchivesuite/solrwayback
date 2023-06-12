package dk.kb.netarchivesuite.solrwayback.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url == null) {
            Path path = Paths.get(resource);
            if (!Files.exists(path)) {
                throw new FileNotFoundException("Unable to locate '" + resource + "'");
            }
            url = path.toUri().toURL();
        }
        return IOUtils.toString(url, StandardCharsets.UTF_8);
    }

    /**
     * 
     * @param resource a class path entry or a file path.
     * @return File if found
     * @throws Exception if the resource could not be fetch
     */
    public static File fetchFile(String resource) throws Exception {

        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url == null) {
            Path path = Paths.get(resource);
            if (!Files.exists(path)) {
                throw new FileNotFoundException("Unable to locate '" + resource + "'");
            }                          
            return new File(path.toUri());
        }
        else {
            return new File(url.toURI());
        }

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
