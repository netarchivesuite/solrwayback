package dk.kb.netarchivesuite.solrwayback.util;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

public class DateUtilsTest {

  @Test
  public void testConvertWaybackDate2SolrDate() throws Exception{  
    String waybackDate = "20071221033234";
    String solrDate = DateUtils.convertWaybackDate2SolrDate(waybackDate);    
    assertEquals("2007-12-21T03:32:34Z", solrDate);      
  }
  
  @Test
  public void testGetSolrDate() throws Exception{      	 
	  String dateString = "2020-05-11 08:45:11";
	  SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");	  
	  df.setTimeZone(TimeZone.getTimeZone("UTC"));
	  Date date = df.parse(dateString);
	  String solrDate = DateUtils.getSolrDate(date);    
      assertEquals("2020-05-11T08:45:11Z", solrDate);      
  }
  
  @Test
  public void testConvertUtcDate2WaybackDate() throws Exception{  
    String dateStr = "2007-12-21T03:32:34";
    String waybackDate = DateUtils.convertUtcDate2WaybackDate(dateStr);
    
    
    assertEquals("20071221033234", waybackDate);      
  }
  

}
