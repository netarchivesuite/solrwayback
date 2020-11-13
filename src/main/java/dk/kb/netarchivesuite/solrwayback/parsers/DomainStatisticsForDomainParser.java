package dk.kb.netarchivesuite.solrwayback.parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

import dk.kb.netarchivesuite.solrwayback.solr.FacetCount;
import dk.kb.netarchivesuite.solrwayback.util.JsonUtils;

public class DomainStatisticsForDomainParser {

  
  
  public static HashMap<Integer,List<FacetCount>> parseDomainStatisticsJson(String jsonString){       
    HashMap<Integer,List<FacetCount>> yearFacetDomainCountMap = new HashMap<Integer,List<FacetCount>>();
    
    
    JSONObject json = new JSONObject(jsonString);
    JSONObject jsonFacets = JsonUtils.getSubObjectIfExists(json, "facets");
    long totalCount = jsonFacets.getLong("count"); //Total summation over all year of all results.    
    JSONObject jsonDomains= JsonUtils.getSubObjectIfExists(jsonFacets, "domains");
    
       
    JSONArray buckets = jsonDomains.getJSONArray("buckets");
    
    for (int i = 0;i<buckets.length();i++) {
      JSONObject bucketArray = (   JSONObject) buckets.get(i);                
      String domain = bucketArray.get("val").toString();        
      JSONObject years = JsonUtils.getSubObjectIfExists(bucketArray, "years");
      JSONArray  yearBuckets = years.getJSONArray("buckets");
    
      for (int j =0;j<yearBuckets.length();j++) {
        JSONObject pair = (   JSONObject) yearBuckets.get(j);
        int year =  pair.getInt("val");
        long count = pair.getLong("count");
       
        //All info needed now
        //System.out.println(domain +" :" + year +" : "+count);         
        List<FacetCount> yearCountForDomains = yearFacetDomainCountMap.get(year);
        if (yearCountForDomains == null) { //needs to be created once
          yearCountForDomains= new ArrayList<FacetCount>();
          yearFacetDomainCountMap.put(year,yearCountForDomains);                        
        }
        FacetCount fc = new FacetCount();
        fc.setValue(domain);
        fc.setCount(count); 
        yearCountForDomains.add(fc);
      }
    
    }
  return yearFacetDomainCountMap;
  }
  

  /*
   * TODO specify format
   * 
   */  
  public static String generateDomainQueryStatisticsString(HashMap<Integer, List<FacetCount>> domainStatisticsForQuery) {      
      StringBuilder matrix = new StringBuilder();
      
      //Logic to create to matrix.
      TreeSet<String> allValues = new TreeSet<String>(); //will be sorted 
      for (int year : domainStatisticsForQuery.keySet()){                
          List<FacetCount> list = domainStatisticsForQuery.get(year);
          for ( FacetCount facetCount : list) {
             allValues.add(facetCount.getValue());                       
          }
      }            
      
      //Create header
      StringJoiner joiner = new StringJoiner(",");
      joiner.add("State"); //part of format
      for (String value: allValues) {
          joiner.add(value);
      }           
      String header = joiner.toString(); 
      matrix.append(header+"\n\n"); //Double line break
            
            
      //Iterate over years and generate each line
      TreeSet<Integer> yearsSorted = new TreeSet<Integer>();
      yearsSorted.addAll(domainStatisticsForQuery.keySet());
      
      for (int year : yearsSorted) {
          joiner = new StringJoiner(",");
          joiner.add(""+year);    
          
          List<FacetCount> yearValues = domainStatisticsForQuery.get(year);
          HashMap<String, Long> valuesMap = new HashMap<String,Long>(); //Make map since we need them in order from header.
          for (FacetCount f: yearValues) {
              valuesMap.put(f.getValue(),f.getCount()); 
          }
          
          for (String value : allValues) {
              Long count = valuesMap.get(value);
              if (count == null) {
                 joiner.add("0");
              }
              else {
                  joiner.add(""+count);
              }              
          }          
          String line = joiner.toString();
          matrix.append(line+"\n");
                              
      }     
      return matrix.toString();      
  }
  
}
