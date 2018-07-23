package dk.kb.netarchivesuite.solrwayback.parsers;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.javac.util.Log;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.solr.WaybackStatistics;


public class WaybackToolbarInjecter {
  private static final Logger log = LoggerFactory.getLogger(WaybackToolbarInjecter.class);
  

  
  public static void main(String[] args) throws Exception{
       
    
    String html="<?xml version=\"1.1\" encoding=\"iso-8859-1\"?>"+
        "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\">"+
        "<head>"+
        "   <title>kimse.rovfisk.dk/katte/</title>"+
        "   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\" />"+         
        "   <link rel=\"stylesheet\" href=\"/style.css\"  type=\"text/css\" media=\"screen\" />"+
        " <link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\"/rss2.php\" />"+        
        "</head>"+
        "<body>"+
        "<a class=\"toplink\" href=\"/\">kimse.rovfisk.dk  </a><a class=\"toplink\" href=\"/katte/\">katte / </a><br /><br /><table cellspacing=\"8\"><tr><td></td><td class=\"itemw\"><a href=\"/katte/?browse=DSC00175.JPG\"><img class=\"lo\" src=\"/cache/katte/DSC00175.JPG\" /></a></td>"+
        "<td></td><td class=\"itemw\"><a href=\"/katte/?browse=DSC00209.JPG\"><img class=\"lo\" src=\"/cache/katte/DSC00209.JPG\" /></a></td>"+
        "</table><br />  </body>"+
        "</html>";
    
    
    
   
    WaybackStatistics stats = new WaybackStatistics();
    stats.setUrl_norm("jp.dk");
    stats.setHarvestDate("2015-09-17T17:02:03Z");        
    stats.setFirstHarvestDate("2015-09-17T17:02:03Z");
    stats.setNextHarvestDate("2015-09-17T17:02:03Z");
    stats.setPreviousHarvestDate("2015-09-17T17:02:03Z");
    stats.setLastHarvestDate("2015-09-17T17:02:03Z");
    stats.setNumberOfHarvest(101);

    HtmlParseResult htmlParsed = new HtmlParseResult();
    htmlParsed.setHtmlReplaced(html);
    htmlParsed.setNumberOfLinksNotFound(1);
    htmlParsed.setNumberOfLinksReplaced(17);
        
    String injectedHtml = injectInHmtl(htmlParsed,stats, "test",1234L, false);
    System.out.println(injectedHtml);   
  }
  
  
  
  public static String injectWaybacktoolBar(String source_file_path, long offset, HtmlParseResult htmlParsedResult, boolean xhtml) throws Exception{       
    
    try{                
    IndexDoc arcEntry = NetarchiveSolrClient.getInstance().getArcEntry(source_file_path, offset);    
    WaybackStatistics stats = NetarchiveSolrClient.getInstance().getWayBackStatistics(arcEntry.getStatusCode(),arcEntry.getUrl(),arcEntry.getUrl_norm(), arcEntry.getCrawlDate());            
    stats.setHarvestDate(arcEntry.getCrawlDate());        
    
    String injectedHtml =injectInHmtl( htmlParsedResult, stats, source_file_path,offset, xhtml);
    return injectedHtml;
   }catch (Exception e){
     log.error("error injecting waybacktoolbar", e);
    return htmlParsedResult.getHtmlReplaced();// no injection (should not happen). 
   }

    
  }
  
  public static String injectInHmtl(HtmlParseResult htmlParsed, WaybackStatistics stats,String source_file_path, long offset, boolean xhtml) throws Exception{
    String orgHtml=htmlParsed.getHtmlReplaced();
    Document doc = Jsoup.parse(orgHtml);
    if (xhtml){
      doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml); //Without this json will not terminate tags correct as xhtml 
    }
    String injectHtml = generateToolbarHtml(htmlParsed,stats, source_file_path, offset);
    
    //Inject right after body if possible, else default to last
    Elements body = doc.select("body");   //Seems this will always work. Will fix missing html/body tags
      //body.append(injectHtml); //Inject just before </body>  
      //body.first().children().first().append(injectHtml); //Inject just after <body> NOT WORKING          
      //body.before(injectHtml);
      body.prepend(injectHtml); //Inject just before </body> 
      log.info("wayback tool injected. xhtml:"+xhtml);
      
    return doc.toString();    
  }
  
  private static String generateToolbarHtml(HtmlParseResult htmlParsed,WaybackStatistics stats, String source_file_path, long offset) throws Exception{
    
  
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    Date d = dateFormat.parse(stats.getHarvestDate());
        
    SimpleDateFormat longFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
        
    log.info(stats.toString());
    String inject = 
    "<!-- BEGIN WAYBACK TOOLBAR INSERT -->" +
    "   <div class=\"closed\" id=\"tegModal\" style=\"\">" +
    "       <div><a onclick=\"toggleModal();return false\" id=\"toggleToolbar\" href=\"#\">Toolbar</a></div>" +
    "       <div><a onclick=\"closeModal();return false\" id=\"closeToolbar\" href=\"#\">Close</a></div>" +
    "       <div id=\"tegContent\">" +
    "           <div class=\"infoLine\">" +
    "               <span class=\"label\">Harvest date:</span>" +
    "               <span class=\"dynamicData\">"+longFormat.format(d)+"</span>" +
    "               <span class=\"inlineLabel\">HTTP status code:</span>" +
    "               <span class=\"dynamicData\">"+stats.getStatusCode() +"</span>" +        
    "           </div>" +
    "           <div class=\"infoLine\">" +
    "               <span class=\"label\">Url:</span>" +
    "               <span class=\"dynamicData\">"+stats.getUrl()+"</span>" +
    "               <span class=\"inlineLabel\">#Harvested:</span>" +
    "               <span class=\"dynamicData\">"+stats.getNumberOfHarvest() +"</span>" +
    "            </div>" +
    "            <div class=\"infoLine\">" +
    "               <span class=\"label\">Domain:</span>" +
    "               <span class=\"dynamicData\">"+stats.getDomain() +"</span>" +
    "               <span class=\"inlineLabel\">#Harvested:</span>" +
    "               <span class=\"dynamicData\">"+stats.getNumberHarvestDomain()+"</span>" +
    "               <span class=\"inlineLabel\">#Content length harvested:</span>" +
    "               <span class=\"dynamicData\">"+stats.getDomainHarvestTotalContentLength()+"</span>" +    
    "            </div>" +
    "            <div class=\"infoLine\">" +
    "               <span class=\"label\">Page resources:</span>" +
    "               <span class=\"inlineLabel\">#Found:</span>" +
    "               <span class=\"dynamicData\">"+htmlParsed.getNumberOfLinksReplaced()+"</span>" +
    "               <span class=\"inlineLabel\">#Not found:</span>" +
    "               <span class=\"dynamicData\">"+htmlParsed.getNumberOfLinksNotFound()+"</span>" +
    "            </div>" +
    "            <div class=\"infoLine\">" +
    "               <span title=\"View in- and out-going links\"class=\"dynamicData icon\">"+generateDomainGraphImageLink("graph_icon.png",stats.getDomain()) +"</span>" +
    "               <span title=\"View dates for harvest\" class=\"dynamicData icon\">"+generateCalendarImageLink("calendar_icon.png",stats.getUrl_norm()) +"</span>" +
    "               <span title=\"View XML\" class=\"dynamicData icon\">"+generatePwid("xml.png",source_file_path,offset) +"</span>" +
    "               <span title=\"View website previews\" class=\"dynamicData icon\">"+generatePagePreviews("preview.png",stats.getUrl_norm()) +"</span>" +
    "               <span title=\"View harvest time for page resources\" class=\"dynamicData icon\">"+generatePageResources("watch.png",source_file_path,offset) +"</span>" +
            "       <span title=\"View domain developement over time\" class=\"dynamicData icon\">" + generateDomainGrowthImageLink("growth_icon.png",stats.getDomain()) +"</span>" +
    "            </div>" +    
    "           <div class=\"paging\">" +
    "               <div class=\"pagingBlock\">" +
    "                   <span class=\"inlineLabel\">First:</span>"+ generateWaybackLinkFromCrawlDateAndUrl(stats.getUrl(), stats.getFirstHarvestDate()) +
   
    "               </div> " +
    "               <div class=\"pagingBlock\">" +
    "                   <span class=\"inlineLabel\">Previous:</span> " + generateWaybackLinkFromCrawlDateAndUrl(stats.getUrl(), stats.getPreviousHarvestDate()) +

    "               </div> " +
    "               <div class=\"pagingBlock\">" +
    "                   <span class=\"inlineLabel\">Next:</span> " + generateWaybackLinkFromCrawlDateAndUrl(stats.getUrl(), stats.getNextHarvestDate()) +
    
    "                   </div> " +
    "               <div class=\"pagingBlock\">" +
    "                   <span class=\"inlineLabel\">Last:</span>" + generateWaybackLinkFromCrawlDateAndUrl(stats.getUrl(), stats.getLastHarvestDate()) + 
    
    "               </div>" +
    "           </div>" +
    "      </div>" +
    "       <style>" +
    "       #tegModal *{background: white; border:0;box-sizing: content-box; color: black;margin: 0;font-family: arial, Helvetica,sans-serif; " +
    "       font-size: 14px; opacity: 1; padding: 0; width: auto}" +
    "       #tegModal p, #tegModal div{display: block}" +
    "       #tegModal span, #tegModal a{display: inline}" +
    "       #tegModal{z-index: 999999 !important; color: black; font-size: 14px;" + "font-family: arial, Helvetica,sans-serif;background: #ffffff; border: 1px solid black;border-radius: 4px; " +
    "       box-shadow: 0 0 5px 5px #ccc; display: block; left: calc(50% - 450px); opacity: 1; padding: 1.5em 1.5em .5em;" +
            "position:fixed; text-align:left !important; top: 25%; width: 900px; z-index: 500; box-sizing: content-box;" +
    "       transition: left 0.4s, opacity 0.3s, padding 0.3s, top 0.4s, width 0.3s;}" +
            "#tegModal p, #tegModal div{color: black !important; font-family: Arial, Helvetica, sans-serif; font-size: 12px !important}" +
    "       #tegModal.closed {box-shadow: 0 0 0 0; left: 3px;opacity: 0.8; padding:0.5em; top: 3px; width: auto; text-orientation: upright; writing-mode: vertical-rl;}" +
    "       #toggleToolbar, #closeToolbar{float: right; margin: -.8em -.5em 2em 2em;}" +
    "       #tegModal.closed #toggleToolbar{float: none; margin: 0;}" +
    "       #toggleToolbar{margin-left: 1em;}" +
    "       #tegModal.closed #tegContent,#tegModal.closed #closeToolbar{display: none}" +
    "       #tegModal .infoLine{margin-bottom: .5em;}" +
    "       #tegModal a img {display: inline-block; margin: 2em 3em 2em 0; max-height: 60px; }" +        
    "       #tegModal a {color: #003399; font-size: 14px; text-decoration: none}" +
    "       #tegModal a:hover {color: #003399; text-decoration: underline}"+
    "       #tegModal.closed a:hover {text-decoration: none}"+
    "       #tegModal .label{background:transparent;color:black;display: inline-block;font-size:12px;font-weight: bold; min-width: 110px;text-align: left;}" +
    "       #tegModal .inlineLabel{display: inline-block;font-weight: bold; margin: 0 .2em 0 .8em;}" +
    "       #tegModal .paging .inlineLabel{margin: 0 .5em 0 .1em;}" +
    "       #tegModal .paging{border-top: 1px solid #ccc; margin-top: 1em; padding-top: 0.8em;}" +
    "       #tegModal .pagingBlock{display: inline-block; margin-right: .8em}" +
    "       #tegModal .dynamicData.icon{display: inline-block}" +
    "       </style>" +
    "       <script type=\"text/javascript\">" +
    "           function toggleModal(){" +
    "               if(document.getElementById(\"tegModal\").className == \"open\"){" +
    "                   document.getElementById(\"tegModal\").className = \"closed\";" +
    "                   document.getElementById(\"toggleToolbar\").innerHTML = \"Toolbar\";" +
    "               }else{" +
    "                   document.getElementById(\"tegModal\").className = \"open\";" +
    "                   document.getElementById(\"toggleToolbar\").innerHTML = \"Hide\";" +
    "               }" +
    "           }           " +
    "           function closeModal(){" +
    "               document.getElementById(\"tegModal\").style.display = \"none\";" +
    "           }" +
    "       </script>" +
    "   </div>" +
    "<!-- END WAYBACK TOOLBAR INSERT -->";
  return inject;
  }
  
  private static String generateWaybackLinkFromCrawlDateAndUrl(String url, String crawlDate) throws Exception{
    
    if (crawlDate != null){
      

      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      Date d = dateFormat.parse(crawlDate);
      SimpleDateFormat waybackDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");      
      String dateFormatted = waybackDateFormat.format(d);

      SimpleDateFormat presentationFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");      
      String datePresentation= presentationFormat.format(d);
  
      return "<a href=\""+PropertiesLoader.WAYBACK_BASEURL+"services/web/"+dateFormatted+"/"+url+"\"" + " title=\""+datePresentation+"\">"+datePresentation+"</a>";
    }
    else{
      return ("none");
    }          
  }
  
 
  
  private static String generatePwid(String image, String source_file_path, long offset) throws Exception{

    return "<a href=\""+PropertiesLoader.WAYBACK_BASEURL+"services/generatepwid?source_file_path="+ source_file_path+ "&offset="+offset +"\" target=\"_blank\"><img src=\""+PropertiesLoader.WAYBACK_BASEURL+"images/"+image+"\" /> </a>";
  }
  
  
  private static String generateCalendarImageLink(String image,String url) throws Exception{

    String urlEncoded=URLEncoder.encode(url, "UTF-8");
    return "<a href=\""+PropertiesLoader.WAYBACK_BASEURL+"calendar.jsp?url="+ urlEncoded+"\" target=\"_blank\"><img src=\""+PropertiesLoader.WAYBACK_BASEURL+"images/"+image+"\" /> </a>";
  }
  
  private static String generatePagePreviews(String image,String url) throws Exception{

    String urlEncoded=URLEncoder.encode(url, "UTF-8");
    return "<a href=\""+PropertiesLoader.WAYBACK_BASEURL+"pagepreviews.jsp?url="+ urlEncoded+"\" target=\"_blank\"><img src=\""+PropertiesLoader.WAYBACK_BASEURL+"images/"+image+"\" /> </a>";
  }
  
  private static String generatePageResources(String image, String source_file_path, long offset) throws Exception{
    return "<a href=\""+PropertiesLoader.WAYBACK_BASEURL+"pageresources.jsp?source_file_path="+ source_file_path+ "&offset="+offset +"\" target=\"_blank\"><img src=\""+PropertiesLoader.WAYBACK_BASEURL+"images/"+image+"\" /> </a>";
  }
  
  private static String generateDomainGraphImageLink(String image,String domain){
    return "<a href=\""+PropertiesLoader.WAYBACK_BASEURL+"waybacklinkgraph.jsp?domain="+domain+"\" target=\"_blank\"><img src=\""+PropertiesLoader.WAYBACK_BASEURL+"images/"+image+"\" /> </a>";
  }
  
  private static String generateDomainGrowthImageLink(String image,String domain){
    return "<a href=\""+PropertiesLoader.WAYBACK_BASEURL+"domaingrowth.html?domain="+domain+"\" target=\"_blank\"><img src=\""+PropertiesLoader.WAYBACK_BASEURL+"images/"+image+"\" /> </a>";
  }
  
  
}
