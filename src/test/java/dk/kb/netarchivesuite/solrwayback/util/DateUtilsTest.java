package dk.kb.netarchivesuite.solrwayback.util;

import static org.junit.Assert.*;

import java.text.ParseException;
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

    @Test
    public void testMemento2waybackDate() throws ParseException {
      String mementoDate = "Thu, 23 Mar 2023 14:05:57 GMT";
      Long waybackDate = DateUtils.convertMementoAcceptDateTime2Waybackdate(mementoDate);

      Long correctDate= 20230323140557L;
      assertEquals(correctDate, waybackDate);
    }


    @Test
    public void testAnotherMemento2waybackDate() throws ParseException {
        String mementoDate = "Thu, 01 Mar 2002 14:05:57 GMT";
        Long waybackDate = DateUtils.convertMementoAcceptDateTime2Waybackdate(mementoDate);

        Long correctDate= 20020301140557L;
        assertEquals(correctDate, waybackDate);
    }
    @Test
    public void testWaybackDate2MementoDate() throws ParseException {
      Long waybackDate = 20230323140557L;
      String correctMementoDate = "Thu, 23 Mar 2023 14:05:57 GMT";

      String convertedDate = DateUtils.convertWaybackdate2Mementodate(waybackDate);
      assertEquals(correctMementoDate, convertedDate);
    }

    @Test
    public void testMemento2Solr() throws ParseException {
      String mementoDate = "Thu, 01 Mar 2002 14:05:57 GMT";
      String solrDate = DateUtils.convertMementodate2Solrdate(mementoDate);

      assertEquals("2002-03-01T14:05:57Z", solrDate);
    }

    @Test
    public void testSolr2Memento() throws ParseException {
        String solrDate = "1998-04-06T17:06:35Z";
        String mementoDate = DateUtils.convertSolrdate2Mementodate(solrDate);

        assertEquals("Mon, 06 Apr 1998 17:06:35 GMT", mementoDate);
    }


  

}
