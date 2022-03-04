package dk.kb.netarchivesuite.solrwayback.interfaces;

import static org.junit.Assert.assertEquals;

import java.io.File;
import org.junit.Test;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;

public class FileMovedMappingResolverTest extends UnitTestUtils {
  
  @Test
  public void testFileMovedMappingResolver() throws Exception {
    File file = getFile("src/test/resources/arc_resolvers/FileMovedMappingTest.txt");

    FileMovedMappingResolver resolver = new FileMovedMappingResolver();
    resolver.setMappingFile(file.getCanonicalPath());
    resolver.initialize();

    //Some warc-files not defined in the moved list
    String warc1="/abc/test/example.warc";
    String warc2="/netarchivemount/warcs/111/example111.warc";
    assertEquals(warc1, resolver.resolveArcFileLocation(warc1).getSource());
    assertEquals(warc2, resolver.resolveArcFileLocation(warc2).getSource());
    
    //These two has been moved
    String warc3="/home/xxx/solrwayback_package_4.2.1/indexing/warcs1/356548-347-20210201093000132-00000-sb-prod-har-001.statsbiblioteket.dk.warc.gz";
    String warc4="/mount/netarchive/test-00000.warc.gz";

    assertEquals(warc3, resolver.resolveArcFileLocation("/home/old/location/356548-347-20210201093000132-00000-sb-prod-har-001.statsbiblioteket.dk.warc.gz").getSource());
    assertEquals(warc4, resolver.resolveArcFileLocation("/oldlocation/test-00000.warc.gz").getSource());
        
    
    
  }
  
}
