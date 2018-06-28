package dk.kb.netarchivesuite.solrwayback.export;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.solr.common.SolrDocument;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;

/**
 * Created by teg on 10/28/16.
 */
public class GenerateCSV {

    private static String NEWLINE="\n";
    private static String SEPARATOR=",";

    private static String formatCsvEntry(Object entry){
        if (entry == null){
            entry = "";
        }
        return "\"" + entry.toString().replace("\"","\"\"") + "\""; // "->""
    }

   public static void addHeadlineFields(StringBuffer buffer, String[] csvFields) throws Exception{
     boolean modified = false;

     buffer.append("#");
     for (String field: csvFields) {
         if (modified) {
             buffer.append(",");
         }
         modified = true;
         buffer.append(nicify(field));
     }
     buffer.append(NEWLINE);
   }

    /**
     * @deprecated use {@link #addHeadlineFields(StringBuffer, String[])} instead as it is generic.
     */
   @Deprecated
   public  static void addHeadlineBrief(StringBuffer buffer) throws Exception{
      buffer.append("#Title,Date,WaybackURL");
      buffer.append(NEWLINE);
    }
    /**
     * @deprecated use {@link #addHeadlineFields(StringBuffer, String[])} instead as it is generic.
     */
   @Deprecated
   public  static void addHeadlineFull(StringBuffer buffer) throws Exception{
     buffer.append("#Title, Host, Public Suffix, Crawl Year, Content Type, Content Language, Crawl Date, URL, Wayback Date, WaybackURL");
     buffer.append(NEWLINE);
     }
   
    private static String nicify(String field) {
        switch (field.toLowerCase(Locale.ENGLISH)) {
            case "public_suffix": return "Public Suffix";
            case "crawl_year":    return "Crawl Year";
            case "content_type":  return "Content Type";
            case "content_language":  return "Content Language";
            case "crawl_date":  return "Crawl Date";
            case "wayback_date":  return "Wayback Date";
            case "waybackurl":  return "WaybackURL";
            default: return field.substring(0, 1).toUpperCase(Locale.ENGLISH) + field.substring(1);
        }
    }

   //Results from the British Library's Shine interface, (webarchive.org.uk/shine), on 04/07/2017.
   
   public static void generateFirstLineHeader(StringBuffer buffer){
     DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
     buffer.append("#Results from The Royal Library (Denmark) Solrwayback interface, on ").
             append(formatOut.format(new Date()));
     buffer.append(NEWLINE);          
   }
    
   public static void generateSecondLineHeader(StringBuffer buffer, String query, String filterQuery){
     buffer.append("#Query:").append(query).append("     Filter query:").append(filterQuery);
     buffer.append(NEWLINE);          
   }

    public  static void generateLine(StringBuffer buffer,SolrDocument doc, String[] fields) {
        boolean modified = false;

        for (String field: fields) {
            if (modified) {
                buffer.append(SEPARATOR);
            }
            modified = true;

            switch (field) {
                case "crawl_date": {
                    buffer.append(formatCsvEntry(
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) doc.get("crawl_date"))));
                    break;
                }
                case "waybackurl": {
                    Long waybackDate = (Long)doc.get("wayback_date");
                    String url = (String)doc.get("url");
                    if (waybackDate != null && url != null) {
                        String callback = PropertiesLoader.WAYBACK_BASEURL +"services/wayback?waybackdata=" +
                                          waybackDate + "/" +url;
                        buffer.append(formatCsvEntry(callback));
                    }
                    break;
                }
                default: {
                    buffer.append(formatCsvEntry(doc.get(field)));
                }
            }
        }
        buffer.append(NEWLINE);
    }

   
    public  static void generateLineBrief(StringBuffer buffer,SolrDocument doc) throws Exception{
           DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
           
           Date crawlDate = (Date) doc.get("crawl_date");                       
           String waybackDate = ""+(Long) doc.get("wayback_date");           
           String url = (String) doc.get("url"); 
           
           buffer.append(formatCsvEntry((String) doc.get("title")));
           buffer.append(SEPARATOR);                                  
           buffer.append(formatOut.format(crawlDate));
           buffer.append(SEPARATOR);
           buffer.append(PropertiesLoader.WAYBACK_BASEURL+"services/wayback?waybackdata="+waybackDate+"/"+url);
           buffer.append(NEWLINE);
    }

    
    public static void generateLineFull(StringBuffer buffer,SolrDocument doc) throws Exception{
           DateFormat formatOut= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
           Date crawlDate = (Date) doc.get("crawl_date");            
           String waybackDate = ""+ (Long) doc.get("wayback_date");
           String url = (String) doc.get("url");          
           
           String contentType = (String)doc.get("content_type");
           String content_type=contentType;
           buffer.append(formatCsvEntry((String) doc.get("title")));
           buffer.append(SEPARATOR);          
           buffer.append(formatCsvEntry((String) doc.get("host")));
           buffer.append(SEPARATOR);           
           buffer.append(formatCsvEntry((String) doc.get("public_suffix")));
           buffer.append(SEPARATOR);
           buffer.append(formatCsvEntry(""+(Integer) doc.get("crawl_year")));
           buffer.append(SEPARATOR);
           buffer.append(formatCsvEntry(content_type));
           buffer.append(SEPARATOR);
           buffer.append(formatCsvEntry((String) doc.get("content_language")));
           buffer.append(SEPARATOR);         
           buffer.append(formatOut.format(crawlDate));
           buffer.append(SEPARATOR);
           buffer.append(formatCsvEntry((String) doc.get("url")));
           buffer.append(SEPARATOR);
           buffer.append(formatCsvEntry(waybackDate));
           buffer.append(SEPARATOR);                                 
           buffer.append(PropertiesLoader.WAYBACK_BASEURL+"services/wayback?waybackdata="+waybackDate+"/"+url);
           buffer.append(NEWLINE);
    }

}
