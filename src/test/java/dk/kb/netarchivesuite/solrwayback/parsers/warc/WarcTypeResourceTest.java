package dk.kb.netarchivesuite.solrwayback.parsers.warc;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;

import org.junit.Test;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.image.ImageUtils;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

public class WarcTypeResourceTest  extends UnitTestUtils{

  
  @Test
  public void testWarcParserTypeResource() throws Exception {
      
    
/*  warc record header

WARC-Source-URI: file:///pool0/7-DNPP/uncompressed/nederland/content/pimfortuyn/20020308/www.pimfortuyn.nl/images/newsBottomBG.jpg
WARC-Creation-Date: 2021-08-27T19:47:16Z
WARC-Type: resource
WARC-Record-ID: <urn:uuid:23f91ed9-da60-4f2b-9dd1-bf39692a7ee1>
WARC-Target-URI: http://www.pimfortuyn.nl/images/newsBottomBG.jpg
WARC-Payload-Digest: sha1:YCLC2FUYV57VOXXTFVZYHCPMWGTQ4N2O
WARC-Block-Digest: sha1:YCLC2FUYV57VOXXTFVZYHCPMWGTQ4N2O
Content-Type: image/jpeg
Content-Length: 1168

*/
    
      File file = getFile("src/test/resources/example_warc/20020308.warc.gz");
      
      ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 64049); //Image entry
      assertEquals(ArcEntry.TYPE.RESOURCE, arcEntry.getType());
      assertEquals(1168, arcEntry.getWarcEntryContentLength());
      assertEquals(200,arcEntry.getStatus_code());
      assertEquals("20020308000000",arcEntry.getWaybackDate());      

      //NO http header at all.
      assertEquals(null, arcEntry.getContentType());
      assertEquals(0, arcEntry.getContentLength());
      
      BufferedImage image = ImageUtils.getImageFromBinary(arcEntry.getBinary());
      assertEquals(135,image.getWidth());
      assertEquals(29,image.getHeight());        
      assertEquals("http://www.pimfortuyn.nl/images/newsBottomBG.jpg",arcEntry.getUrl());    
  
  }
  
}
