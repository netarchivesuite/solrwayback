package dk.kb.netarchivesuite.solrwayback;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.image.ImageUtils;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;


public class WarcParserTest {
       
    @Test
    public void testWarcParser() throws Exception {
        
        File file = getFile("src/test/resources/example_warc/IAH-20080430204825-00000-blackbook.warc");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 181688); //Image entry
        assertEquals("image/jpeg", arcEntry.getContentType());
        assertEquals("hewlett.jpg", arcEntry.getFileName());
        assertEquals(7812, arcEntry.getWarcEntryContentLength());
        assertEquals(7510, arcEntry.getContentLength());
                        
        BufferedImage image = ImageUtils.getImageFromBinary(arcEntry.getBinary());
        assertEquals(300,image.getWidth());
        assertEquals(116,image.getHeight());        
        System.out.println(arcEntry.getUrl());
    }


    /**
     * Multi protocol resource loader. Primary attempt is direct file, secondary is classpath resolved to File.
     *
     * @param resource a generic resource.
     * @return a File pointing to the resource.
     */
    private static File getFile(String resource) throws IOException {
        File directFile = new File(resource);
        if (directFile.exists()) {
            return directFile;
        }
        URL classLoader = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (classLoader == null) {
            throw new FileNotFoundException("Unable to locate '" + resource + "' as direct File or on classpath");
        }
        String fromURL = classLoader.getFile();
        if (fromURL == null || fromURL.isEmpty()) {
            throw new FileNotFoundException("Unable to convert URL '" + fromURL + "' to File");
        }
        return new File(fromURL);
    }
}
