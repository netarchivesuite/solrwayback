package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.interfaces.ArcSource;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry.TYPE;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import dk.kb.netarchivesuite.solrwayback.util.InputStreamUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public class WarcParser extends  ArcWarcFileParserAbstract {

  private static final Logger log = LoggerFactory.getLogger(WarcParser.class);
  private static final String newLineChar ="\r\n";
  public static String WARC_HEADER_ENCODING ="ISO-8859-1";


  /*
   *Header example(notice the two different parts):
   *WARC/1.0
   *WARC-Type: response
   *WARC-Target-URI: http://www.boerkopcykler.dk/images/low_Trance-27.5-2-LTD-_20112013_151813.jpg
   *WARC-Date: 2014-02-03T18:18:53Z
   *WARC-Payload-Digest: sha1:C4HTYCUOGJ2PCQIKSRDAOCIDMFMFAWKK
   *WARC-IP-Address: 212.97.133.94
   *WARC-Record-ID: <urn:uuid:1068b604-f3d5-40b9-8aaf-7ed0df0a20b3>
   *Content-Type: application/http; msgtype=response
   *Content-Length: 7446
   *
   *HTTP/1.1 200 OK
   *Content-Type: image/jpeg
   *Last-Modified: Wed, 20 Nov 2013 14:18:21 GMT
   *Accept-Ranges: bytes
   *ETag: "8034965dfbe5ce1:0"
   *Server: Microsoft-IIS/7.0
   *X-Powered-By: ASP.NET
   *Date: Mon, 03 Feb 2014 18:18:53 GMT
   *Connection: close
   *Content-Length: 7178
   */
  public static ArcEntry getWarcEntry(ArcSource arcSource, long warcEntryPosition, boolean loadBinary) throws Exception {

    if (arcSource.getSource().toLowerCase(Locale.ROOT).endsWith(".gz")){ //It is zipped
      return getWarcEntryZipped(arcSource, warcEntryPosition, loadBinary);
    }
    else {
      return getWarcEntryNotZipped(arcSource, warcEntryPosition, loadBinary);
    }          
  }

  public static ArcEntry getWarcEntryNotZipped(ArcSource arcSource, long warcEntryPosition,boolean loadBinary) throws Exception {

    ArcEntry warcEntry = new ArcEntry();
    warcEntry.setFormat(ArcEntry.FORMAT.WARC);
    warcEntry.setSource(arcSource);
    warcEntry.setOffset(warcEntryPosition);

    try (InputStream is = arcSource.get()) {
        InputStreamUtils.skipFully(is, warcEntryPosition);

        try (BufferedInputStream bis = new BufferedInputStream(is)) {
            loadWarcHeader(bis, warcEntry);

            //log.debug("Arc entry : totalsize:"+totalSize +" headersize:"+headerSize+" binary size:"+binarySize);
            if (loadBinary) {
                loadBinary(bis, warcEntry);
            }
        }
        return warcEntry;
    }
  }

  /*
   * Will load the header information into the warcEntry
   * The  BufferedInputStream will be returned with pointer in start of binary 
   * warcEntry will have binaryArraySize defined
   * 
   */
  private static void loadWarcHeader(BufferedInputStream bis, ArcEntry warcEntry) throws Exception{

    StringBuffer headerLinesBuffer = new StringBuffer();
    String line = readLine(bis); // First line
    headerLinesBuffer.append(line+newLineChar);

    if  (!(line.startsWith("WARC/"))) //No version check yet
    {            
      throw new IllegalArgumentException("WARC header is not WARC/'version', instead it is : "+line);
    }            

    while (!"".equals(line)) { // End of warc first header block is an empty line
      line = readLine(bis);                
      headerLinesBuffer.append(line+newLineChar);
      populateWarcFirstHeader(warcEntry, line);             

    }

    int byteCount=0; //Bytes of second header
   
    if( !(warcEntry.getType() == ArcEntry.TYPE.RESOURCE)){       
      LineAndByteCount lc =readLineCount(bis);
      line=lc.getLine();
      warcEntry.setStatus_code(getStatusCode(line));
      headerLinesBuffer.append(line+newLineChar);
      byteCount +=lc.getByteCount();                    

      while (!"".equals(line)) { // End of warc second header block is an empty line
        lc =readLineCount(bis);         
        line=lc.getLine();        
        headerLinesBuffer.append(line+newLineChar);
        byteCount +=lc.getByteCount(); 
        populateWarcSecondHeader(warcEntry, line);                                              
      }        
      warcEntry.setHeader(headerLinesBuffer.toString());
    }
    else {
      warcEntry.setHeader(""); //NONE for resource type
      warcEntry.setStatus_code(200); //fake it . Warc-indexer does the same
    }

    long totalSize= warcEntry.getWarcEntryContentLength();
    long binarySize = totalSize-byteCount;

    warcEntry.setBinaryArraySize(binarySize);
  }

  public static ArcEntry getWarcEntryZipped(ArcSource arcSource, long warcEntryPosition, boolean loadBinary) throws Exception {

    ArcEntry warcEntry = new ArcEntry();
    warcEntry.setFormat(ArcEntry.FORMAT.WARC);
    warcEntry.setSource(arcSource);
    warcEntry.setOffset(warcEntryPosition);

    try (InputStream is = arcSource.get()) {
        InputStreamUtils.skipFully(is, warcEntryPosition);

        // log.info("file is zipped:"+arcFilePath);
        try (GZIPInputStream stream = new GZIPInputStream(is);
             BufferedInputStream  bis= new BufferedInputStream(stream)) {

            loadWarcHeader(bis, warcEntry);

            //System.out.println("Arc entry : totalsize:"+totalSize +" binary size:"+binarySize +" firstHeadersize:"+byteCount);
            if (loadBinary) {
                loadBinary(bis, warcEntry);
            }
        }
    }
    return warcEntry;
      /*
          System.out.println("-------- binary start");
          System.out.println(new String(chars));
          System.out.println("-------- slut");
       */
  }


  public static BufferedInputStream lazyLoadBinary(ArcSource arcSource, long arcEntryPosition) throws Exception{
    ArcEntry arcEntry = new ArcEntry(); // We just throw away the header info anyway 

    InputStream is = arcSource.get();
    InputStreamUtils.skipFully(is, arcEntryPosition);

    if (arcSource.getSource().toLowerCase(Locale.ROOT).endsWith(".gz")){ //It is zipped
      // log.info("file is zipped:"+arcFilePath);
      GZIPInputStream zipStream = new GZIPInputStream(is);
      BufferedInputStream  bis= new BufferedInputStream(zipStream);
      loadWarcHeader(bis, arcEntry);

      BoundedInputStream maxStream = new BoundedInputStream(bis, arcEntry.getBinaryArraySize());
      return new BufferedInputStream(maxStream); // It's a mess to use nested BufferedInputStreams...

    } else {
      BufferedInputStream  bis = new BufferedInputStream(is);
      loadWarcHeader(bis, arcEntry);
      BoundedInputStream maxStream = new BoundedInputStream(bis, arcEntry.getBinaryArraySize());
      return new BufferedInputStream(maxStream);
    }

  }

  public static String getWarcLastUrlPart(String warcHeaderLine) {        
    //Example:
    //WARC-Target-URI: http://www.boerkopcykler.dk/images/low_Trance-27.5-2-LTD-_20112013_151813.jpg
    String urlPath = warcHeaderLine.substring(16); // Skip WARC-Target-URI:                     
    String paths[] = urlPath.split("/");
    String fileName = paths[paths.length - 1];
    //log.debug("file:"+fileName +" was extracted from URL:"+warcHeaderLine);
    if (fileName == null){
      fileName="";
    }
    return fileName.trim();
  }

  private static String getWarcUrl(String warcHeaderLine) {        
    //Example:
    //WARC-Target-URI: http://www.boerkopcykler.dk/images/low_Trance-27.5-2-LTD-_20112013_151813.jpg
    String urlPath = warcHeaderLine.substring(16);                      
    return urlPath.trim();
  }

  private static void populateWarcFirstHeader(ArcEntry warcEntry, String headerLine) {
    //log.debug("Parsing warc headerline(part 1):"+headerLine);                              
    if (headerLine.startsWith("WARC-Target-URI:")) {
      warcEntry.setFileName(getWarcLastUrlPart(headerLine));
      warcEntry.setUrl(getWarcUrl(headerLine));
    }    

    //Example:
    //Content-Length: 31131
    else if (headerLine.startsWith("Content-Length:")) {
      String[] contentLine = headerLine.split(" ");
      long totalSize = Long.parseLong(contentLine[1].trim());               
      warcEntry.setWarcEntryContentLength(totalSize);                       
    }       

    else if (headerLine.startsWith("WARC-Date:")) {
      String[] contentLine = headerLine.split(" ");
      String crawlDate =contentLine[1].trim();  //Zulu/UTC time   : 2020-04-28T08:17:36Z                                
      warcEntry.setCrawlDate(crawlDate);                         
      String waybackDate = DateUtils.convertUtcDate2WaybackDate(crawlDate);             
      warcEntry.setWaybackDate(waybackDate);                          
    }
    else if (headerLine.startsWith("WARC-Type:")) {
      String[] contentLine = headerLine.split(" ");
      warcEntry.setType(TYPE.valueOf(contentLine[1].trim().toUpperCase()));   // will fail if new type is found          
    }


  }

  private static void populateWarcSecondHeader(ArcEntry warcEntry, String headerLine) {
    //  log.debug("parsing warc headerline(part 2):"+headerLine);                
    //Content-Type: image/jpeg
    // or Content-Type: text/html; charset=windows-1252          
    if (headerLine.toLowerCase().startsWith("content-type:")) {            
      String[] part1 = headerLine.split(":");
      String[] part2= part1[1].split(";");                        
      warcEntry.setContentType(part2[0].trim());          
      if (part2.length == 2){
        String charset = part2[1].trim();
        if (charset.startsWith("charset=")){                                   
          String headerEncoding=charset.substring(8).replace("\"", ""); ////Some times Content-Type: text/html; charset="utf-8" instead of Content-Type: text/html; charset=utf-8
          warcEntry.setContentCharset(charset.substring(8));
        }                                   
      }


    }  //Content-Length: 31131
    else if (headerLine.toLowerCase().startsWith("content-length:")) {
      String[] contentLine = headerLine.split(" ");
      long totalSize = Long.parseLong(contentLine[1].trim());               
      warcEntry.setContentLength(totalSize);                       
    }
    else if (headerLine.toLowerCase().startsWith("content-encoding:")) {
      String[] contentLine = headerLine.split(":");               
      warcEntry.setContentEncoding(contentLine[1].trim().replace("\"", "")); //Some times Content-Type: text/html; charset="utf-8" instead of Content-Type: text/html; charset=utf-8                       
    }
    else if (headerLine.toLowerCase().startsWith("location:")) {                                      
      warcEntry.setRedirectUrl(headerLine.substring(9).trim());
    }
    else if (headerLine.toLowerCase().startsWith("transfer-encoding:")) {                                      
      String transferEncoding=headerLine.substring(18).trim();
      log.debug("transfer-encoding:"+transferEncoding);
      if (transferEncoding.equalsIgnoreCase("chunked")) {
        warcEntry.setChunked(true);
      }
    }    




  }

  public static String readLine(BufferedInputStream  bis) throws Exception{
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int current = 0; // CRLN || LN
    while ((current = bis.read()) != '\r' && current != '\n') {             
      baos.write((byte) current);  
    }
    if (current == '\r') {
      bis.read(); // line ends with 10 13        
    }

    return baos.toString(WARC_HEADER_ENCODING);
  }

  public static LineAndByteCount readLineCount(BufferedInputStream  bis) throws Exception{
    int count = 0;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    int current = 0; // CRLN || LN

    count++; //Also count linefeed
    while ((current = bis.read()) != '\r' && current != '\n') {             
      baos.write((byte)current);       
      count++;
    }
    if (current == '\r') {
      bis.read(); // line ends with 10 13
      count++;
    }       
    LineAndByteCount lc = new LineAndByteCount();
    lc.setLine(baos.toString(WARC_HEADER_ENCODING));
    lc.setByteCount(count);

    return lc;

  }




}
