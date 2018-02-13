package dk.kb.netarchivesuite.solrwayback.parsers;

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
 * It will just call the ArcFileParserFactory with the resolved filename.
 * 
 */
public class ArcParserFileResolver {

  private static ArcFileLocationResolverInterface resolver = new IdentityArcFileResolver(); //Default
  private static final Logger log = LoggerFactory.getLogger(ArcFileLocationResolverInterface.class);  
  
  public static void setArcFileLocationResolver(ArcFileLocationResolverInterface resolverImpl){
    resolver=resolverImpl;     
  }

  public static ArcEntry getArcEntry (String source_file_path, long offset) throws Exception{
    
    String fileLocation = resolver.resolveArcFileLocation(source_file_path);
    log.info("Resolved arcfile location:"+source_file_path +"->"+fileLocation);
        
    return ArcFileParserFactory.getArcEntry(fileLocation, offset);
    
  }
  
  
  
  
}
