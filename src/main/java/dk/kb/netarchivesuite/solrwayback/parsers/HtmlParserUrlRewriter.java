package dk.kb.netarchivesuite.solrwayback.parsers;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;

public class HtmlParserUrlRewriter {

	private static final Logger log = LoggerFactory.getLogger(HtmlParserUrlRewriter.class);
    
	
	//Problem is jsoup text(...) or html(...) encodes & and for the <style> @import... </style> the HTML urls must not be encoded. (blame HTML standard)
	//So it can not be set using JSOUP and must be replaced after.
	private static final String AMPERSAND_REPLACE="_STYLE_AMPERSAND_REPLACE_";
	
	private static Pattern backgroundUrlPattern = Pattern.compile(".*background:url\\((.*)\\).*");
	private static final String CSS_IMPORT_PATTERN_STRING = 
			"(?s)\\s*@import\\s+(?:url)?[(]?\\s*['\"]?([^'\")]*\\.css)['\"]?\\s*[)]?.*";
	private static Pattern  CSS_IMPORT_PATTERN = Pattern.compile(CSS_IMPORT_PATTERN_STRING);

	//replacing urls that points into the world outside solrwayback because they are never harvested
    private static final String NOT_FOUND_LINK=PropertiesLoader.WAYBACK_BASEURL+"services/notfound/";
	
	public static void main(String[] args) throws Exception{


		  	String css="@import \"mystyle.css\";\n"+
                "@import url(slidearrows.css);\n"+
                "@import url(shadow_frames.css);\n"+
                "body {";

		 /*

		//String css= new String(Files.readAllBytes(Paths.get("/home/teg/Desktop/toke.css")));

		System.out.println(css);

		String[] result = css.split("\n", 100);
		//Get the lines starting with @import until you find one that does not. 
		int index = 0;
		while (result[index].startsWith("@import ") && index <result.length){    		
			String importLine = result[index++];

			System.out.println("css import found:"+importLine);

			Matcher m = CSS_IMPORT_PATTERN.matcher(importLine);
			if (m.matches()){
				String url= m.group(1);

				System.out.println("found url:"+url);

			}
		}

		System.out.println("done");
		System.exit(1);
*/

		String html="<?xml version=\"1.1\" encoding=\"iso-8859-1\"?>"+
				"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\">"+
				"<head>"+
				"   <title>kimse.rovfisk.dk/katte/</title>"+
				"   <style type=\"text/css\" media=\"screen\">@import \"/css/forum.css\";</style>"+ 
				"   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\" />"+         
				"   <link rel=\"stylesheet\" href=\"/style.css\"  type=\"text/css\" media=\"screen\" />"+
				" <link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\"/rss2.php\" />"+
				"</head>"+
				"<body>"+
				" <style type=\"text/css\" media=\"screen\">@import \"//www.dr.dk/drdkGlobal/spot/spotGlobal.css\";</style> "+
				" <style type=\"text/css\" media=\"screen\">@import \"/design/www/global/css/globalPrint.css\";</style> "+
                "<style type=\"text/css\" media=\"screen\">@import url(http://en.statsbiblioteket.dk/portal_css/SB%20Theme/resourceplonetheme.sbtheme.stylesheetsmain-cachekey-7e976fa2b125f18f45a257c2d1882e00.css);</style> "+
                "<a class=\"toplink\" href=\"test\" class=\"button\" style=\"background:url(img/homeico.png) no-repeat ; width:90px; margin:0px 0px 0px 16px;\">kimse.rovfisk.dk  </a><a class=\"toplink\" href=\"/katte/\">katte / </a><br /><br /><table cellspacing=\"8\"><tr><td></td><td class=\"itemw\"><a href=\"/katte/?browse=DSC00175.JPG\"><img class=\"lo\" src=\"/cache/katte/DSC00175.JPG\" /></a></td>"+
                "<td></td><td class=\"itemw\"><a href=\"/katte/?browse=DSC00209.JPG\"><img class=\"lo\" src=\"/cache/katte/DSC00209.JPG\" /></a></td>"+
                "</table><br />  </body>"+
                "</html>";

		HashSet<String> urlSet = new HashSet<String>();
		
		Document doc = Jsoup.parse(html,"http:/test.dk"); //TODO baseURI?
		
		collectRewriteUrlsForStyleImport(urlSet,doc, "http://test.dk");
	     
	       
	       
		
	      
	      System.exit(1);
		//collectStyleBackgroundRewrite(urlSet,doc, "a", "style", "http:/thomas.dk");


		URL baseUrl = new URL("http:www.google.com/someFolder/");
		URL url = new URL( baseUrl , "../test.html");
		System.out.println(url.toString());


	}


	/* CSS can start witht the following and need to be url rewritten also.
	 * @import "mystyle.css";
	 * @import url(slidearrows.css);
	 * @import url(shadow_frames.css) print;
	 */

	public static String replaceLinksCss(ArcEntry arc) throws Exception{

		String type="downloadRaw"; //not supporting nested @imports...
		String css = new String(arc.getBinary(),arc.getContentEncoding());
		String url=arc.getUrl();

		String[] result = css.split("\n", 100); //Doubt there will be more than 100 of these.
		//Get the lines starting with @import until you find one that does not. 
		int index = 0;
				
		while (index <result.length && result[index].startsWith("@import ") ){    		
		  String importLine = result[index++];
		  importLine = importLine.substring(0, Math.min(200, importLine.length()-1)); //Import is in the start of line, it can be very long (minimized) 		  
			Matcher m = CSS_IMPORT_PATTERN.matcher(importLine); // 
			if (m.matches()){
				String cssUrl= m.group(1);		   
				URL base = new URL(url);
				String resolvedUrl = new URL( base ,cssUrl).toString();
				log.info("resolve CSS import url:"+resolvedUrl);		

				IndexDoc indexDoc = NetarchiveSolrClient.getInstance().findClosestHarvestTimeForUrl(resolvedUrl, arc.getCrawlDate());		         
				if (indexDoc!=null){    		    			 
					String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path() +"&offset="+indexDoc.getOffset(); 
					css=css.replaceFirst(cssUrl, newUrl);
				}else{
				    css=css.replaceFirst(cssUrl, NOT_FOUND_LINK);				
					log.info("CSS @import url not harvested:"+cssUrl);
				}	    		 	    		 
			}
		}    	    	    	
		return css;
	}

	public static HtmlParseResult replaceLinks(ArcEntry arc) throws Exception{
	  AtomicInteger numberOfLinksReplaced = new  AtomicInteger();
	  AtomicInteger numberOfLinksNotFound = new  AtomicInteger(); 
	  
		long start = System.currentTimeMillis();
		String encoding = arc.getContentEncoding();
		if (encoding == null){
		  encoding ="UTF-8";
		}
		
		String html = new String(arc.getBinary(), encoding);
		String url=arc.getUrl();


		Document doc = Jsoup.parse(html,url); //TODO baseURI?

				//List<String> urlList = new ArrayList<String>();
		HashSet<String> urlSet = new HashSet<String>();


		
		collectRewriteUrlsForElement(urlSet, doc, "img", "src");
		collectRewriteUrlsForElement(urlSet,doc, "body", "background");
		collectRewriteUrlsForElement(urlSet,doc, "link", "href");
		collectRewriteUrlsForElement(urlSet,doc, "script", "src");
		collectRewriteUrlsForElement(urlSet,doc, "td", "background");
		collectStyleBackgroundRewrite(urlSet,doc, "a", "style",url);
		collectRewriteUrlsForStyleImport(urlSet,doc, url);
		
		log.info("#unique urlset to resolve:"+urlSet.size());

		ArrayList<IndexDoc> docs = NetarchiveSolrClient.getInstance().findClosetsHarvestTimeForMultipleUrls(urlSet,arc.getCrawlDate());


		HashMap<String,IndexDoc> urlReplaceMap = new HashMap<String,IndexDoc>();
		for (IndexDoc indexDoc: docs){
			urlReplaceMap.put(indexDoc.getUrl(),indexDoc);     		     		 
		}

		replaceUrlForElement(urlReplaceMap,doc, "img", "src", "downloadRaw",  numberOfLinksReplaced , numberOfLinksNotFound);
		replaceUrlForElement(urlReplaceMap,doc, "body", "background", "downloadRaw" ,  numberOfLinksReplaced ,  numberOfLinksNotFound);             	     	 
		replaceUrlForElement(urlReplaceMap,doc, "link", "href", "view",  numberOfLinksReplaced ,  numberOfLinksNotFound);
		replaceUrlForElement(urlReplaceMap,doc, "script", "src", "downloadRaw",  numberOfLinksReplaced,  numberOfLinksNotFound);
		replaceUrlForElement(urlReplaceMap,doc, "td", "background", "downloadRaw",  numberOfLinksReplaced ,  numberOfLinksNotFound);    	 
        replaceUrlsForStyleImport(urlReplaceMap,doc,"downloadRaw",url ,  numberOfLinksReplaced,  numberOfLinksNotFound);
		replaceStyleBackground(urlReplaceMap,doc, "a", "style", "downloadRaw",url,  numberOfLinksReplaced,  numberOfLinksNotFound);
		

		//This are not resolved until clicked
		rewriteUrlForElement(doc, "a" ,"href",arc.getCrawlDate());
		rewriteUrlForElement(doc, "area" ,"href",arc.getCrawlDate());		
		rewriteUrlForElement(doc, "form" ,"action",arc.getCrawlDate());

		log.info("Number of resolves:"+urlSet.size() +" total time:"+(System.currentTimeMillis()-start));    	 
        log.info("numberOfReplaced:"+numberOfLinksReplaced + " numbernotfound:"+numberOfLinksNotFound);
		
		String html_output= doc.toString();
		html_output=html_output.replaceAll(AMPERSAND_REPLACE, "&");		
		
		HtmlParseResult res = new HtmlParseResult();
		res.setHtmlReplaced(html_output);
		res.setNumberOfLinksReplaced(numberOfLinksReplaced.intValue());
		res.setNumberOfLinksNotFound(numberOfLinksNotFound.intValue());		
		return res;
	}

	
	public static String generatePwid(ArcEntry arc) throws Exception{

      long start = System.currentTimeMillis();
      String html = new String(arc.getBinary(),arc.getContentEncoding());
      String url=arc.getUrl();


      Document doc = Jsoup.parse(html,url); //TODO baseURI?

              //List<String> urlList = new ArrayList<String>();
      HashSet<String> urlSet = new HashSet<String>();

      collectRewriteUrlsForElement(urlSet,doc, "area", "href");
      collectRewriteUrlsForElement(urlSet, doc, "img", "src");
      collectRewriteUrlsForElement(urlSet,doc, "body", "background");
      collectRewriteUrlsForElement(urlSet, doc, "link", "href");
      collectRewriteUrlsForElement(urlSet , doc, "script", "src");
      collectRewriteUrlsForElement(urlSet, doc, "td", "background");
      collectStyleBackgroundRewrite(urlSet , doc, "a", "style",url);
      collectRewriteUrlsForStyleImport(urlSet, doc,url);
      
      log.info("#unique urlset to resolve:"+urlSet.size());

      ArrayList<IndexDoc> docs = NetarchiveSolrClient.getInstance().findClosetsHarvestTimeForMultipleUrls(urlSet,arc.getCrawlDate());

      StringBuffer buf = new StringBuffer();
      for (IndexDoc indexDoc: docs){
          buf.append("<part>\n");        
          buf.append("pwid:netarkivet.dk:"+indexDoc.getCrawlDate()+":part:"+indexDoc.getUrl() +"\n");
          buf.append("</part>\n");
          //pwid:netarkivet.dk:time:part:url
      
      }
     return buf.toString();
	}

  
	public static HashSet<String> getResourcLinksForHtmlFromArc(ArcEntry arc) throws Exception{

      long start = System.currentTimeMillis();
      String html = new String(arc.getBinary(),arc.getContentEncoding());
      String url=arc.getUrl();


      Document doc = Jsoup.parse(html,url); //TODO baseURI?

              //List<String> urlList = new ArrayList<String>();
      HashSet<String> urlSet = new HashSet<String>();      

      collectRewriteUrlsForElement(urlSet,doc, "area", "href");
      collectRewriteUrlsForElement(urlSet, doc, "img", "src");
      collectRewriteUrlsForElement(urlSet,doc, "body", "background");
      collectRewriteUrlsForElement(urlSet, doc, "link", "href");
      collectRewriteUrlsForElement(urlSet , doc, "script", "src");
      collectRewriteUrlsForElement(urlSet, doc, "td", "background");
      collectStyleBackgroundRewrite(urlSet , doc, "a", "style",url);
      collectRewriteUrlsForStyleImport(urlSet, doc,url);
            
     return urlSet;
    }
	
	

	public static void replaceUrlForElement( HashMap<String,IndexDoc>  map,Document doc,String element, String attribute , String type ,  AtomicInteger numberOfLinksReplaced,   AtomicInteger numberOfLinksNotFound) throws Exception{

		for (Element e : doc.select(element)) {    		 
			String url = e.attr("abs:"+attribute);

			if (url == null  || url.trim().length()==0){
				continue;
			}
			String urlFixed = url.replace(" ", "%20");
			IndexDoc indexDoc = map.get(urlFixed);   
			if (indexDoc!=null){    		    			 
				String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path()+"&offset="+indexDoc.getOffset();    			 
				e.attr(attribute,newUrl);    			     		 
			    numberOfLinksReplaced.getAndIncrement();
			}
			else{
			     e.attr(attribute,NOT_FOUND_LINK);
				log.info("No harvest found for:"+url);
			    numberOfLinksNotFound.getAndIncrement();;
			 }

		}
	}

	public static void replaceStyleBackground( HashMap<String,IndexDoc>  map,Document doc,String element, String attribute , String type, String baseUrl,   AtomicInteger numberOfLinksReplaced,  AtomicInteger numberOfLinksNotFound) throws Exception{

		for (Element e : doc.select(element)) {

			String style = e.attr(attribute);

			if (style == null  || style.trim().length()==0){
				continue;
			}    		  

			String urlUnresolved = getStyleMatch(style);   		   		

			if ( urlUnresolved != null){
				URL base = new URL(baseUrl);
				String resolvedUrl = new URL( base ,urlUnresolved).toString();			
				String resolvedUrlfixed = resolvedUrl.replaceAll(" ", "%20");
				IndexDoc indexDoc = map.get(resolvedUrlfixed);   
				if (indexDoc!=null){    		    			 
					String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path() +"&offset="+indexDoc.getOffset();    			     		
					String styleFixed=style.replaceAll(urlUnresolved,newUrl);    			     
					e.attr(attribute,styleFixed); 
				     numberOfLinksReplaced.getAndIncrement();
				}
				else{
				  String styleFixed=style.replaceAll(urlUnresolved,NOT_FOUND_LINK);                    
                  e.attr(attribute,styleFixed); 				  			     
                  log.info("No harvest found for:"+resolvedUrl);
                  numberOfLinksNotFound.getAndIncrement();
				}


			}

		}   }

	/*
	 * For stupid HTML standard 
	 * <a href="test" style="background:url(img/homeico.png) no-repeat ; width:90px; margin:0px 0px 0px 16px;">
	 * 
	 */
	public static void collectStyleBackgroundRewrite(HashSet<String> set,Document doc,String element, String attribute ,  String baseUrl) throws Exception{

		for (Element e : doc.select(element)) {

			String style = e.attr(attribute);

			if (style == null  || style.trim().length()==0){
				continue;
			}    		     		 
			String unResolvedUrl = getStyleMatch(style);   		   		
			if (unResolvedUrl != null){
				URL base = new URL(baseUrl);
				URL resolvedUrl = new URL( base , unResolvedUrl);			
				String resolvedUrlFixed = resolvedUrl.toString().replace(" ", "%20");
				set.add(resolvedUrlFixed);
			}

		}
	}

	/*
	 * background:url(img/homeico.png) no-repeat ; width:90px ... ->  img/homeico.png
	 */
	private static String getStyleMatch(String style){
		Matcher m = backgroundUrlPattern.matcher(style);
		if (m.matches()){
			String url= m.group(1);
			return url;
		}
		return null;    	
	}

	public static void collectRewriteUrlsForElement(HashSet<String> set,Document doc,String element, String attribute ) throws Exception{

		for (Element e : doc.select(element)) {
			String url = e.attr("abs:"+attribute);

			if (url == null  || url.trim().length()==0){
				continue;
			}    		     		
			String urlFixed = url.replace(" ","%20");
			set.add(urlFixed);   		    		 		
		}
	}


	//<style type="text/css" media="screen">@import url(http://en.statsbiblioteket.dk/portal_css/SB%20Theme/resourceplonetheme.sbtheme.stylesheetsmain-cachekey-7e976fa2b125f18f45a257c2d1882e00.css);</style> 
	public static void collectRewriteUrlsForStyleImport(HashSet<String> set, Document doc, String baseUrl) throws Exception{

      for (Element e : doc.select("style")) {         
        String styleTagContent = e.data();          
        Matcher m = CSS_IMPORT_PATTERN.matcher(styleTagContent);
        if (m.matches()){
            String cssUrl= m.group(1);         
              URL base = new URL(baseUrl);
              URL resolvedUrl = new URL( base , cssUrl);           
              
              String resolvedUrlFixed = resolvedUrl.toString().replaceAll(" ","%20");
              set.add(resolvedUrlFixed);
        }                   
      }
  }


	
	public static void replaceUrlsForStyleImport( HashMap<String,IndexDoc>  map, Document doc, String type, String baseUrl,   AtomicInteger numberOfLinksReplaced,   AtomicInteger numberOfLinksNotFound) throws Exception{
	
      for (Element e : doc.select("style")) {         
        String styleTagContent = e.data();          
        Matcher m = CSS_IMPORT_PATTERN.matcher(styleTagContent);
        if (m.matches()){
          String url = m.group(1);
          URL base = new URL(baseUrl);
          URL resolvedUrl = new URL( base , url);            
          
          String resolvedUrlFixed = resolvedUrl.toString().replace(" ","%20");
          IndexDoc indexDoc = map.get(resolvedUrlFixed);   
          if (indexDoc!=null){                             
              String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path()+"_STYLE_AMPERSAND_REPLACE_offset="+indexDoc.getOffset();                 
              log.info("replaced @import:"+e );
              log.info("replaced with:"+newUrl );
              e.html("@import url("+newUrl+");"); //e.text will be encoded                            
              log.info("new tag (html):"+e);
              numberOfLinksReplaced.incrementAndGet();
          }
          else{
            e.html("@import url("+NOT_FOUND_LINK+");"); //e.text will be encoded
              log.info("No harvest found for:"+resolvedUrl );
              numberOfLinksNotFound.incrementAndGet();
          }
          
        }                   
      }
  }

	
	/*
	 * Only rewrite to new service, no need to resolve until clicked
	 * 
	 */
	public static void rewriteUrlForElement(Document doc, String element, String attribute,  String crawlDate) throws Exception{      
		for (Element e : doc.select(element)) {
			String url = e.attr("abs:"+attribute);
			if (url == null  || url.trim().length()==0){
				continue;
			}    		               
            String urlEncoded=canonicalizeUrl(url);            
			String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+"viewhref?url="+urlEncoded+"&crawlDate="+crawlDate;    			
			e.attr("href",newUrl);    			     		 
		}   		  		
	}

public static String canonicalizeUrl(String url) throws Exception{
  String urlNorm=Normalisation.canonicaliseURL(url); 
  String urlEncoded=URLEncoder.encode(urlNorm, "UTF-8");
  //log.info("after encode1:"+urlEncoded);
  return urlEncoded;
}
	

	public static HashMap<String,String> test(String html,String url) throws Exception{
		HashMap<String,String> imagesSet = new HashMap<String,String>();  // To remove duplicates
		Document doc = Jsoup.parse(html,url); //TODO baseURI?
		//System.out.println(doc);
		return imagesSet;
	}

}
