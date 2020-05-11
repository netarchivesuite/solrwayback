package dk.kb.netarchivesuite.solrwayback.service.dto;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.IOUtils;
import org.brotli.dec.BrotliInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Notice this class is returned both by the ArcParser and WarcParser.
//Could not think of a good common name...

@XmlRootElement
public class ArcEntry {

  private static final Logger log = LoggerFactory.getLogger(ArcEntry.class);
  
  private boolean hasBeenDecompressed=false;
  private byte[] binary;
  private int status_code;
  private String header;//Both headers for WARC.
  private String ip;
  private String url;
  private String contentCharset;
  private long contentLength;
  private long warcEntryContentLength; //From warc header#1. This does not exist for arc files
  private String contentType; //As returned by the webserver when harvested
  private String contentTypeExt; //As returned by the webserver when harvested
  private String fileName;
  private String crawlDate; // format 2009-12-09T05:32:50Z
  private String contentEncoding;
  private String waybackDate; // format 20080331193532
  private String redirectUrl; //null if not redirect
  
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
  public void setUrl(String url) {
    this.url = url;
  }
  public String getContentEncoding() {
    return contentEncoding;
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
  /*
   * Will decompres if gzip.
   */
  public String getBinaryContentAsStringUnCompressed() throws Exception{
    if ("br".equalsIgnoreCase(contentEncoding)){
     log.warn("br (brotli) encoding not supported");    
     InputStream brIs = new BrotliInputStream(new ByteArrayInputStream(binary));
     String content = IOUtils.toString(brIs, "UTF-8");
     return content;      
    }
    
    else if ("gzip".equalsIgnoreCase(contentEncoding) || "x-gzip".equalsIgnoreCase(contentEncoding)){
      log.info("gzip detected, decompressing");      
      GZIPInputStream gzipStream = new GZIPInputStream (new ByteArrayInputStream(binary));                       
      String content = IOUtils.toString(gzipStream, "UTF-8");      
      return content;      
    }
    else{
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
      

    }  
  }

}
