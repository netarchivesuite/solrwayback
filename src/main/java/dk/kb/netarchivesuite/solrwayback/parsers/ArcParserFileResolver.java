package dk.kb.netarchivesuite.solrwayback.parsers;

import java.util.HashMap;

import dk.kb.netarchivesuite.solrwayback.interfaces.ArcSource;
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

  private static final HashMap<String, ArcSource> cache = new HashMap<>();

  private static ArcFileLocationResolverInterface resolver = new IdentityArcFileResolver(); // Default
  private static final Logger log = LoggerFactory.getLogger(ArcFileLocationResolverInterface.class);

  public static void setArcFileLocationResolver(ArcFileLocationResolverInterface resolverImpl) {
    resolver = resolverImpl;
  }

  /*
   * 
   * @param file_path is the file location, the file location must be resolved
   * first.
   * 
   * @param offset offset in the warc file
   */
  public static ArcEntry getArcEntry(String source_file_path_org, long offset) throws Exception {

    // Maybe this will stop code scan think parameter is used unvalidated.
    // It is validated later to be .warc/.warcs.gz/.arc/.arc.gz
    String source_file_path = source_file_path_org.trim();

    try {
      ArcSource arcSource = cache.get(source_file_path);
      if (arcSource == null) {

        arcSource = resolver.resolveArcFileLocation(source_file_path);
        cache.put(source_file_path, arcSource);
      }

      return ArcFileParserFactory.getArcEntry(arcSource, offset);

    } catch (Exception e) {
      // It CAN happen, but crazy unlikely, and not critical at all... (took 10
      // threads spamming 1M+ requests/sec for it to happen in a test.):
      log.error("Critical error resolving warc:" + source_file_path + " and offset:" + offset + " Error:" + e.getMessage());
      throw new Exception(e);
    }
  }

}
