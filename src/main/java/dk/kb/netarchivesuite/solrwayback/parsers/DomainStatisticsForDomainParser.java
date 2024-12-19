package dk.kb.netarchivesuite.solrwayback.parsers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

import dk.kb.netarchivesuite.solrwayback.service.dto.FacetCount;
import dk.kb.netarchivesuite.solrwayback.util.JsonUtils;

public class DomainStatisticsForDomainParser {

  
  
  public static Map<String,List<FacetCount>> parseDomainStatisticsJson(String jsonString){
    Map<String,List<FacetCount>> yearFacetDomainCountMap = new HashMap<String,List<FacetCount>>();

    
    JSONObject json = new JSONObject(jsonString);
    JSONObject jsonFacets = JsonUtils.getSubObjectIfExists(json, "facets");
    long totalCount = jsonFacets.getLong("count"); //Total summation over all year of all results.    
    JSONObject jsonDomains= JsonUtils.getSubObjectIfExists(jsonFacets, "domains");
    
    if (jsonDomains != null) {
        JSONArray buckets = jsonDomains.getJSONArray("buckets");

        for (int i = 0; i < buckets.length(); i++) {
            JSONObject bucketArray = (JSONObject) buckets.get(i);
            String domain = bucketArray.get("val").toString();
            JSONObject years = JsonUtils.getSubObjectIfExists(bucketArray, "years");
            JSONArray yearBuckets = years.getJSONArray("buckets");

            for (int j = 0; j < yearBuckets.length(); j++) {
                JSONObject pair = (JSONObject) yearBuckets.get(j);
                // format : YYYY-MM-DDT00:00:00Z -> YYYY-MM-DD
                String year = pair.getString("val").substring(0, 10);
                long count = pair.getLong("count");

                // All info needed now
                // System.out.println(domain +" :" + year +" : "+count);
                List<FacetCount> yearCountForDomains = yearFacetDomainCountMap.get(year);
                if (yearCountForDomains == null) { // needs to be created once
                    yearCountForDomains = new ArrayList<FacetCount>();
                    yearFacetDomainCountMap.put(year, yearCountForDomains);
                }
                FacetCount fc = new FacetCount();
                fc.setValue(domain);
                fc.setCount(count);
                yearCountForDomains.add(fc);
            }
        }
    }
  return yearFacetDomainCountMap;
  }
  

  /*
   * TODO specify format
   * 
   */  
  public static String generateDomainQueryStatisticsString(Map<String, List<FacetCount>> domainStatisticsForQuery, List<String> dates) {
      StringBuilder matrix = new StringBuilder();
      
      //Logic to create to matrix.
      TreeSet<String> allValues = new TreeSet<String>(); //will be sorted 
      for (String year : domainStatisticsForQuery.keySet()){
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
            
      // Create body only if there are values
      if (!allValues.isEmpty()) {
          // Iterate over years and generate each line
          TreeSet<String> yearsSorted = new TreeSet<String>();
          yearsSorted.addAll(dates);
          
          for (String year : yearsSorted) {
              joiner = new StringJoiner(",");
              joiner.add("" + year);
              
              List<FacetCount> yearValues = new ArrayList<>();
              if (domainStatisticsForQuery.containsKey(year)) {
                  yearValues = domainStatisticsForQuery.get(year);
              }
              Map<String, Long> valuesMap = new HashMap<String,Long>(); //Make map since we need them in order from header.
              for (FacetCount f: yearValues) {
                  valuesMap.put(f.getValue(),f.getCount()); 
              }
              
              for (String value : allValues) {
                  Long count = valuesMap.get(value);
                  if (count == null) {
                     joiner.add("0");
                  }
                  else {
                      joiner.add("" + count);
                  }
              }
              String line = joiner.toString();
              matrix.append(line + "\n");
          }
      }     
      return matrix.toString();      
  }
  
}
