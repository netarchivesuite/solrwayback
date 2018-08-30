package dk.kb.netarchivesuite.solrwayback.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {

  public static String convertWaybackDate2SolrDate(String waybackDate) throws Exception{
    
    SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");          
    Date date = waybackDateFormat.parse(waybackDate);

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //not thread safe, so create new                   
    String solrDate = dateFormat.format(date)+"Z";
  
    return solrDate;
  }
  
  public static String getSolrDate(Date date){
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //not thread safe, so create new         
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));    
    return dateFormat.format(date)+"Z";

  }

  
}
