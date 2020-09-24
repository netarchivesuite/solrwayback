package dk.kb.netarchivesuite.solrwayback.util;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    
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

        try (InputStream in = url.openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream(1024);) {
            byte[] buffer = new byte[1024];
            int len = in.read(buffer);
            while (len != -1) {
                out.write(buffer, 0, len);
                len = in.read(buffer);
            }
            return out.toString("utf-8");
        }
    }
    
}
