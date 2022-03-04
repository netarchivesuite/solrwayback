package dk.kb.netarchivesuite.solrwayback.interfaces;


import java.util.Map;

public class IdentityArcFileResolver implements ArcFileLocationResolverInterface {
  /*
   * This implementation just returns the same file location as output. Can be used if path to the arc-files is the same as 
   * the index field: source_file_path
   */    
  @Override
  public ArcSource resolveArcFileLocation(String source_file_path) {
    return ArcSource.fromFile(source_file_path);
  }
  @Override
  public void setParameters(Map<String, String> parameters) {
    //Does not use parameters
  }
  
  @Override
  public void initialize() {
    // do noting
  }
  
}