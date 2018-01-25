package dk.kb.netarchivesuite.solrwayback.smurf;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import dk.kb.netarchivesuite.solrwayback.service.dto.smurf.SmurfYearBuckets;
import dk.kb.netarchivesuite.solrwayback.service.dto.smurf.YearCount;

public class SmurfUtil {
    
  public static  SmurfYearBuckets generateYearBuckets(HashMap<Integer, Long> yearFacetsQuery,  HashMap<Integer, Long>  yearFacetsAll , int baseYear , Integer endyear){
    
    ArrayList<Double> yearCountPercent = new  ArrayList<Double>();  
    ArrayList<YearCount> yearCountTotal= new ArrayList<YearCount>();

    int maxyear = Calendar.getInstance().get(Calendar.YEAR);
    if(endyear != null){
      maxyear=endyear;
    }
    
    
    boolean anyResults=false;
    
    for (int year = baseYear ;year<=maxyear; year++){
      Long countAll = yearFacetsAll.get(year); 
      Long countQuery = yearFacetsQuery.get(year);
      YearCount yearCount= new YearCount();
      if (countQuery==null){
        countQuery = 0L;
      }else{
        anyResults=true;
      }        
      
      if (countAll== null){
        countAll=0L; // handle divide by zero        
        yearCountPercent.add(0d);
      }
      else{
        double percent = divide(countQuery,countAll);
        yearCountPercent.add(percent);
      }
                                
      yearCount.setYear(year);
      yearCount.setCount(countQuery);
      yearCount.setTotal(countAll);
      yearCountTotal.add(yearCount);         
    
    }

    SmurfYearBuckets buckets = new SmurfYearBuckets();
    buckets.setBaseYear(baseYear);
    buckets.setYearCountPercent(yearCountPercent);
    buckets.setYearCountsTotal(yearCountTotal);
    buckets.setEmptyResult(!anyResults);
        
    return buckets;        
  }
  
  
  private static double divide(long l1,long l2){
    NumberFormat nf = DecimalFormat.getInstance(Locale.ENGLISH);
    DecimalFormat df = (DecimalFormat)nf;
    df.applyPattern("#.##########");   
    double percent=  (double) l1  /  (double) l2;     
    return Double.parseDouble(df.format(percent));    
  }
  

}
