package dk.kb.netarchivesuite.solrwayback.parsers.warc;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;

import org.junit.Test;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.image.ImageUtils;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;


public class WarcGzParserTest  extends UnitTestUtils{
       
    
    @Test
    public void testEvilWarcParser() throws Exception {                      
        // The binary is not loaded, so content offsets in the WARC-File does not need match
        // so the WARC file can be edited with further evil header entries.
        File file = getFile("src/test/resources/example_warc/Evil-Warc-Headers.warc");
        try {        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 0);  //first entry, no WARC metadata header
        
        assertEquals("text/plain", arcEntry.getContentType());
        assertEquals("robots.txt", arcEntry.getFileName());
        assertEquals(6000, arcEntry.getWarcEntryContentLength());       
        assertEquals(200,arcEntry.getStatus_code());
        assertEquals(5155,arcEntry.getContentLength()); //This entry has a tab/multiple whitespace before size
        }
        catch(Exception e) {
            e.printStackTrace();
            fail();            
        }                
    }
    
    @Test
    public void testWarcGzParser() throws Exception {
        
        File file = getFile("src/test/resources/example_warc/IAH-20080430204825-00000-blackbook.warc.gz");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 48777); //Image entry. offsets can be seen in the cdx file
        assertEquals("image/jpeg", arcEntry.getContentType());
        assertEquals("hewlett.jpg", arcEntry.getFileName());
        assertEquals(7812, arcEntry.getWarcEntryContentLength());
        assertEquals(7510, arcEntry.getContentLength());
        assertEquals(200,arcEntry.getStatus_code());
        
        BufferedImage image = ImageUtils.getImageFromBinary(arcEntry.getBinaryDecoded());
        assertEquals(300,image.getWidth());
        assertEquals(116,image.getHeight());        
        assertEquals("http://www.archive.org/images/hewlett.jpg",arcEntry.getUrl());
        System.out.println(arcEntry.getCrawlDate());
        System.out.println(arcEntry.getWaybackDate());

    
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
