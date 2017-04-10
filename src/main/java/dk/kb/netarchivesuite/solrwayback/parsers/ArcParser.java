package dk.kb.netarchivesuite.solrwayback.parsers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;


public class ArcParser {

    private static final Logger log = LoggerFactory.getLogger(ArcParser.class);
    
    /*
    */
    
    /*
     *Header example:
     *http://www.radionyt.dk/forum/Default.asp?mode=message&Id=10846&ForumId=31 86.58.185.215 20090610094553 text/html 35257
     *HTTP/1.1 200 OK
     *Cache-Control: private
     *Connection: close
     *Date: Wed, 10 Jun 2009 09:45:54 GMT
     *Content-Length: 35025
     *Content-Type: text/html
     *Server: Microsoft-IIS/6.0
     *MicrosoftOfficeWebServer: 5.0_Pub
     *X-Powered-By: ASP.NET
     */
    public static ArcEntry getArcEntry(String arcFilePath, long arcEntryPosition) throws Exception {
        RandomAccessFile raf= null ;
        try{
       
          if (arcFilePath.endsWith(".gz")){ //It is zipped
            return getArcEntryZipped(arcFilePath, arcEntryPosition);                       
         }
            ArcEntry arcEntry = new ArcEntry();

            raf = new RandomAccessFile(new File(arcFilePath), "r");
            raf.seek(arcEntryPosition);

            String line = raf.readLine(); // First line
        

            if  (!(line.startsWith("http"))) //No version check yet
            {            
                throw new IllegalArgumentException("ARC header does not start with http : "+line);
            }         
                        
            arcEntry.setFileName(getArcLastUrlPart(line));            
            arcEntry.setCrawlDate(getCrawlDate(line));
            arcEntry.setUrl(getArcUrl(line));
            
            long afterFirst = raf.getFilePointer();

            String[] split = line.split(" ");
            int totalSize = Integer.parseInt(split[split.length - 1]);
            while (!"".equals(line)) { // End of header block is an empty line
                line = raf.readLine();
                populateArcHeader(arcEntry, line);
            }
            // Load the binary blog. We are now right after the header. Rest will be the binary
            long headerSize = raf.getFilePointer() - afterFirst;
            long binarySize = totalSize - headerSize;
            
            //log.debug("Arc entry : totalsize:"+totalSize +" headersize:"+headerSize+" binary size:"+binarySize);            
            byte[] bytes = new byte[(int) binarySize];
            raf.read(bytes);

            arcEntry.setBinary(bytes);
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

    
    public static ArcEntry getArcEntryZipped(String arcFilePath, long arcEntryPosition) throws Exception {
      RandomAccessFile raf=null;
      try{
            ArcEntry arcEntry = new ArcEntry();
            raf = new RandomAccessFile(new File(arcFilePath), "r");
            raf.seek(arcEntryPosition);
          
            log.info("file is zipped:"+arcFilePath);
            InputStream is = Channels.newInputStream(raf.getChannel());                           
           
     
            GZIPInputStream stream = new GZIPInputStream(is);             
           
            BufferedInputStream  bis= new BufferedInputStream(stream);
   
          String line = readLine(bis); // First line
          
          if  (!(line.startsWith("http"))) //No version check yet
          {            
            throw new IllegalArgumentException("ARC header does not start with http : "+line);
          }            
          
          arcEntry.setFileName(getArcLastUrlPart(line));            
          arcEntry.setCrawlDate(getCrawlDate(line));
          arcEntry.setUrl(getArcUrl(line));
                    
        
          
          
          String[] split = line.split(" ");
          int totalSize = Integer.parseInt(split[split.length - 1]);
          
          int byteCount=0; //Bytes of second header
          
          LineAndByteCount lc =readLineCount(bis);
          line=lc.getLine();
          byteCount +=lc.getByteCount();                    

          while (!"".equals(line)) { // End of warc second header block is an empty line
              
            lc =readLineCount(bis);
            line=lc.getLine();
            byteCount +=lc.getByteCount();                    
            
              populateArcHeader(arcEntry, line);
            //  System.out.println("parsing headerline:"+line);
          }
          
         
          
          int binarySize = totalSize-byteCount;                                                   
          //System.out.println("Arc entry : totalsize:"+totalSize +" binary size:"+binarySize +" firstHeadersize:"+byteCount);          
          byte[] chars = new byte[binarySize];           
          bis.read(chars);
          
          raf.close();
          bis.close();
          arcEntry.setBinary(chars); 
/*
          System.out.println("-------- binary start");
          System.out.println(new String(chars));
          System.out.println("-------- slut");
*/

          return arcEntry;
      }
      catch(Exception e){
          throw e;
      }
      finally {
          if (raf!= null){
              raf.close();
           }
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
        return fullUrl;
    }

    
    private static String getCrawlDate(String arcHeaderLine) throws Exception {
    	SimpleDateFormat dForm = new SimpleDateFormat("yyyyMMddHHmmss");    
        DateFormat solrDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");    

    	try {
        String[] split = arcHeaderLine.split(" ");
        String crawlDateTime = split[2];
    	Date d = dForm.parse(crawlDateTime);
    	String format = solrDateFormat.format(d);
    	
    	return format+"Z";         
    	} 
    	catch(Exception e){
    		log.error("error Parsing crawlDate from headline:"+arcHeaderLine);
    		throw new RuntimeException("Could not parse timestamp from '" + arcHeaderLine+"'",e);
    	}
    }
    
    private static void populateArcHeader(ArcEntry arcEntry, String headerLine) {
        if (headerLine.toLowerCase().startsWith("content-length:")) {
            String[] split = headerLine.split(":");
            arcEntry.setContentLength(Integer.parseInt(split[1].trim()));
        } else if (headerLine.toLowerCase().startsWith("content-type:")) {
            //text/html; charset=
            String[] part1 = headerLine.split(":");
            String[] part2= part1[1].split(";");                                  
           arcEntry.setContentType(part2[0].trim());
        }
       
        
    }
}
