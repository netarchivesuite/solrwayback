package dk.kb.netarchivesuite.solrwayback.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.solr.common.util.Pair;
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

   public static String  convertWaybackDate2SolrDate(String waybackdate) {
    
    SimpleDateFormat dForm = null;    
    DateFormat solrDateFormat =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
     //Legacy format for arc-files can have waybackdate without seconds. (before year 2003. Example: 200102050709 )
    if (waybackdate.length()== 12 ){
      dForm = new SimpleDateFormat("yyyyMMddHHmm");            
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
     * Converts the content of an Accept-DateTime Header to a waybackdate.
     * @param acceptDateTime header value.
     * @return              the waybackdate representation for the given RFC1123 date.
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc1123">RFC1123</a> for the dateformat in memento headers.
     */
  public static Long convertMementoAcceptDateTime2Waybackdate(String acceptDateTime) throws ParseException {
      DateFormat mementoFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
      mementoFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

      DateFormat waybackdateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
      waybackdateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

      Date d = mementoFormat.parse(acceptDateTime);

      String format = waybackdateFormat.format(d);
      Long waybackDate = Long.parseLong(format);
      log.info("Constructed this waybackdate from Accept-Datetime header: '{}'", format);
      return waybackDate;
  }

  public static String convertWaybackdate2Mementodate(Long waybackdate) throws ParseException {
      DateFormat waybackdateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
      waybackdateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      DateFormat mementoFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
      mementoFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

      Date d = waybackdateFormat.parse(String.valueOf(waybackdate));
      String mementoDate = mementoFormat.format(d);
      return mementoDate;
  }

  public static String convertMementodate2Solrdate(String mementoDate) throws ParseException {
      DateFormat mementoFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
      mementoFormat.setTimeZone(TimeZone.getTimeZone("GMT"));


      DateFormat solrDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //not thread safe, so create new
      solrDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

      Date d = mementoFormat.parse(mementoDate);
      String solrDate = solrDateFormat.format(d) + "Z";

      return solrDate;
  }

  public static String convertSolrdate2Mementodate(String solrDate) throws ParseException {
      DateFormat solrDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //not thread safe, so create new
      solrDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

      DateFormat mementoFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
      mementoFormat.setTimeZone(TimeZone.getTimeZone("GMT"));



      Date d = solrDateFormat.parse(solrDate);
      String mementoDate = mementoFormat.format(d);

      return mementoDate;
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
  
  /**
   * Add to the given date one time scale
   * 
   * @param date the start date
   * @param scale the time scale (YEAR, MONTH, WEEK, DAY)
   * @return the new date
   */
  public static LocalDate addScaleToDate(LocalDate date, String scale) {
      LocalDate nextDate;
      switch (scale) {
          case "MONTH" :
              nextDate = date.plusMonths(1);
              break;
          case "WEEK" :
              nextDate = date.plusWeeks(1);
              break;
          case "DAY" :
              nextDate = date.plusDays(1);
              break;
          case "YEAR" :
          default :
              nextDate = date.plusYears(1);
              break;
      }
      return nextDate;
  }
  

  /**
   * Calculate the period between start date and end date. The result is returned in the same unit
   * as the scale
   * 
   * @param start the start date
   * @param end the end date
   * @param scale the time scale (YEAR, MONTH, WEEK, DAY)
   * @return the period between start date and end date
   */
  public static int calculateBucket(LocalDate start, LocalDate end, String scale) {
      Period period = Period.between(start, end);
      int buckets = 0;
      switch (scale) {
          case "YEAR" :
              buckets = period.getYears();
              break;
          case "MONTH" :
              buckets = period.getYears() * 12 + period.getMonths();
              break;
          case "WEEK" :
              buckets = period.getYears() * 52 + period.getMonths() * 4 + period.getDays() / 7;
              break;
          case "DAY" :
          default :
              buckets = period.getYears() * 365 + period.getMonths() * 30 + period.getDays();
              break;
      }
      return buckets;
  }

  /**
   * Calculate the end date of a period to complete a calendar month or year
   * 
   * @param date the start date of the period
   * @param scale the time scale
   * @return the end date of the calendar period
   */
  public static LocalDate getEndOfFirstPeriod(LocalDate date, String scale) {
      LocalDate nextDate = DateUtils.addScaleToDate(date, scale);
      if ("MONTH".equals(scale)) {
          nextDate = YearMonth.from(date).atEndOfMonth().plusDays(1);
      } else if ("YEAR".equals(scale)) {
          nextDate = LocalDate.of(date.getYear() + 1, 1, 1);
      }
      return nextDate;
  }

  /**
   * Calculate all the periods between start date and end date
   * 
   * @param start the start date
   * @param end the end date
   * @param scale the time scale
   * @return a list of pair of localdate (start of period, end of period)
   */
  public static List<Pair<LocalDate, LocalDate>> calculatePeriods(LocalDate start, LocalDate end, String scale){
      List<Pair<LocalDate, LocalDate>> listPeriods = new ArrayList<>();
      LocalDate startPeriod = start;
      LocalDate endPeriod = getEndOfFirstPeriod(start, scale);
      boolean endOfPeriod = false;
      while (!endOfPeriod) {
          if (endPeriod.isAfter(end)) {
              endPeriod = end;
              endOfPeriod = true;
              listPeriods.add(new Pair<>(startPeriod, endPeriod));
          } else {
              listPeriods.add(new Pair<>(startPeriod, endPeriod.minusDays(1)));
          }
          startPeriod = endPeriod;
          endPeriod = addScaleToDate(endPeriod, scale);
      }
      return listPeriods;
  }

}
