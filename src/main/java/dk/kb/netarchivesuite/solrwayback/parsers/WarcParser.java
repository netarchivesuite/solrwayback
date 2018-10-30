package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class WarcParser {

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
    public static ArcEntry getWarcEntry(String warcFilePath, long warcEntryPosition) throws Exception {
        if (warcFilePath.endsWith(".gz")) { //It is zipped
            return getWarcEntryZipped(warcFilePath, warcEntryPosition);
        }
    
        try( RandomAccessFile raf = new RandomAccessFile(new File(warcFilePath), "r");){
          ArcEntry warcEntry = new ArcEntry();
            List<String> headerLinesBuffer = new ArrayList<>();
            
            raf.seek(warcEntryPosition);
            
            String line = raf.readLine(); // First line
            headerLinesBuffer.add(line);
            
            if  (!(line.startsWith("WARC/"))) //No version check yet
            {            
                throw new IllegalArgumentException("WARC header is not WARC/'version', instead it is : "+line);
            }            
            
            while (!"".equals(line)) { // End of warc first header block is an empty line                
                line = raf.readLine();
                headerLinesBuffer.add(line);
                populateWarcFirstHeader(warcEntry, line);
            }
    
            //Now we need to count bytes to ensure that we do not read to far.
            int byteCount=0; //Bytes of second header
            
            do {
                LineAndByteCount lc =readLineCount(raf);
                line=lc.getLine();
                headerLinesBuffer.add(line);
                byteCount +=lc.getByteCount();
                populateWarcSecondHeader(warcEntry, line);
                if (byteCount >= (int) warcEntry.getWarcEntryContentLength()){
                    //After second header, we have an empty line, and then the contents
                    //And after contents, we have two empty lines and then the next entry
                    //But if there is no content, we are about to read into the two empty lines here.
                    //So if we get to the end of the content as noted, stop and do not read the empty line
                    break;
                }
    
            } while (!"".equals(line)); // End of warc second header block is an empty line
    
            int totalSize= (int) warcEntry.getWarcEntryContentLength();
            int binarySize = totalSize-byteCount;
    
            //log.debug("Warc entry : totalsize:{} headersize:{} binary size:{}",totalSize,byteCount,binarySize);

            byte[] chars = new byte[binarySize];
            IOUtils.readFully(Channels.newInputStream(raf.getChannel()), chars);
    
            warcEntry.setBinary(chars);
            warcEntry.setHeader(String.join(newLineChar,headerLinesBuffer));
            return warcEntry;
        }
    }

    
    public static ArcEntry getWarcEntryZipped(String warcFilePath, long warcEntryPosition) throws Exception {
      try (RandomAccessFile raf = new RandomAccessFile(new File(warcFilePath), "r");) {
          List<String> headerLinesBuffer = new ArrayList<>();
          ArcEntry warcEntry = new ArcEntry();
    
          raf.seek(warcEntryPosition);
    
          try (DataInputStream bis = new DataInputStream(new GZIPInputStream(Channels.newInputStream(raf.getChannel())))) {
        
              String line = readLine(bis); // First line
              headerLinesBuffer.add(line);
        
              if (!(line.startsWith("WARC/"))) //No version check yet
              {
                  throw new IllegalArgumentException("WARC header is not WARC/'version', instead it is : " + line);
              }
        
              while (!"".equals(line)) { // End of warc first header block is an empty line
                  line = readLine(bis);
                  headerLinesBuffer.add(line);
                  populateWarcFirstHeader(warcEntry, line);
            
              }
        
              int byteCount = 0; //Bytes of second header
        
              LineAndByteCount lc = readLineCount(bis);
              line = lc.getLine();
              headerLinesBuffer.add(line);
              byteCount += lc.getByteCount();
        
              while (!"".equals(line)) { // End of warc second header block is an empty line
            
                  lc = readLineCount(bis);
                  line = lc.getLine();
                  headerLinesBuffer.add(line);
                  byteCount += lc.getByteCount();
            
                  populateWarcSecondHeader(warcEntry, line);
            
              }
        
              int totalSize = (int) warcEntry.getWarcEntryContentLength();
              int binarySize = totalSize - byteCount;
        
        
              byte[] chars = new byte[binarySize];
              IOUtils.readFully(bis, chars);
        
              warcEntry.setBinary(chars);
              warcEntry.setHeader(String.join(newLineChar,headerLinesBuffer));
              return warcEntry;
          }
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
        return urlPath;
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
            int totalSize = Integer.parseInt(contentLine[1].trim());               
            warcEntry.setWarcEntryContentLength(totalSize);                       
        }       
        
        else if (headerLine.startsWith("WARC-Date:")) {
            String[] contentLine = headerLine.split(" ");
             String crawlDate =contentLine[1].trim();  //Zulu time                                      
             warcEntry.setCrawlDate(crawlDate);
                                        
             Instant instant = Instant.parse (crawlDate);  //JAVA 8
             Date date = java.util.Date.from( instant );
             String waybackDate = date2waybackdate(date);             
             warcEntry.setWaybackDate(waybackDate);                          
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
                   warcEntry.setContentEncoding(headerEncoding);                                      
                 }                                   
               }
               
               
          }  //Content-Length: 31131
          else if (headerLine.toLowerCase().startsWith("content-length:")) {
            String[] contentLine = headerLine.split(" ");
              int totalSize = Integer.parseInt(contentLine[1].trim());               
              warcEntry.setContentLength(totalSize);                       
          }
          else if (headerLine.toLowerCase().startsWith("content-encoding:")) {
            String[] contentLine = headerLine.split(":");               
            warcEntry.setContentEncoding(contentLine[1].trim().replace("\"", "")); //Some times Content-Type: text/html; charset="utf-8" instead of Content-Type: text/html; charset=utf-8                       
          }      
      }
     
     public static String readLine(DataInput  bis) throws Exception{
        return readLineCount(bis).getLine();
     }
    
    public static LineAndByteCount readLineCount(DataInput bis) throws Exception {
        int count = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        byte current = 0; // CRLN || LN
        
        final byte r = '\r';
        final byte n = '\n';
        while ((current = bis.readByte()) != r && current != n) {
            baos.write(current);
            count++;
        }
        count++; //Also count linefeed
        if (current == r) {
            bis.readByte(); // line ends with 10 13
            count++;
        }
        LineAndByteCount lc = new LineAndByteCount();
        lc.setLine(baos.toString(WARC_HEADER_ENCODING));
        lc.setByteCount(count);
        
        return lc;
        
    }
    
     
     
     //TODO move to util class
       public static String  date2waybackdate(Date date) { 
       SimpleDateFormat dForm = new SimpleDateFormat("yyyyMMddHHmmss");        
       try {
       String waybackDate = dForm.format(date);
       return waybackDate;                              
       } 
       catch(Exception e){        
       log.error("Could not parse date:"+date,e);
       return "20170101010101"; //Default, should never happen.
       }
   }
    
}
