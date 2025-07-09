package dk.kb.netarchivesuite.solrwayback.parsers;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

public class ArcHeader2WarcHeader {

  private static final Logger log = LoggerFactory.getLogger(ArcHeader2WarcHeader.class);
  private static final String newLineChar ="\r\n";
  
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
   
  public static String arcHeader2WarcHeader(ArcEntry arc){
    
    int index = arc.getHeader().indexOf(ArcParser.newLineChar);
    String arcHeaderExceptFirstLine = arc.getHeader().substring(index+ArcParser.newLineChar.length());
    int secondHeaderLength = arcHeaderExceptFirstLine.getBytes().length;
    long contentLength = secondHeaderLength + arc.getContentLength(); 
    
    // temp code below to check contentlenght for arc binary.
    String arcHeader = arc.getHeader().substring(0,index);
    String[] tokens = arcHeader.split(" ");
    int  binaryLengthHeader = Integer.parseInt(tokens[tokens.length-1]);
    
    if (binaryLengthHeader != contentLength) { //Just an observation. 
      //no need to log.
      //log.info("Arc binary content length mismatch between arc header info and actual size. Actual size:"+contentLength +": from header"+binaryLengthHeader);  
    }
            
    StringBuilder b = new StringBuilder();
    b.append("WARC/1.0"+newLineChar);
    b.append("WARC-Type: response"+newLineChar);
    b.append("WARC-Target-URI: "+arc.getUrl() +newLineChar);
    b.append("WARC-Date: "+arc.getCrawlDate() +newLineChar);        
    //WARC-Payload-Digest is optional.
    b.append("WARC-IP-Address: "+arc.getIp()+newLineChar);
    b.append("WARC-Record-ID: <urn:uuid:"+UUID.randomUUID().toString()+">"+newLineChar);    
    b.append("Content-Type: application/http; msgtype=response"+newLineChar);
    b.append("Content-Length: "+binaryLengthHeader +newLineChar); 
    b.append(newLineChar);
    //Now append the arc header, except first line
     
     b.append(arcHeaderExceptFirstLine);
     return b.toString();
  }
  
/*  
  private static String getISO8601StringForDate(Date date) {
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
      return dateFormat.format(date);
}
  */
  
}

