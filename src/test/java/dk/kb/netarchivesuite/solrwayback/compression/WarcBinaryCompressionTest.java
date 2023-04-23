package dk.kb.netarchivesuite.solrwayback.compression;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;


/*
 * The binary in the warc-files can be
 * 1) Brotli encoded
 * 2) Gziped
 * 3) Chucked
 * 4) Various combination of above. GZIP+Chunked is 
 * 
 * For parsing the HTML we need to load the binary as clear text with no compression.
 * The chuncking must always be removed when streaming the resources since we can not
 * control the chuking done by the tomcat or apache server on solrwayback.war. 
 * So it will be chucked automaticaly with HTTP/1.1  
 * 
 */
public class WarcBinaryCompressionTest extends UnitTestUtils{

    private static String HTML_TEXT_PART="Extremely simple webpage used for testing GZip and Brotli transmission compression.";
    
    @Test
    public void testCompressionsNoneGz() throws Exception {        
        File file = getFile("compressions_warc/transfer_compression_none.warc.gz");        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 881); //HTML entry
        String content = arcEntry.getStringContentAsStringSafe();
        assertTrue(content.indexOf(HTML_TEXT_PART)> 0);                         
    }
    
    @Test
    public void testCompressionsNone() throws Exception {        
        File file = getFile("compressions_warc/transfer_compression_none.warc");        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 1198); //HTML entry
        String content = arcEntry.getStringContentAsStringSafe();
        assertTrue(content.indexOf(HTML_TEXT_PART)> 0);                         
    }

    @Test
    public void testCompressionsZipGz() throws Exception {        
        File file = getFile("compressions_warc/transfer_compression_gzip.warc.gz");        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 899); //HTML entry
        String content = arcEntry.getStringContentAsStringSafe();
        assertTrue(content.indexOf(HTML_TEXT_PART)> 0);                         
    }

    @Test
    public void testCompressionsZip() throws Exception {        
        File file = getFile("compressions_warc/transfer_compression_gzip.warc");        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 1227); //HTML entry
        String content = arcEntry.getStringContentAsStringSafe();
        assertTrue(content.indexOf(HTML_TEXT_PART)> 0);                         
    }

    

    @Test
    public void testCompressionsBrotli() throws Exception {        
        File file = getFile("compressions_warc/transfer_compression_brotli.warc");        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 1227); //HTML entry
        String content = arcEntry.getStringContentAsStringSafe();
        assertTrue(content.indexOf(HTML_TEXT_PART)> 0);    
        //System.out.println(content);
    }


    @Test
    public void testCompressionsBrotliGz() throws Exception {        
        File file = getFile("compressions_warc/transfer_compression_brotli.warc.gz");        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 898); //HTML entry
        String content = arcEntry.getStringContentAsStringSafe();
        assertTrue(content.indexOf(HTML_TEXT_PART)> 0);    
        //System.out.println(content);
    }

    
    
    @Test
    public void testCompressionsZipChunkedGz() throws Exception {        
        File file = getFile("compressions_warc/transfer_compression_gzip_chunked.warc.gz");        
        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 275); //HTML entry
        String content = arcEntry.getStringContentAsStringSafe();
            
       // System.out.println(content);
    }
    
}
