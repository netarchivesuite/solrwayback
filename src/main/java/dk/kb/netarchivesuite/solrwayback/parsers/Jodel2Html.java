package dk.kb.netarchivesuite.solrwayback.parsers;

import org.json.JSONObject;

public class Jodel2Html {

  public static String json2Html(String jsonString){
  StringBuilder b = new StringBuilder();
  JSONObject json = new JSONObject(jsonString);       
   
  
  b.append("<html><body>");
  
  //DO stuff write html
  
    
   b.append("</body></html>"); 
    return b.toString();
  }
  
  
  
  
}
