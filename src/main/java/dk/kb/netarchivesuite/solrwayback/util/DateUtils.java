package dk.kb.netarchivesuite.solrwayback.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateUtils {

  private static final Logger log = LoggerFactory.getLogger(DateUtils .class);

  // dateSecond and dateMillisecond are used by solrTimestampToJavaDate
  private static final DateFormat dateSecond = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  private static final DateFormat dateMillisecond = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'");
  static {
    dateSecond.setTimeZone(TimeZone.getTimeZone("UTC"));
    dateMillisecond.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

   public static String  convertWaybackDate2SolrDate(String waybackdate) throws Exception {
    
    SimpleDateFormat dForm = null;    
    DateFormat solrDateFormat =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
     //Legacy format for arc-files can have waybackdate without seconds. (before year 2003. Example: 200102050709 )
    if (waybackdate.length()== 12 ){
      dForm = new SimpleDateFormat("yyyyMMddHHmm");            
      log.debug("legacy arc waybackdate:"+waybackdate);
    }
    else{
       dForm = new SimpleDateFormat("yyyyMMddHHmmss");    
    }            
      try {

      Date d = dForm.parse(waybackdate);
      String format = solrDateFormat.format(d);                
      return format+"Z";         
      } 
      catch(Exception e){        
          throw new RuntimeException("Could not parse waybackdate from:"+waybackdate,e);
      }
  }
  
  
    /**
     * @return Solr formatted timestamp with second precision.
     */
  public static String getSolrDate(Date date){
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //not thread safe, so create new         
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));    
    return dateFormat.format(date)+"Z";
  }

    /**
     * @return Solr formatted timestamp with millisecond precision.
     */
  public static String getSolrDateFull(Date date){
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); //not thread safe, so create new
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormat.format(date)+"Z";
  }

  public static String convertUtcDate2WaybackDate(String solrDate) throws RuntimeException{
	  try {  
	  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //not thread safe, so create new         
	    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	    Date date = dateFormat.parse(solrDate);	    
	    final String NEW_FORMAT = "yyyyMMddHHmmss";
	    dateFormat.applyPattern(NEW_FORMAT);	   	   
	    return dateFormat.format(date);
	  }
      catch(Exception e) {
	    throw new RuntimeException("Error parsing UTC date:"+solrDate,e);	
      }
  }

    /**
     * Converts {@code 2022-10-24T09:53:00Z} or {@code 2022-10-24T09:53:00.000Z} to Java Date.
     * @return Java Date from Solr ISO-Date.
     */
  public static synchronized Date solrTimestampToJavaDate(String solrDate) {
      try {
          return dateMillisecond.parse(solrDate);
      } catch (ParseException e) {
          try {
              return dateSecond.parse(solrDate);
          } catch (ParseException ex) {
              throw new RuntimeException("Unable to parse '" + solrDate + "' as a Solr ISO timestamp");
          }
      }
  }

}
