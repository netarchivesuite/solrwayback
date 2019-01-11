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
import dk.kb.netarchivesuite.solrwayback.parsers.Normalisation;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;


public class WarcParserTest extends UnitTestUtils{
       
    @Test
    public void testWarcParser() throws Exception {
        
        File file = getFile("src/test/resources/example_warc/IAH-20080430204825-00000-blackbook.warc");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 181688); //Image entry
        assertEquals("image/jpeg", arcEntry.getContentType());
        assertEquals("hewlett.jpg", arcEntry.getFileName());
        assertEquals(7812, arcEntry.getWarcEntryContentLength());
        assertEquals(7510, arcEntry.getContentLength());
        assertEquals(200,arcEntry.getStatus_code());
        
        BufferedImage image = ImageUtils.getImageFromBinary(arcEntry.getBinary());
        assertEquals(300,image.getWidth());
        assertEquals(116,image.getHeight());        
        assertEquals(" http://www.archive.org/images/hewlett.jpg",arcEntry.getUrl());
        
        System.out.println(arcEntry.getCrawlDate());
        System.out.println(arcEntry.getWaybackDate());
    
    }
    
    @Test
    public void testRedirect() throws Exception {

        File file = getFile("src/test/resources/example_warc/IAH-20080430204825-00000-blackbook.warc");
        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 216504); //Image entry

        
        System.out.println(arcEntry.getRedirectUrl());
        
    
    }
    
    

             
}
