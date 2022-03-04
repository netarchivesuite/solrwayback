package dk.kb.netarchivesuite.solrwayback.util;

import java.io.EOFException;
import java.io.IOException;
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

    /**
     * Re-implementation of {@link org.apache.commons.io.IOUtils#skipFully} using {@link InputStream#skip} instead of
     * {@link InputStream#skip} to allow for efficient skipping.
     * @param input stream to skip.
     * @param toSkip the number of bytes to skip.
     * @return the number of bytes skipped. This will always be equal to toSkip as everything else raises an Exception.
     * @throws IOException              if there is a problem reading the file.
     * @throws IllegalArgumentException if toSkip is negative.
     * @throws EOFException             if the number of bytes skipped was incorrect.
     */
    public static long skipFully(final InputStream input, final long toSkip) throws IOException {
        if (toSkip < 0) {
            throw new IllegalArgumentException("Bytes to skip must not be negative: " + toSkip);
        }
        final long skipped = skip(input, toSkip);
        if (skipped != toSkip) {
            throw new EOFException("Bytes to skip: " + toSkip + " actual: " + skipped);
        }
        return toSkip;
    }

    /**
     * Ported and adjusted from {@link org.apache.commons.io.IOUtils#read} for using {@code read} to use
     * {@code skip} to allow for efficient skipping.
     * This implementation guarantees that it will skip as many bytes
     * as possible before giving up; this may not always be the case for
     * skip() implementations in subclasses of {@link InputStream}.
     * @param input byte stream to skip
     * @param toSkip number of bytes to skip.
     * @return number of bytes actually skipped.
     * @throws IOException              if there is a problem reading the file
     * @throws IllegalArgumentException if toSkip is negative
     */
    public static long skip(final InputStream input, final long toSkip) throws IOException {
        if (toSkip < 0) {
            throw new IllegalArgumentException("Skip count must be non-negative, actual: " + toSkip);
        }
        long remain = toSkip;
        while (remain > 0) {
            // See https://issues.apache.org/jira/browse/IO-203 for why we use read() rather than delegating to skip()
            final long n = input.skip(remain);
            if (n == 0) { // skip has no explicit EOF mechanism. We do not retry when we skip nothing
                break;
            }
            remain -= n;
        }
        return toSkip - remain;
    }
}
