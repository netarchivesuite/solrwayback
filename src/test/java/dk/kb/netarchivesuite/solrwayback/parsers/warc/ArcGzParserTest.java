package dk.kb.netarchivesuite.solrwayback.parsers.warc;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.File;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;


public class ArcGzParserTest extends UnitTestUtils {
       
    @Test
    public void testArcGzParserHtml() throws Exception {
        
        File file = getFile("src/test/resources/example_arc/IAH-20080430204825-00000-blackbook.arc.gz");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 1306,true); //HTML entry
        assertEquals("text/html", arcEntry.getContentType());
        assertEquals("www.archive.org", arcEntry.getFileName());
        assertEquals(366, arcEntry.getContentLength()); //From header        
        assertEquals(366,arcEntry.getBinary().length); //Actually loaded in binary
        assertEquals(200,arcEntry.getStatus_code());
       //System.out.println(new String(arcEntry.getBinary())); //from <html> to </html>
    }
     
    
    @Test
    public void testArcGzParserImage() throws Exception {
        
        File file = getFile("src/test/resources/example_arc/IAH-20080430204825-00000-blackbook.arc.gz");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 7733 ,true); //Image entry (or   9699) 
        assertEquals("image/jpeg", arcEntry.getContentType());
        assertEquals("logoc.jpg", arcEntry.getFileName());
        assertEquals(1662, arcEntry.getContentLength()); //From header        
        assertEquals(1662,arcEntry.getBinary().length); //Actually loaded in binary
    }
    
 
    @Test
    public void testLazyLoadBinary() throws Exception {
        
        File file = getFile("src/test/resources/example_arc/IAH-20080430204825-00000-blackbook.arc.gz");        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(),7733, false); //Image entry
        
        assertNull(arcEntry.getBinary());
        arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 7733,true); 
        byte[] orgBinary = arcEntry.getBinary();        
        try (BufferedInputStream buf = arcEntry.getBinaryRaw()) {
            byte[] newBinary = new byte[(int) arcEntry.getBinaryArraySize()];
            assertEquals("The expected number of bytes should be read from the lazy stream",
                         newBinary.length, IOUtils.read(buf, newBinary));
            assertEquals(orgBinary.length, newBinary.length); //Same length
            assertArrayEquals(orgBinary, newBinary); //Same binary
            assertEquals("There should be no more content in the lazy loaded stream", -1, buf.read());
        }
    }

    
    
}
