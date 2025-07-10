package dk.kb.netarchivesuite.solrwayback.parsers.warc;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;


public class ArcGzParserTest extends UnitTestUtils {
       
    @Test
    public void testArcGzParserHtml() throws Exception {
        
        File file = getFile("src/test/resources/example_arc/IAH-20080430204825-00000-blackbook.arc.gz");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 1306); //HTML entry
        assertEquals("text/html", arcEntry.getContentType());
        assertEquals("www.archive.org", arcEntry.getFileName());
        assertEquals(366, arcEntry.getContentLength()); //From header        
        assertEquals(366,arcEntry.getBinaryDecodedBytes().length); //Actually loaded in binary
        assertEquals(200,arcEntry.getStatus_code());
       //System.out.println(new String(arcEntry.getBinary())); //from <html> to </html>
    }
     
    
    @Test
    public void testArcGzParserImage() throws Exception {
        
        File file = getFile("src/test/resources/example_arc/IAH-20080430204825-00000-blackbook.arc.gz");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 7733); //Image entry (or   9699)
        assertEquals("image/jpeg", arcEntry.getContentType());
        assertEquals("logoc.jpg", arcEntry.getFileName());
        assertEquals(1662, arcEntry.getContentLength()); //From header        
        assertEquals(1662,arcEntry.getBinaryDecodedBytes().length); //Actually loaded in binary
    }
}
