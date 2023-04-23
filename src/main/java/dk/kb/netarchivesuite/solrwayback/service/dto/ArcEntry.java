package dk.kb.netarchivesuite.solrwayback.service.dto;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.annotation.XmlRootElement;

import dk.kb.netarchivesuite.solrwayback.interfaces.ArcSource;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.util.LimitedReader;
import it.unimi.dsi.fastutil.Arrays;
import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.parsers.ArcParser;
import dk.kb.netarchivesuite.solrwayback.parsers.WarcParser;

//Notice this class is returned both by the ArcParser and WarcParser.
//Could not think of a good common name...



@XmlRootElement
public class ArcEntry {

    private static boolean LENIENT_DECHUNK = true;
    
    public enum FORMAT {
        ARC,
        WARC
      }

    
    public enum TYPE {
      REVISIT,
      RESPONSE,
      ARC,
      RESOURCE,
    }
    
    
  private FORMAT format;
    
  private static final Logger log = LoggerFactory.getLogger(ArcEntry.class);
  private ArcSource arcSource;
  private long offset;  
  private boolean hasBeenDecompressed=false;
  private boolean chunked=false;
  private byte[] binary;
  private byte[] cachedBinary;
  private long binaryTrueSize;
  private int status_code;
  private String header;//Both headers for WARC.
  private String ip;
  private String url;
  private String contentCharset;
  private long contentLength;
  private long warcEntryContentLength; //From warc header#1. This does not exist for arc files
  private String contentType; //As returned by the webserver when harvested
  private TYPE type;  
  private String contentTypeExt; //As returned by the webserver when harvested
  private String fileName; //only filename
  private String crawlDate; // format 2009-12-09T05:32:50Z
  private String contentEncoding;
  private String waybackDate; // format 20080331193532
  private String redirectUrl; //null if not redirect
  
  
  
  public ArcSource getArcSource() {
    return arcSource;
}
public void setSource(ArcSource arcSource) {
    this.arcSource = arcSource;
}
public long getOffset() {
    return offset;
}
public void setOffset(long offset) {
    this.offset = offset;
}

/*
 * Will only be loaded if specificed when constructed
 */
 public byte[] getBinaryDisabled() {
    return binary;
  }
  public void setBinaryDisabled(byte[] binary) {
    //very dirty hack for now.          
    this.binary = binary;
  }
  public long getContentLength() {
    return contentLength;
  }
  public void setContentLength(long contentLength) {
    this.contentLength = contentLength;
  }
  public String getContentType() {
    return contentType;
  }
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }
  public String getFileName() {
    return fileName;
  }
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
  public long getWarcEntryContentLength() {
    return warcEntryContentLength;
  }
  public void setWarcEntryContentLength(long warcEntryContentLength) {
    this.warcEntryContentLength = warcEntryContentLength;
  }
  public String getCrawlDate() {
    return crawlDate;
  }
  public void setCrawlDate(String crawlDate) {
    this.crawlDate = crawlDate;
  }
  
  public String getWaybackDate() {
    return waybackDate;
  }
  public void setWaybackDate(String waybackDate) {
    this.waybackDate = waybackDate;
  }
  public String getUrl() {
    return url;
  }  
  
  /*
   *   In warc-header Target-URI can be enclosed in < and >. Remove them if that is the case   
   *   heritrix: WARC-Target-URI: https://test.dk/
   *   wget: WARC-Target-URI: <https://test.dk/>  
   */
  public void setUrl(String url) {
     if (url.startsWith("<") && url.endsWith(">")){// remove <>
        url = url.substring(1,url.length()-1);
     }      
     this.url = url;
     }
  
  public String getContentEncoding() {
    return contentEncoding;
  }

  
  
  public long getBinaryArraySize() {
    return binaryTrueSize;
}
public void setBinaryArraySize(long binaryArraySize) {
    this.binaryTrueSize = binaryArraySize;
}
/**
   * Lenient setter for content-encoding (compression).
   * Will trim leading and trailing whitespace and remove {@code "}-characters.
   * @param contentEncoding the encoding to use when retrieving content.
   */
  public void setContentEncoding(String contentEncoding) {
    this.contentEncoding = contentEncoding == null ? null : contentEncoding.trim().replace("\"", "");
  }
  public String getContentTypeExt() {
    return contentTypeExt;
  }
  public void setContentTypeExt(String contentTypeExt) {
    this.contentTypeExt = contentTypeExt;
  }

  public String getContentCharset() {
    return contentCharset;
  }

  /**
   * Lenient setter for content-charset. Will trim leading and trailing whitespace and remove {@code "}-characters.
   * @param contentCharset the encoding to use when retrieving content as a String.
   */
  // TODO: How does this differ from contentEncoding?
  public void setContentCharset(String contentCharset) {
    this.contentCharset = contentCharset == null ? null : contentCharset.trim().replace("\"", "");
  }
  public String getHeader() {
    return header;
  }

  public void setHeader(String header) {
    this.header = header;
  }
  public String getIp() {
    return ip;
  }
  public void setIp(String ip) {
    this.ip = ip;
  }
  public int getStatus_code() {
    return status_code;
  }
  public void setStatus_code(int status_code) {
    this.status_code = status_code;
  }
  public String getRedirectUrl() {
    return redirectUrl;
  }
  public void setRedirectUrl(String redirectUrl) {
    this.redirectUrl = redirectUrl;
  } 
  
  public boolean isHasBeenDecompressed() {
    return hasBeenDecompressed;
  }
  public void setHasBeenDecompressed(boolean hasBeenDecompressed) {
    this.hasBeenDecompressed = hasBeenDecompressed;
  }
    
  
public TYPE getType() {
    return type;
  }
  public void setType(TYPE type) {
    this.type = type;
  }
public void setChunked(boolean chunked) {
    this.chunked = chunked;
}


public  FORMAT getFormat() {
    return format;
}
public void setFormat(FORMAT format) {
    this.format = format;
}

    /**
     * Constructs an (W)ARC neutral {@code InputStream} that delivers the binary content for this (W)ARC entry.
     * The content is the resource itself, sans HTTP headers or similar.
     * <p>
     * If the (W)ARC is marked as gzip-compressed, the content will be automatically gzip-uncompressed.
     * <p>
     * This method does not handle decompression or dechunking outside of basic (W)ARC compression.
     * <p>
     * The caller should take care to close the returned {@code InputStream} after use as failing to do so
     * might cause resource leaks.
     * @return a stream with the binary content from this (W)ARC entry.
     * @throws IOException if the binary could not be read.
     * @see #getBinaryNoChunking()
     * @see #getBinaryDecoded()
     */
    public BufferedInputStream getBinaryRaw() throws IOException {
        if (cachedBinary != null) {
            return new BufferedInputStream(new ByteArrayInputStream(cachedBinary));
        }
        // TODO: If binaryArraySize is "small" this should be cached
        switch (format) {
            case ARC:
                return ArcParser.lazyLoadContent(arcSource, offset);
            case WARC:
                return WarcParser.lazyLoadBinary(arcSource, offset);
            default:
                throw new UnsupportedOperationException(
                        "Loading of binaries for the format '" + format + "' is unsupported. " +
                        "Offending URL is '" + url + "'");
        }
    }

    /**
     * De-chunks (see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Transfer-Encoding#chunked_encoding) the
     * binary delivered form {@link #getBinaryRaw()} but does not change anything else.
     * <p>
     * Note that it is possible for the content to be compressed as it was delivered by the web server.
     * In general this method is only used for proxying content from source (W)ARCs to new WARCs.
     * <p>
     * The content is the resource itself, sans HTTP headers or similar.
     * @return a stream with the binary content from this (W)ARC entry, guaranteed not to be chunked.
     * @throws IOException if the binary could not be read.
     * @see #getBinaryRaw()
     * @see #getBinaryDecoded()
     */
    public InputStream getBinaryNoChunking() throws IOException {
        return maybeDechunk(getBinaryRaw());
    }

    /**
     * De-chunks (see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Transfer-Encoding#chunked_encoding),
     * un-zips and un-Brotlis the binary for the binary delivered form {@link #getBinaryRaw()} but does not change
     * anything else.
     * <p>
     * In general this method is used for further processing of the content, such as determining image dimensions
     * or extracting links from webpages.
     * <p>
     * The content is the resource itself, sans HTTP headers or similar.
     * @return a stream with the binary content from this (W)ARC entry, guaranteed not to be chunked or compressed.
     * @throws IOException if the binary could not be read.
     * @see #getBinaryRaw()
     * @see #getBinaryNoChunking()
     */
    public InputStream getBinaryDecoded() throws IOException {
        //Chain the inputstreams in correct order.
        InputStream maybeDechunked = getBinaryNoChunking();
        InputStream maybeUnzipped = maybeUnzip(maybeDechunked);
        return maybeBrotliDecode(maybeUnzipped);
    }

    /**
     * Wrapper for {@link #getBinaryDecoded()} that delivers the full binary content as a {@code byte[]}.
     * <p>
     * Note: This method returns the full content which can lead to Out Of Memory.
     *       This is the least recommended method for retrieving binaries from the entry.
     * @return the full binary content.
     * @see #getStringContentAsStringSafe()
     */
    public byte[] getBinaryDecodedBytes() throws IOException {
        if (binaryTrueSize > Arrays.MAX_ARRAY_SIZE) {
            String message = String.format(
                    Locale.ROOT, "Binary size too large for java byte[]. Size: %d, source='%s', url='%s'",
                    binaryTrueSize, getArcSource().getSource(), url);
            log.error(message);
            throw new ArrayIndexOutOfBoundsException(message);
        }

        byte[] bytes = new byte[(int) binaryTrueSize];

        // we are not using IOUtils.readFully as we'd rather return non-complete data than nothing
        int read = IOUtils.read(getBinaryDecoded(), bytes);
        if (read == -1) {
            log.warn("Attempted to load binary for {}#{} but got EOF immediately",
                     getArcSource().getSource(), getOffset());
        } else if (read < bytes.length) {
            log.warn("Incomplete binary for {}#{}: {}/{} bytes read",
                     getArcSource().getSource(), getOffset(), read, bytes.length);
        }
        return bytes;
    }


    /**
     * Wrapper for {@link #getBinaryDecoded()} that converts the binary content to characters using the encoding
     * in the HTTP headers or UTF-8 if no encoding has been provided.
     * <p>
     * Note: This method returns the full content which can lead to Out Of Memory if the output is stored on heap.
     * Consider using {@link #getStringContentAsStringSafe()} or {@link #getStringContentAsStringSafe()} instead.
     * @return the binary content as characters.
     * @see #getStringContentAsStringSafe()
     */
    public Reader getStringContentFull() throws IOException {
        return new InputStreamReader(getBinaryDecoded(), getCharsetSafe());
    }

    /**
     * Wrapper for {@link #getStringContentFull()} that returns at most
     * {@link PropertiesLoaderWeb#WARC_ENTRY_TEXT_MAX_CHARACTERS} characters. Excess characters are ignored.
     * <p>
     * This is a recommended method for retrieving textual content for further on-heap processing.
     * @return at most {@link PropertiesLoaderWeb#WARC_ENTRY_TEXT_MAX_CHARACTERS} characters from
     *         {@link #getStringContentFull()}.
     * @see #getStringContentFull()
     * @see #getStringContentAsStringSafe()
     */
    public Reader getStringContentSafe() throws IOException {
        return new LimitedReader(getStringContentFull(), PropertiesLoaderWeb.WARC_ENTRY_TEXT_MAX_CHARACTERS);
    }

    /**
     * Memory limited wrapper for {@link #getStringContentFull()} that returns at most
     * {@link PropertiesLoaderWeb#WARC_ENTRY_TEXT_MAX_CHARACTERS} characters. Excess characters are ignored.
     * <p>
     * This is a recommended method for retrieving textual content for further on-heap processing.
     * @return at most {@link PropertiesLoaderWeb#WARC_ENTRY_TEXT_MAX_CHARACTERS} characters from
     *         {@link #getStringContentFull()}.
     * @see #getStringContentSafe()
     * @see #getStringContentFull()
     */
    public String getStringContentAsStringSafe() throws IOException {
        int max = PropertiesLoaderWeb.WARC_ENTRY_TEXT_MAX_CHARACTERS;
        StringWriter sw = new StringWriter();
        long expected = Math.min(max, binaryTrueSize);
        try {
            long charCount = IOUtils.copyLarge(getStringContentFull(), sw, 0, max);
            if (charCount == max) {
                // #bytes != #characters, but this is acceptable for logging
                log.debug("getStringContentSafe() skipped approximately {} trailing characters due to {}={} for '{}'",
                          binaryTrueSize - max, PropertiesLoaderWeb.WARC_ENTRY_TEXT_MAX_CHARACTERS_PROPERTY,
                          PropertiesLoaderWeb.WARC_ENTRY_TEXT_MAX_CHARACTERS, url);
            }
            return sw.toString();
        } catch (Exception e) {
            String message = String.format(
                    Locale.ROOT, "Exception while converting %.2fMB of binary for '%s' to String using charset '%s'",
                    expected/1048576.0, getUrl(), getCharsetSafe());
            log.warn(message, e);
            throw new IOException(message, e);
        } catch (OutOfMemoryError e) {
            String message = String.format(
                    Locale.ROOT, "OutOfMemoryError while converting %.2fMB of binary for '%s' to String using charset '%s'",
                    expected/1048576.0, getUrl(), getCharsetSafe());
            log.error(message, e);
            OutOfMemoryError oome = new OutOfMemoryError(message);
            oome.initCause(e);
            // TODO: Consider downgrading this to a standard Exception as the OOM (hopefully) does not make the overall state inconsistent
            throw oome;
        }
    }

    /**
     * Sets the binary for this (W)ARC entry representation to the given content.
     * The content will be converted to binary with respect to {@link #getContentCharset()} and potentially compressed,
     * depending on {@link #getContentEncoding()}.
     * <p>
     * Warning: Although an attempt is made to keep the content representation synced to the original HTTP headers,
     *          this is not guaranteed.
     * @param content replacement for the existing binary.
     */
    public void setStringContent(String content) {
        if (contentEncoding != null &&
            !contentEncoding.equalsIgnoreCase("gzip") &&
            !contentEncoding.equalsIgnoreCase("x-gzip")) {
            log.debug("setStringContent(...) has to disable content-encoding as encoding of '{}' compression " +
                      "is not supported", contentEncoding);
            contentEncoding = null;
            hasBeenDecompressed = true;
        }

        if ("gzip".equalsIgnoreCase(contentEncoding) || "x-gzip".equalsIgnoreCase(contentEncoding)) {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
                 GZIPOutputStream gout = new GZIPOutputStream(bos)) {
                IOUtils.copy(new StringReader(content), gout, getCharsetSafe());
                cachedBinary = bos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("IOException while assigning content", e);
            }
        } else {
            cachedBinary = content.getBytes(getCharsetSafe());
        }
        binaryTrueSize = cachedBinary.length;
        setChunked(false);
        // TODO: Mask the getter methods for binary
    }

  private InputStream maybeBrotliDecode(InputStream before) throws IOException {
      if (!"br".equalsIgnoreCase(contentEncoding)) {
          return before;
      }
      log.info("brotli decode");
      InputStream brIs = new BrotliInputStream(before);
      this.setContentEncoding("identity");
      return brIs;
  }

  /* Will fail if stream is not chuncked, and it seems this can happen. (Do not trust http header)
  private InputStream maybeDechunk(InputStream before) throws Exception{
    
      
      if (isChunked()) {
      log.info("dechunking");
      this.setChunked(false);
          return new ChunkedInputStream(before);
          
      }
      else {
          return before;
      }                
  }
  */
  
  private InputStream maybeUnzip(InputStream before) throws IOException {
      if ("gzip".equalsIgnoreCase(contentEncoding) || "x-gzip".equalsIgnoreCase(contentEncoding)) {
//          this.setContentEncoding("identity");
          return new GZIPInputStream(before);
      }
      else {
          return before;
      }                
  }

  /**
   * Checks if an input stream seems to be chunked. If so, the stream content is de-chunked.
   * If not, the stream content is returned unmodified.
   * Chunked streams must begin with {@code ^[0-9a-z]{1,8}(;.{0,1024})?\r\n}.
   * Note: Closing the returned stream will automatically close input.
   * @param input a stream with the response body from a HTTP-response.
   * @return the un-chunked content of the given stream.
   * @throws IOException if the stream could not be processed.
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Transfer-Encoding">Transfer-Encoding</a>
   */
  public static InputStream maybeDechunk(InputStream input) throws IOException {
      final BufferedInputStream buf = new BufferedInputStream(input) {
          @Override
          public void close() throws IOException {
              super.close();
              input.close();
          }
      };
      buf.mark(1024); // Room for a lot of comments
      int pos = 0;
      int c = -1;
      // Check for hex-number
      while (pos <= 8) { // Max 8 digits + the character after
          c = buf.read();
          if (c== -1) { // EOF
              log.debug("maybeDechunk reached EOF while looking for hex digits at pos " + pos + ": " +
                        "Not a chunked stream, returning content as-is");
              buf.reset();
              return buf;
          }
          if (('0' <= c && c <= '9') || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F')) {
              pos++;
              continue;
          }
          break;
      }
      if (pos == 0 || pos > 8) {

        //log.debug("maybeDechunk found " + pos + " hex digits: Not a chunked stream, returning content as-is");
          buf.reset();
          return buf;
      }
      // Check for \r\n or extension
      if (c == -1) { // EOF
          log.debug("maybeDechunk reached EOF while looking for extension or \\r\\n at pos " + pos + ": " +
                    "Not a chunked stream, returning content as-is");
          buf.reset();
          return buf;
      }
      pos++;
      if (c == ';') { // Extension
          while (pos < 1024) {
              while (pos < 1024 && c != '\r' && c != -1) { // Look for CR
                  c = buf.read();
                  pos++;
              }
              if (c == -1) {
                  break;
              }
              c = buf.read();
              pos++;
              if (c == '\n' || c == -1) { // LF
                  break;
              }
          }
          if (pos == 1024 || c == -1) {
              log.info("maybeDechunk found hex digits and start of an extension but could not locate CRLF: " +
                        "Not a chunked stream, returning content as-is for ");
              buf.reset();
              return buf;
          }
          log.debug("maybeDechunk found hex digits and an extension: Probably chunked stream, returning content " +
                    "wrapped in a de-chunker for " );
          return dechunk(buf);
      }
      // Not with extension. Next chars must be CRLF
      if (c == '\r') { // CR
          c = buf.read();
          if (c == '\n') { // LF
              log.debug("maybeDechunk found hex digits CRLF: Probably chunked stream, returning content " +
                        "wrapped in a de-chunker");
              return dechunk(buf);
          }
          log.info("maybeDechunk found hex digits followed by CR (0x" + Integer.toHexString('\r') +
                   ") and but the charactor following that was 0x" + Integer.toHexString(c) +
                   " and not LF (" + Integer.toHexString('\n') + ") ");
      } else if (c == '\n') {
          if (LENIENT_DECHUNK) {
              log.debug("maybeDechunk found hex digits followed by LF (0x" + Integer.toHexString('\n') +
                       ") but expected CRLF. This is likely chunking delivered by a non-standard compliant server." +
                       " The de-chunker is lenient and accepts this ");
              return dechunk(buf);
          }
          log.info("maybeDechunk found hex digits followed by LF (0x" + Integer.toHexString('\n') +
                   ") but expected CRLF. This is likely chunking delivered by a non-standard compliant server." +
                   " The de-chunker is not lenient and will return the stream as-is");
      } else {
        /*  
        log.debug("maybeDechunk found hex digits but could not locate CRLF. Instead it found 0x" +
                   Integer.toHexString(c) + ": Not a chunked stream, returning content as-is for");
                   */
      }
      buf.reset();
      return buf;
  }
  private static ChunkedInputStream dechunk(InputStream in) throws IOException {
      in.reset();
      return new ChunkedInputStream(in) {
          @Override
          public void close() throws IOException {
              super.close();
              in.close();
          }
      };
  }

    /**
     * @return the charset from the HTTP header if possible, else {@code utf-8}.
     */
    private Charset getCharsetSafe() {
        Charset charset;
        String encoding = this.getContentCharset();
        charset = StandardCharsets.UTF_8;
        if (encoding == null) {
            encoding = "UTF-8";
        }
        try {
            charset = Charsets.toCharset(encoding);
        } catch (Exception e) {
            log.debug("The encoding '{}' for '{}' is not supported. Falling back to UTF-8", encoding, url);
        }
        return charset;
    }

}
