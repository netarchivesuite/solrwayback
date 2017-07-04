package dk.kb.netarchivesuite.solrwayback.export;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.solr.common.SolrDocument;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;

/**
 * Created by teg on 10/28/16.
 */
public class GenerateCSV {

    private static String NEWLINE="\n";
    private static String SEPARATOR=",";

 

    private static String formatCsvEntry(String entry){
        if (entry == null){
            entry ="";
        }
        entry=entry.replaceAll("\"","\"\""); // "->""
        return "\""+ entry +"\"";
    }


    

    
   public  static void addHeadlineBrief(StringBuffer buffer) throws Exception{
      buffer.append("Title,Date,URL");
      buffer.append(NEWLINE);
      }

   
   //Results from the British Library's Shine interface, (webarchive.org.uk/shine), on 04/07/2017.
   
   public static void generateFirstLineHeader(StringBuffer buffer){
     DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
     buffer.append("Results from The Royal Library (Denmark) Solrwayback interface, on "+formatOut.format(new Date()));
     buffer.append(NEWLINE);          
   }
    
   public static void generateSecondLineHeader(StringBuffer buffer, String query, String filterQuery){
     buffer.append("Query:"+query +"     Filter query:"+filterQuery);
     buffer.append(NEWLINE);          
   }

   
   
    public  static void generateLineBrief(StringBuffer buffer,SolrDocument doc) throws Exception{
           DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            buffer.append(formatCsvEntry((String) doc.get("title")));
            buffer.append(SEPARATOR);
            
            Date crawlDate = (Date) doc.get("crawl_date");            
            String waybackDate = (String) doc.get("wayback_date");
            String url = (String) doc.get("url");            
            buffer.append(formatOut.format(crawlDate));
            buffer.append(SEPARATOR);
            buffer.append(PropertiesLoader.WAYBACK_BASEURL+"services/wayback?waybackdata="+waybackDate+"/"+url);
            buffer.append(NEWLINE);
    }

      

}
