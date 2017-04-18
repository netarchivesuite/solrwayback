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


public class ArcGzParserTest extends UnitTestUtils {
       
    @Test
    public void testArcGzParserHtml() throws Exception {
        
        File file = getFile("src/test/resources/example_arc/IAH-20080430204825-00000-blackbook.arc.gz");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 1306 ); //HTML entry
        assertEquals("text/html", arcEntry.getContentType());
        assertEquals("www.archive.org", arcEntry.getFileName());
        assertEquals(366, arcEntry.getContentLength()); //From header        
        assertEquals(366,arcEntry.getBinary().length); //Actually loaded in binary
    
       //System.out.println(new String(arcEntry.getBinary())); //from <html> to </html>
    }
     
    
    @Test
    public void testArcGzParserImage() throws Exception {
        
        File file = getFile("src/test/resources/example_arc/IAH-20080430204825-00000-blackbook.arc.gz");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 7733 ); //Image entry (or   9699) 
        assertEquals("image/jpeg", arcEntry.getContentType());
        assertEquals("logoc.jpg", arcEntry.getFileName());
        assertEquals(1662, arcEntry.getContentLength()); //From header        
        assertEquals(1662,arcEntry.getBinary().length); //Actually loaded in binary
    }
    
       
}
