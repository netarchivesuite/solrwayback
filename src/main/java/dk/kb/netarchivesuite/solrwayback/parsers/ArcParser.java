package dk.kb.netarchivesuite.solrwayback.parsers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;

public class ArcParser extends  ArcWarcFileParserAbstract{

  private static final Logger log = LoggerFactory.getLogger(ArcParser.class);
  public static final String newLineChar ="\r\n"; //This is warc header ending, but the header is also only used for display or warc export
  public static String ARC_HEADER_ENCODING ="ISO-8859-1";
  /*
   *Header example:
   *http://www.radionyt.dk/forum/Default.asp?mode=message&Id=10846&ForumId=31 86.58.185.215 20090610094553 text/html 35257
   *HTTP/1.1 200 OK
   *Cache-Control: private
   *Connection: close
   *Date: Wed, 10 Jun 2009 09:45:54 GMT
   *Content-Length: 35025
   *Content-Type: text/html
   *Content-Encoding: gzip 
   *Server: Microsoft-IIS/6.0
   *MicrosoftOfficeWebServer: 5.0_Pub
   *X-Powered-By: ASP.NET
   */
  public static ArcEntry getArcEntry(String arcFilePath, long arcEntryPosition, boolean loadBinary) throws Exception {
    RandomAccessFile raf= null ;
    try{

      if (arcFilePath.endsWith(".gz")){ //It is zipped
        return getArcEntryZipped(arcFilePath, arcEntryPosition, loadBinary);                       
      }
      else {
          return getArcEntryNotZipped(arcFilePath, arcEntryPosition, loadBinary);
      }      
    }
    catch(Exception e){
      throw e;
    }
    finally{
      if (raf!= null){
        raf.close();
      }
    }
  }

  
  public static ArcEntry getArcEntryNotZipped(String arcFilePath, long arcEntryPosition, boolean loadBinary) throws Exception {
      RandomAccessFile raf= null ;
      ArcEntry arcEntry = new ArcEntry();
      arcEntry.setFormat(ArcEntry.FORMAT.ARC);
      arcEntry.setSourceFilePath(arcFilePath);
      arcEntry.setOffset(arcEntryPosition);
      
      try{        
      
        raf = new RandomAccessFile(new File(arcFilePath), "r");
        raf.seek(arcEntryPosition);

        loadArcHeaderNotZipped(raf, arcEntry);
        
        long binarySize=arcEntry.getBinaryArraySize();
        
        if (loadBinary) {        
        //log.debug("Arc entry : totalsize:"+totalSize +" headersize:"+headerSize+" binary size:"+binarySize);            
          byte[] bytes = new byte[(int) binarySize];
          raf.read(bytes);
          arcEntry.setBinary(bytes);
        }
                        
        return arcEntry;
      }
      catch(Exception e){
        throw e;
      }
      finally{
        if (raf!= null){
          raf.close();
        }
      }      
      
  }

  public static ArcEntry getArcEntryZipped(String arcFilePath, long arcEntryPosition, boolean loadBinary) throws Exception {

    
    ArcEntry arcEntry = new ArcEntry();
    arcEntry.setFormat(ArcEntry.FORMAT.ARC);
    arcEntry.setSourceFilePath(arcFilePath);
    arcEntry.setOffset(arcEntryPosition);
    
      try (RandomAccessFile raf = new RandomAccessFile(new File(arcFilePath), "r")){ 
      
      raf.seek(arcEntryPosition);          

      // log.info("file is zipped:"+arcFilePath);
      InputStream is = Channels.newInputStream(raf.getChannel());                           
      GZIPInputStream stream = new GZIPInputStream(is);             
      BufferedInputStream  bis= new BufferedInputStream(stream);

      loadArcHeaderZipped(bis, arcEntry);
      
      //System.out.println("Arc entry : totalsize:"+totalSize +" binary size:"+binarySize +" firstHeadersize:"+byteCount);          
      long binarySize = arcEntry.getBinaryArraySize();
       if (loadBinary) {
        byte[] chars = new byte[(int)binarySize];           
        
        bis.read(chars);
        arcEntry.setBinary(chars);
      }      
      raf.close();
      bis.close();
      
      
      return arcEntry;
    }
    catch(Exception e){
      throw e;
    }    
  }

  
  
  
  /*
   * Will load the header information into the warcEntry
   * The  BufferedInputStream will be returned with pointer in start of binary 
   * warcEntry will have binaryArraySize defined
   * 
   */
  private static void loadArcHeaderZipped( BufferedInputStream bis, ArcEntry arcEntry) throws Exception{
      
    StringBuffer headerLinesBuffer = new StringBuffer(); 

 
    String line = readLine(bis); // First line
    headerLinesBuffer.append(line+newLineChar);

    if  (!(line.startsWith("http"))) //No version check yet
    {            
      throw new IllegalArgumentException("ARC header does not start with http : "+line);
    }            
    
    arcEntry.setFileName(getArcLastUrlPart(line));                    
    String waybackDate = getWaybackDate(line);                    
    arcEntry.setCrawlDate(DateUtils.convertWaybackDate2SolrDate(waybackDate));          
    arcEntry.setWaybackDate(waybackDate);
    arcEntry.setUrl(getArcUrl(line));
    arcEntry.setIp(getIp(line));            

    String[] split = line.split(" ");
    int totalSize = Integer.parseInt(split[split.length - 1]);

    int byteCount=0; //Bytes of second header

    LineAndByteCount lc =readLineCount(bis);
    line=lc.getLine();
    arcEntry.setStatus_code(getStatusCode(line));
    
    headerLinesBuffer.append(line+newLineChar);
    byteCount +=lc.getByteCount();                    

    while (!"".equals(line)) { // End of warc second header block is an empty line              
      lc =readLineCount(bis);
      line=lc.getLine();
      headerLinesBuffer.append(line+newLineChar);
      byteCount +=lc.getByteCount();                    

      populateArcHeader(arcEntry, line);        
    }
    arcEntry.setHeader(headerLinesBuffer.toString());
    
    int binarySize = totalSize-byteCount;                                                   
    arcEntry.setContentLength(binarySize); //trust the load, not the http-header for arc-files
    arcEntry.setBinaryArraySize(binarySize);      
  }
  
  /*
   * Will load the header information into the warcEntry
   * The  RandomAccessFile will be returned with pointer in start of binary 
   * warcEntry will have binaryArraySize defined
   * 
   */
  private static void loadArcHeaderNotZipped(RandomAccessFile raf, ArcEntry arcEntry) throws Exception{
      
      StringBuffer headerLinesBuffer = new StringBuffer();
      
      String line = raf.readLine(); // First line
      headerLinesBuffer.append(line+newLineChar);
      
      if  (!(line.startsWith("http"))) //No version check yet
      {            
        throw new IllegalArgumentException("ARC header does not start with http : "+line);
      }         

      arcEntry.setFileName(getArcLastUrlPart(line));            

      String waybackDate = getWaybackDate(line);                    
      arcEntry.setCrawlDate(DateUtils.convertWaybackDate2SolrDate(waybackDate));          
      arcEntry.setWaybackDate(waybackDate);                       
      arcEntry.setUrl(getArcUrl(line));
      arcEntry.setIp(getIp(line));

      long afterFirst = raf.getFilePointer();

      String[] split = line.split(" ");
      int totalSize = Integer.parseInt(split[split.length - 1]);
      line = raf.readLine(); // second line http+status: HTTP/1.1 302 Found
      arcEntry.setStatus_code(getStatusCode(line));      
      while (!"".equals(line)) { // End of header block is an empty line
      
        line = raf.readLine();
        headerLinesBuffer.append(line+newLineChar);
        populateArcHeader(arcEntry, line);
      }
      
      arcEntry.setHeader(headerLinesBuffer.toString());
      
      // Load the binary blog. We are now right after the header. Rest will be the binary
      long headerSize = raf.getFilePointer() - afterFirst;
      long binarySize = totalSize - headerSize;
      arcEntry.setContentLength(binarySize);  //trust the load, not the http-header for arc-files

      arcEntry.setBinaryArraySize(binarySize);
      
  }
  

  public static BufferedInputStream lazyLoadBinary(String arcFilePath, long arcEntryPosition) throws Exception{
      ArcEntry arcEntry = new ArcEntry(); // We just throw away the header info anyway 
      
      if (arcFilePath.endsWith(".gz")){ //It is zipped
          
          RandomAccessFile raf = new RandomAccessFile(new File(arcFilePath), "r");
          raf.seek(arcEntryPosition);          

          // log.info("file is zipped:"+arcFilePath);
          InputStream is = Channels.newInputStream(raf.getChannel());                           
          GZIPInputStream zipStream = new GZIPInputStream(is);
          BufferedInputStream  bis= new BufferedInputStream(zipStream);
          loadArcHeaderZipped(bis, arcEntry);
          
          BoundedInputStream maxStream = new BoundedInputStream(bis, arcEntry.getBinaryArraySize());
          return new BufferedInputStream(maxStream); // It's a mess to use nested BufferedInputStreams...
          
        }
        else {
            RandomAccessFile raf = new RandomAccessFile(new File(arcFilePath), "r");
            raf.seek(arcEntryPosition);
            loadArcHeaderNotZipped(raf, arcEntry);
            InputStream is = Channels.newInputStream(raf.getChannel());
            BoundedInputStream maxStream = new BoundedInputStream(is, arcEntry.getBinaryArraySize());
            BufferedInputStream bis = new BufferedInputStream(maxStream);
            return bis;
        }            
      
  }
  
  public static String readLine(BufferedInputStream  bis) throws Exception{
    StringBuffer buf = new StringBuffer();
    int current = 0; // CRLN || LN
    while ((current = bis.read()) != '\r' && current != '\n') {             
      buf.append((char) current);
    }
    if (current == '\r') {
      bis.read(); // line ends with 10 13        
    }


    return buf.toString();

  }

  public static LineAndByteCount readLineCount(BufferedInputStream  bis) throws Exception{
    int count = 0;
    StringBuffer buf = new StringBuffer();
    int current = 0; // CRLN || LN

    count++; //Also count linefeed
    while ((current = bis.read()) != '\r' && current != '\n') {             
      buf.append((char) current);
      count++;
    }
    if (current == '\r') {
      bis.read(); // line ends with 10 13
      count++;
    }       
    LineAndByteCount lc = new LineAndByteCount();
    lc.setLine(buf.toString());
    lc.setByteCount(count);

    return lc;

  }


  private static String getArcLastUrlPart(String arcHeaderLine) {
    String[] split = arcHeaderLine.split(" ");
    String fullUrl = split[0];
    String paths[] = fullUrl.split("/");
    String fileName = paths[paths.length - 1];
    if (fileName == null){
      fileName="";
    }
    return fileName.trim();
  }


  private static String getArcUrl(String arcHeaderLine) {
    String[] split = arcHeaderLine.split(" ");
    String fullUrl = split[0];
    return fullUrl.trim();
  }

  private static String getIp(String arcHeaderLine) {
    String[] split = arcHeaderLine.split(" ");
    return split[1];
  }

  private static String getWaybackDate(String arcHeaderLine) throws Exception {
    String[] split = arcHeaderLine.split(" ");
    return split[2];                                
  }

  private static void populateArcHeader(ArcEntry arcEntry, String headerLine) {
    if (headerLine.toLowerCase().startsWith("content-length:")) {      
      String[] split = headerLine.split(":");
      //arcEntry.setContentLength(Integer.parseInt(split[1].trim())); //Dont trust server. Use binary size.        
    } else if (headerLine.toLowerCase().startsWith("content-type:")) {
      //text/html; charset=
      String[] part1 = headerLine.split(":");
      String[] part2= part1[1].split(";");                                  
      arcEntry.setContentType(part2[0].trim());
      
      if (part2.length == 2){
        String charsetString = part2[1].trim();
        if (charsetString.toLowerCase().startsWith("charset=")){
          String  charset= charsetString.substring(8).replace("\"", "");
          arcEntry.setContentCharset( charset);
        }                    
      }
      
    }
    else if (headerLine.toLowerCase().startsWith("content-encoding:")) {
      //text/html; charset=
      String[] contentHeader = headerLine.split(":");                                  
      
      arcEntry.setContentEncoding(contentHeader[1].trim());
      //log.info("setting content encoding:"+contentHeader[1].trim());
    }
    else if (headerLine.toLowerCase().startsWith("location:")) {                                      
      arcEntry.setRedirectUrl(headerLine.substring(9).trim());
    }
    else if (headerLine.toLowerCase().startsWith("transfer-encoding:")) {                                      
        String transferEncoding=headerLine.substring(18).trim();
        log.debug("transfer-encoding:"+transferEncoding);
        if (transferEncoding.equalsIgnoreCase("chunked")) {
            arcEntry.setChunked(true);
         }
      }    
    
    
  }  
  
}
