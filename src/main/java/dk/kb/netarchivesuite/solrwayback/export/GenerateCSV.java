package dk.kb.netarchivesuite.solrwayback.export;


import java.util.Date;
import java.util.List;

import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.common.SolrDocument;

/**
 * Created by teg on 10/28/16.
 */
// TODO: This would be better with a dedicated CSV writer to handle the different types and escaping properly
public class GenerateCSV {

    private static String NEWLINE="\n";
    
    private static final String FIELD_SEPARATOR = ",";
    private static final String MULTIVALUE_SEPARATOR = "\t";


    private final String[] fields;
    private boolean hasBeenCalled = false;

    /**
     * Create a stateful CSV handler where first call to {@link #toCVSLine(SolrDocument)} will insert headers above
     * the standard output.
     * @param fields the fields to write.
     */
    public GenerateCSV(String[] fields) {
        this.fields = fields;
    }

    /**
     * Output the {@link #fields} for the given Solr Document.
     * If this is the first call, the output will have a header line before the data line.
     * @param doc a Solr Document with at least {@link #fields}.
     * @return a String containing the {@link #fields} content as Comma Separated Values.
     */
    public String toCVSLine(SolrDocument doc) {
        StringBuffer sb = new StringBuffer();
        if (!hasBeenCalled) {
            addHeadlineFields(sb, fields);
            hasBeenCalled = true;
        }
        generateLine(sb, doc, fields);
        return sb.toString();
    }


    public static void addHeadlineFields(StringBuffer buffer, String[] csvFields) {
     boolean modified = false;
     
     for (String field: csvFields) {
         if (modified) {
             buffer.append(FIELD_SEPARATOR);
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
                    result.append(String.join(MULTIVALUE_SEPARATOR, (List<String>) field_value));
                } else if (field_value instanceof String) {
                    result.append(escapeQuotes(field_value.toString()));
                } else if (field_value instanceof Date) {
                    // Dates formatted to ISO-8601 with second granularity
                    result.append(escapeQuotes(DateUtils.getSolrDate((Date) field_value)));
                } else {
                    // Numbers and boolean are appended directly (no quotes)
                    result.append(field_value);
                }
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
