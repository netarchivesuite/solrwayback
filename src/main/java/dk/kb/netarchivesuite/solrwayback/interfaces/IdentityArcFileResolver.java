package dk.kb.netarchivesuite.solrwayback.interfaces;


public class IdentityArcFileResolver implements ArcFileLocationResolverInterface {
  /*
   * This implementation just returns the same file location as output. Can be used if path to the arc-files is the same as 
   * the index field: source_file_path 
   */    
  @Override
  public String resolveArcFileLocation(String source_file_path) {        
    return source_file_path;
  }

}