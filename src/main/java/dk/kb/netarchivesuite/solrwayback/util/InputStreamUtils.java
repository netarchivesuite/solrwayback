package dk.kb.netarchivesuite.solrwayback.util;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class InputStreamUtils {

  
  private static Log log = LogFactory.getLog(InputStreamUtils.class );
  
  /*
   * This method will detect if the inputstream is gzip and unzip the inputstream.
   * If inputstream is not zippet, the same inputstream will be returned. 
   *  
   */
  public static InputStream maybeDecompress(InputStream input) throws Exception {
    final PushbackInputStream pb = new PushbackInputStream(input, 2);

    int header = pb.read();
    if(header == -1) {
        return pb;
    }

    int b = pb.read();
    if(b == -1) {
        pb.unread(header);
        return pb;
    }

    pb.unread(new byte[]{(byte)header, (byte)b});

    header = (b << 8) | header;

    if(header == GZIPInputStream.GZIP_MAGIC) {
     log.debug("GZIP stream detected");  
      return new GZIPInputStream(pb);
    } else {
        return pb;
    }
 }
  
}
