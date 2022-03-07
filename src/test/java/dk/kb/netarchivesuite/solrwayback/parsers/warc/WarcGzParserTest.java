package dk.kb.netarchivesuite.solrwayback.parsers.warc;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.image.ImageUtils;
import dk.kb.netarchivesuite.solrwayback.parsers.ArcFileParserFactory;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;


public class WarcGzParserTest  extends UnitTestUtils{
       
    @Test
    public void testWarcGzParser() throws Exception {
        
        File file = getFile("src/test/resources/example_warc/IAH-20080430204825-00000-blackbook.warc.gz");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 48777); //Image entry. offsets can be seen in the cdx file
        assertEquals("image/jpeg", arcEntry.getContentType());
        assertEquals("hewlett.jpg", arcEntry.getFileName());
        assertEquals(7812, arcEntry.getWarcEntryContentLength());
        assertEquals(7510, arcEntry.getContentLength());
        assertEquals(200,arcEntry.getStatus_code());
        
        BufferedImage image = ImageUtils.getImageFromBinary(arcEntry.getBinary());
        assertEquals(300,image.getWidth());
        assertEquals(116,image.getHeight());        
        assertEquals("http://www.archive.org/images/hewlett.jpg",arcEntry.getUrl());
        System.out.println(arcEntry.getCrawlDate());
        System.out.println(arcEntry.getWaybackDate());

    
    }

    
    @Test
    public void testLazyLoadBinary() throws Exception {
        
        File file = getFile("src/test/resources/example_warc/IAH-20080430204825-00000-blackbook.warc.gz");        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 48777, false); //Image entry
        
        assertNull(arcEntry.getBinary());
        arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 48777); //Image entry and load binary
        byte[] orgBinary = arcEntry.getBinary();        
        try (BufferedInputStream buf = arcEntry.getBinaryLazyLoad()) {

            byte[] newBinary = new byte[(int) arcEntry.getBinaryArraySize()];
            assertEquals("The expected number of bytes should be read from the lazy stream",
                         newBinary.length, IOUtils.read(buf, newBinary));
            assertEquals(orgBinary.length, newBinary.length); //Same length
            assertArrayEquals(orgBinary, newBinary); //Same binary
            assertEquals("There should be no more content in the lazy loaded stream", -1, buf.read());
        }
    }

    
    /* The warc file used for these tests below can not be shared.
   
     @Test
      public void testWarcEncoding() throws Exception {
      
      //content-type is lower case in warc
      File file = getFile("/media/teg/1200GB_SSD/netarkiv/pligt/5065-215-20131114083855-00000-kb-test-har-003.kb.dk.warc.gz");
      
      ArcEntry arcEntry = ArcFileParserFactory.getArcEntry(file.getCanonicalPath(), 353703887); //Image entry      
      assertEquals("text/html", arcEntry.getContentType());
      assertEquals(195850, arcEntry.getContentLength());                                          
    }
        
        
    
                
    @Test
    public void testWarcParserJSZipped() throws Exception {
    
      
        File file = getFile("src/test/resources/273422-246-20170326210303322-00000-sb-prod-har-004.statsbiblioteket.dk.warc.gz");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 71228603); //Image entry

        System.out.println(arcEntry.getContentLength());
        System.out.println(new String(arcEntry.getBinary()));
        
        assertEquals("text/css", arcEntry.getContentType());
        assertEquals("style.css", arcEntry.getFileName());
        assertEquals(10952, arcEntry.getWarcEntryContentLength());
        assertEquals(10443, arcEntry.getContentLength());                                          
    }
    
    @Test
    public void testWarcParserHtmlZipped() throws Exception {
        
        File file = getFile("src/test/resources/273422-246-20170326210303322-00000-sb-prod-har-004.statsbiblioteket.dk.warc.gz");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(),     57516462); //HTML

        System.out.println(arcEntry.getContentLength());
        System.out.println(new String(arcEntry.getBinary()));
        
        assertEquals("text/css", arcEntry.getContentType());
        assertEquals("style.css", arcEntry.getFileName());
        assertEquals(10952, arcEntry.getWarcEntryContentLength());
        assertEquals(10443, arcEntry.getContentLength());                          
    }
    
    @Test
    public void testWarcParserImageZipped() throws Exception {
        
        File file = getFile("src/test/resources/273422-246-20170326210303322-00000-sb-prod-har-004.statsbiblioteket.dk.warc.gz");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 57271819); //Image entry

        System.out.println(arcEntry.getContentLength());
        System.out.println(new String(arcEntry.getBinary()));
        
        assertEquals("text/css", arcEntry.getContentType());
        assertEquals("style.css", arcEntry.getFileName());
        assertEquals(10952, arcEntry.getWarcEntryContentLength());
        assertEquals(10443, arcEntry.getContentLength());                          
    }
 */
}
