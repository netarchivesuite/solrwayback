package dk.kb.netarchivesuite.solrwayback.export;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.solr.common.SolrDocument;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;

/**
 * Created by teg on 10/28/16.
 */
public class GenerateCSV {

    private static String NEWLINE="\n";
    
    private static final String FIELD_SEPARATOR = ",";
    private static final String MULTIVALUE_SEPARATOR = "\t";

    
    
    

   public static void addHeadlineFields(StringBuffer buffer, String[] csvFields) throws Exception{
     boolean modified = false;
     
     for (String field: csvFields) {
         if (modified) {
             buffer.append(",");
         }
         modified = true;
         buffer.append(field);
     }
     buffer.append(NEWLINE);
   }

  
      

    public  static void generateLine(StringBuffer buffer,SolrDocument doc, String[] fieldList) {
           StringBuilder result = new StringBuilder();
        
        
        for (String field : fieldList) {
            Object field_value = doc.getFieldValue(field.trim());
            if (field_value != null) { //if null, just output a tab
                
                if (field_value instanceof List) { //if multivalued
                    field_value = String.join(MULTIVALUE_SEPARATOR, (List<String>) field_value);
                }
                String escaped = escapeQuotes(field_value.toString());
                result.append(escaped);
            } else {
                result.append(escapeQuotes(""));
            }
            result.append(FIELD_SEPARATOR);
        }
        
        
        //Remove last tab  - need to be done smarter
        result.delete(result.length() - FIELD_SEPARATOR.length(), result.length());
        
        buffer.append(result.toString());
        buffer.append(NEWLINE);
        
    }

    
  //Sets " around the expression and replaces " with "". (CSV format)
    private static String escapeQuotes(String text) {
        if (text == null) {
            return "";
        }
        return "\"" + text.replaceAll("\"", "\"\"") + "\"";
    }
    
    
}
