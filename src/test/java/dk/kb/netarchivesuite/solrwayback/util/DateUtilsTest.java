package dk.kb.netarchivesuite.solrwayback.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class DateUtilsTest {

  @Test
  public void testConvertWaybackDate2SolrDate() throws Exception{  
    String waybackDate = "20071221033234";
    String solrDate = DateUtils.convertWaybackDate2SolrDate(waybackDate);    
    assertEquals("2007-12-21T03:32:34Z", solrDate);      
  }

}
