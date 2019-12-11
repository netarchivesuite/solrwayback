package dk.kb.netarchivesuite.solrwayback.parsers;

import java.util.ConcurrentModificationException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.interfaces.ArcFileLocationResolverInterface;
import dk.kb.netarchivesuite.solrwayback.interfaces.IdentityArcFileResolver;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

/*
 * This class will resolve the arc-file location using source_file_path from the index.
 * The resolver class is defined in solrwayback.properties.
 * Default is the identity resolver, which can be used if file locations seen from solrwayback is the same as source_file_path
 * 
 * When a file has been resolved it will cache the location making future requests fasters. 
 * TODO implement a max cache size.
 *  
 * It will just call the ArcFileParserFactory with the resolved filename.
 * 
 */
public class ArcParserFileResolver {

  private static HashMap<String, String> cache = new HashMap<String, String>();

  private static ArcFileLocationResolverInterface resolver = new IdentityArcFileResolver(); // Default
  private static final Logger log = LoggerFactory.getLogger(ArcFileLocationResolverInterface.class);

  public static void setArcFileLocationResolver(ArcFileLocationResolverInterface resolverImpl) {
    resolver = resolverImpl;
  }

  public static ArcEntry getArcEntry(String source_file_path, long offset) throws Exception {
    try {
      String cached = cache.get(source_file_path);
      String fileLocation = null;
      if (cached != null) {
        fileLocation = cached;
        // log.info("Using cached arcfile location:"+source_file_path +"->"+fileLocation);
      } else {
        fileLocation = resolver.resolveArcFileLocation(source_file_path);
        cache.put(source_file_path, fileLocation);
        log.debug("Resolved arcfile location:" + source_file_path + "->" + fileLocation);
      }
      return ArcFileParserFactory.getArcEntry(fileLocation, offset);
    } catch (Exception e) {
      // It CAN happen, but crazy unlikely, and not critical at all... (took 10 threads spamming 1M+ requests/sec for it to happen in a test.):
      log.error("Critical error resolving warc:"+source_file_path +" and offset:"+offset +" Error:",e.getMessage());
      throw new Exception(e);
    }

  }

}
