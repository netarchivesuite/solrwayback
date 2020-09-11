package dk.kb.netarchivesuite.solrwayback.solr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeSet;

public class TempTokeDomainStatsForQuery {

  public static void main(String[] args) {
    
    
    //Set up SSH tunnel first
    NetarchiveSolrClient.initialize("http://localhost:52300/solr/ns/");
        
    NetarchiveSolrClient client = NetarchiveSolrClient.getInstance();
    
    String query="demokrati";
    List<String> fq = new ArrayList<String>();
    
    try  {
    
      long start=System.currentTimeMillis();
      HashMap<Integer, List<FacetCount>> domainStatisticsForQuery = client.domainStatisticsForQuery(query, fq); // <- This method need Toke's eyes.     
      System.out.println("Time in millis:"+( System.currentTimeMillis() -start)) ;      
     
      
      //List data. Just to see extraction is correct. 
      String matrix = generateDomainQueryStatisticsString(domainStatisticsForQuery); //no need to refactor!
      System.out.println(matrix);
      
      
      
      
      
    }
    catch (Exception e) {      
      e.printStackTrace();
    }
    
    
  }
  
  /*
   * No need to refactor!
   * 
   */
  
  private static String generateDomainQueryStatisticsString(HashMap<Integer, List<FacetCount>> domainStatisticsForQuery) {
    
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
