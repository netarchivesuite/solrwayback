package dk.kb.netarchivesuite.solrwayback.interfaces;

import java.util.Map;
import java.util.function.Supplier;

public interface ArcFileLocationResolverInterface {

  /**
   * Returns a {@link Supplier} that delivers an InputStream for the given (WARC) source file.
   * The supplier can be called multiple times, each time delivering an InputStream positioned at the beginning of
   * the source file.
   * It is the responsibility of the caller to close the InputStream after use.
   *
   * This level of indirection allows for handling of moved files, WARCs delivered over HTTP or similar.
   * A simple situation is just a string manipulation of the url, a more complicated situation can be
   * using a lookup service give the filename.  
   *  
   * Implementing classes must have the default constructor.
   * Parameters are given by setParameters method.
   *     
   * @param source_file_path is the complete file path when the arc file was indexed. example : /mountA/0211/filedir/12345.warc.gz
   * @return a Supplier that delivers an InputStream for the (w)arc file, positioned at the beginning.
   */
  ArcSource resolveArcFileLocation(String source_file_path);

  void setParameters(Map<String, String> parameters);
  void initialize();

}
