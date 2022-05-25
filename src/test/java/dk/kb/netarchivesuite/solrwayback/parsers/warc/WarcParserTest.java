package dk.kb.netarchivesuite.solrwayback.parsers.warc;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.image.ImageUtils;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;


public class WarcParserTest extends UnitTestUtils{
       
    @Test
    public void testWarcParser() throws Exception {
        
        File file = getFile("src/test/resources/example_warc/IAH-20080430204825-00000-blackbook.warc");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 181688,true); //Image entry
        assertEquals("image/jpeg", arcEntry.getContentType());
        assertEquals("hewlett.jpg", arcEntry.getFileName());
        assertEquals(7812, arcEntry.getWarcEntryContentLength());
        assertEquals(7510, arcEntry.getContentLength());
        assertEquals(200,arcEntry.getStatus_code());
        
        BufferedImage image = ImageUtils.getImageFromBinary(arcEntry.getBinary());
        assertEquals(300,image.getWidth());
        assertEquals(116,image.getHeight());        
        assertEquals("http://www.archive.org/images/hewlett.jpg",arcEntry.getUrl());
    
        //System.out.println(arcEntry.getCrawlDate());
        //System.out.println(arcEntry.getWaybackDate());
    
    }
    
    @Test
    public void testRedirect() throws Exception {

        File file = getFile("src/test/resources/example_warc/IAH-20080430204825-00000-blackbook.warc");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 216504,false); //Image entry

        //System.out.println(arcEntry.getRedirectUrl());
        
    
    }
    

    @Test
    public void testLazyLoadBinary() throws Exception {
        
        File file = getFile("src/test/resources/example_warc/IAH-20080430204825-00000-blackbook.warc");        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 181688, false); //Image entry
        
        assertNull(arcEntry.getBinary());
        arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 181688,true); //Image entry and load binary
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
}
