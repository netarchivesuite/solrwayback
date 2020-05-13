package dk.kb.netarchivesuite.solrwayback.encoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import org.junit.Test;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.encoders.Sha1Hash;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

public class SHA1Test extends UnitTestUtils{
         
  	  @Test
      public void testSha1Hash() {  	
  		  
  		  try {
  			//Do a real calculation on the binary in a warc-file.  			  
  			  /* header for the entry with offset: 181688
  			   
  			    WARC/0.17
                WARC-Type: response
                WARC-Target-URI: http://www.archive.org/images/hewlett.jpg
                WARC-Date: 2008-04-30T20:48:35Z
                WARC-Payload-Digest: sha1:5NAYYF4QDMNTCMGOQUJ6DQTCEIB7QKFS
                WARC-IP-Address: 207.241.229.39
                WARC-Record-ID: <urn:uuid:0a556a1b-a3b3-4ec0-82e5-7b54ed5293cb>
                Content-Type: application/http; msgtype=response
                Content-Length: 7812
  			   */  			  
  			  
  	        File file = getFile("src/test/resources/example_warc/IAH-20080430204825-00000-blackbook.warc");  	        
  	        ArcEntry arcEntry = Facade.getArcEntry(file.getCanonicalPath(), 181688); //Image entry  			      		
  			byte[] bytes = arcEntry.getBinary();    			
  		    InputStream is = new ByteArrayInputStream(bytes);                       
  		    String hash= Sha1Hash.createSha1(is);
            assertEquals("sha1:5NAYYF4QDMNTCMGOQUJ6DQTCEIB7QKFS", hash); //This is the sha1 value in the warc file       
  		  }
  		  catch(Exception e) {  			 
  			  e.printStackTrace();
  			  fail("Error calculating SHA1");
  		  }
       
    }        
}
