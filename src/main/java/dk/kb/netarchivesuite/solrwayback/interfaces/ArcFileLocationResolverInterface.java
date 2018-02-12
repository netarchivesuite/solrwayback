package dk.kb.netarchivesuite.solrwayback.interfaces;

public interface ArcFileLocationResolverInterface {

  /*
   * This method must return the new file location path from the original source_file_path
   * It is only required if the path has been changed since indexing. This can be the case if
   * it was index on another computer and the file-path is different compared to where solrwayback is running. 
   * A simple situation is just a string manipulation of the url, a more complicated situation can be
   * using a lookup service give the filename.  
   *  
   * Implementing classes must have the default constructor
   *     
   *@param source_file_path is the complete file path when the arc file was indexed. example : /mountA/0211/filedir/12345.warc.gz
   *@return the new path : ie /mountB/12345.warc.gz   
   */
  String resolveArcFileLocation(String source_file_path);
  
  
}
