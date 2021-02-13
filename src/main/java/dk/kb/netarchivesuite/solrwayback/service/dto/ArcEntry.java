package dk.kb.netarchivesuite.solrwayback.service.dto;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.httpclient.ChunkedInputStream;
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

    
    
  private FORMAT format;
    
  private static final Logger log = LoggerFactory.getLogger(ArcEntry.class);
  private String sourceFilePath; //full path
  private long offset;  
  private boolean hasBeenDecompressed=false;
  private boolean chunked=false;
  private byte[] binary;
  private int status_code;
  private String header;//Both headers for WARC.
  private String ip;
  private String url;
  private String contentCharset;
  private long contentLength;
  private long warcEntryContentLength; //From warc header#1. This does not exist for arc files
  private long binaryArraySize;
  private String contentType; //As returned by the webserver when harvested
  private String contentTypeExt; //As returned by the webserver when harvested
  private String fileName; //only filename
  private String crawlDate; // format 2009-12-09T05:32:50Z
  private String contentEncoding;
  private String waybackDate; // format 20080331193532
  private String redirectUrl; //null if not redirect
  
  
  
  public String getSourceFilePath() {
    return sourceFilePath;
}
public void setSourceFilePath(String sourceFilePath) {
    this.sourceFilePath = sourceFilePath;
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
 public byte[] getBinary() {
    return binary;
  }
  public void setBinary(byte[] binary) {
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
    return binaryArraySize;
}
public void setBinaryArraySize(long binaryArraySize) {
    this.binaryArraySize = binaryArraySize;
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
    
public boolean isChunked() {
    return chunked;
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
/*
 * Will wrap the byte[] in a BufferedInputStream
 * 
 * It is up the caller to close the inputstream!
 * 
 */
  public BufferedInputStream getBinaryLazyLoad() throws Exception{
      if (format.equals(FORMAT.ARC)) {
         return ArcParser.lazyLoadBinary(sourceFilePath, offset);
     }
      else {
          return WarcParser.lazyLoadBinary(sourceFilePath, offset);
      }            
  }
  
  public InputStream getBinaryLazyLoadNoChucking() throws Exception{
      if (format.equals(FORMAT.ARC)) {
         BufferedInputStream is = ArcParser.lazyLoadBinary(sourceFilePath, offset);              
         InputStream maybeDechunked = maybeDechunk(is);
         //InputStream maybeUnziped = maybeUnzip(maybeDechunked);
         InputStream maybeBrotliDecoded = maybeBrotliDecode(maybeDechunked);
         
         return maybeBrotliDecoded;
      }
      else {
           InputStream is = WarcParser.lazyLoadBinary(sourceFilePath, offset);              
           InputStream maybeDechunked = maybeDechunk(is);
           //InputStream maybeUnziped = maybeUnzip(maybeDechunked);
           InputStream maybeBrotliDecoded = maybeBrotliDecode(maybeDechunked);
           return maybeBrotliDecoded;
      }            
  }
  
  
  
  public InputStream getBinaryStreamNoEncoding() throws Exception {
     
      //Chain the inputstreams in correct order.
      InputStream binaryStream = new ByteArrayInputStream(binary);      
      InputStream maybeDechunked = maybeDechunk(binaryStream);
      InputStream maybeUnziped = maybeUnzip(maybeDechunked);
      InputStream maybeBrotliDecoded = maybeBrotliDecode(maybeUnziped);
      return  maybeBrotliDecoded;      
  }
  

  /*
   * Will dechunk, unzip  Brotli decode  if required (in that order)
   */
  public String getBinaryContentAsStringUnCompressed() throws Exception{
    
      
      
    
      /*
      InputStream binaryStream = new ByteArrayInputStream(binary);           
      InputStream maybeBrotliDecoded = maybeBrotliDecode(binaryStream) ;
      InputStream maybeDechunked = maybeDechunk(maybeBrotliDecoded);
                 
//      log.info(IOUtils.toString(maybeUnziped , "UTF-8"));      
      //InputStream maybeDechunked = maybeDechunk(maybeUnziped);
 */
                 
      
     
      //Chain the inputstreams in correct order.
      InputStream binaryStream = new ByteArrayInputStream(binary);      
      InputStream maybeDechunked = maybeDechunk(binaryStream);
      InputStream maybeUnziped = maybeUnzip(maybeDechunked);
      InputStream maybeBrotliDecoded = maybeBrotliDecode(maybeUnziped);
    
      hasBeenDecompressed=true;
      String encoding = this.getContentCharset();
      if (encoding == null) {
          encoding = "UTF-8";
      }
      
      
      String uncomressed =IOUtils.toString(maybeBrotliDecoded, encoding);     
      return uncomressed;
      
     
     /*
      ((return IOUtils.toString(maybeBrotliDecoded, "UTF-8"); //READ below!                       
                          
      //IMPORTANT! If no encoding is applied , maybe replace UTF-8 with below
      /*    
        if (encoding == null || "utf-8".equals(encoding)){ // still check?
      String content = IOUtils.toString(gzipStream, "UTF-8");
    
    
      String encoding = this.getContentCharset();
      if (encoding == null || "utf-8".equals(encoding)){
        encoding ="UTF-8";   
      }
      
      log.info("creating text string from encoding:"+encoding);
      try {
      String text = new String(this.getBinary(),encoding);
      return text;
      }
      catch(Exception e){
          log.warn("Encoding error for encoding:"+encoding);          
      }
      return new String(this.getBinary()); //UNKOWN ENCODING!
      */

      
  }

  private InputStream maybeBrotliDecode(InputStream before) throws Exception{
      
   
      if ("br".equalsIgnoreCase(contentEncoding)){    
          log.info("brotli decode");
          InputStream brIs = new BrotliInputStream(before);
          this.setContentEncoding("identity");
          return brIs;                
      }
      else {
          return before;
      }                
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
  
  private InputStream maybeUnzip(InputStream before) throws Exception{
      if ("gzip".equalsIgnoreCase(contentEncoding) || "x-gzip".equalsIgnoreCase(contentEncoding)) {
          this.setContentEncoding("identity");
          log.info("uunzip");
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
   * @param id the designation/name/id of the content. Only used for logging
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
          log.debug("maybeDechunk found " + pos + " hex digits: Not a chunked stream, returning content as-is");
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
          log.info("maybeDechunk found hex digits but could not locate CRLF. Instead it found 0x" +
                   Integer.toHexString(c) + ": Not a chunked stream, returning content as-is for");
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

  
}
