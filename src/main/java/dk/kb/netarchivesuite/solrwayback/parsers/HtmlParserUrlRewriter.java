package dk.kb.netarchivesuite.solrwayback.parsers;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.solr.SolrClient;

public class HtmlParserUrlRewriter {

	private static final Logger log = LoggerFactory.getLogger(HtmlParserUrlRewriter.class);

	private static Pattern backgroundUrlPattern = Pattern.compile(".*background:url\\((.*)\\).*");
	private static final String CSS_IMPORT_PATTERN_STRING = 
			"(?s)\\s*@import\\s+(?:url)?[(]?\\s*['\"]?([a-zA-Z0-9_.-]*\\.css)['\"]?\\s*[)]?.*";
	private static Pattern  CSS_IMPORT_PATTERN = Pattern.compile(CSS_IMPORT_PATTERN_STRING);


	public static void main(String[] args) throws Exception{


		  	String css="@import \"mystyle.css\";\n"+
                "@import url(slidearrows.css);\n"+
                "@import url(shadow_frames.css);\n"+
                "body {";

		 

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

		String html="<?xml version=\"1.1\" encoding=\"iso-8859-1\"?>"+
				"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\">"+
				"<head>"+
				"   <title>kimse.rovfisk.dk/katte/</title>"+
				"   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\" />"+         
				"   <link rel=\"stylesheet\" href=\"/style.css\"  type=\"text/css\" media=\"screen\" />"+
				" <link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\"/rss2.php\" />"+
				"</head>"+
				"<body>"+
				" <style type=\"text/css\" media=\"screen\">@import \"//www.dr.dk/drdkGlobal/spot/spotGlobal.css\";</style> "+
				" <style type=\"text/css\" media=\"screen\">@import \"/design/www/global/css/globalPrint.css\";</style> "+


                "<a class=\"toplink\" href=\"test\" class=\"button\" style=\"background:url(img/homeico.png) no-repeat ; width:90px; margin:0px 0px 0px 16px;\">kimse.rovfisk.dk  </a><a class=\"toplink\" href=\"/katte/\">katte / </a><br /><br /><table cellspacing=\"8\"><tr><td></td><td class=\"itemw\"><a href=\"/katte/?browse=DSC00175.JPG\"><img class=\"lo\" src=\"/cache/katte/DSC00175.JPG\" /></a></td>"+
                "<td></td><td class=\"itemw\"><a href=\"/katte/?browse=DSC00209.JPG\"><img class=\"lo\" src=\"/cache/katte/DSC00209.JPG\" /></a></td>"+
                "</table><br />  </body>"+
                "</html>";

		HashSet<String> urlSet = new HashSet<String>();
		System.out.println(html);
		Document doc = Jsoup.parse(html,"http:/test.dk"); //TODO baseURI?
		collectStyleBackgroundRewrite(urlSet,doc, "a", "style", "http:/thomas.dk");


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
		while (result[index].startsWith("@import ") && index <result.length){    		
			String importLine = result[index++];
			log.info("css import found:"+importLine +" bytes:" + Arrays.toString(importLine.getBytes()));

			Matcher m = CSS_IMPORT_PATTERN.matcher(importLine);
			if (m.matches()){
				String cssUrl= m.group(1);		   
				URL base = new URL(url);
				String resolvedUrl = new URL( base ,cssUrl).toString();
				log.info("resolve CSS import url:"+resolvedUrl);		

				IndexDoc indexDoc = SolrClient.getInstance().findClosestHarvestTimeForUrl(resolvedUrl, arc.getCrawlDate());		         
				if (indexDoc!=null){    		    			 
					String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?arcFilePath="+indexDoc.getArc_full() +"&offset="+indexDoc.getOffset(); 
					css=css.replaceFirst(cssUrl, newUrl);
				}else{
					log.info("CSS @import url not harvested:"+cssUrl);
				}	    		 	    		 
			}
		}    	    	    	
		return css;
	}

	public static String replaceLinks(ArcEntry arc) throws Exception{

		long start = System.currentTimeMillis();
		String html = new String(arc.getBinary(),arc.getContentEncoding());
		String url=arc.getUrl();


		Document doc = Jsoup.parse(html,url); //TODO baseURI?

				//List<String> urlList = new ArrayList<String>();
		HashSet<String> urlSet = new HashSet<String>();


		collectRewriteUrlsForElement(urlSet,doc, "area", "href");
		collectRewriteUrlsForElement(urlSet, doc, "img", "src");
		collectRewriteUrlsForElement(urlSet,doc, "body", "background");
		collectRewriteUrlsForElement(urlSet,doc, "link", "href");
		collectRewriteUrlsForElement(urlSet,doc, "script", "src");
		collectRewriteUrlsForElement(urlSet,doc, "td", "background");
		collectStyleBackgroundRewrite(urlSet,doc, "a", "style",url);

		log.info("#unique urlset to resolve:"+urlSet.size());

		ArrayList<IndexDoc> docs = SolrClient.getInstance().findClosetsHarvestTimeForMultipleUrls(urlSet,arc.getCrawlDate());


		HashMap<String,IndexDoc> urlReplaceMap = new HashMap<String,IndexDoc>();
		for (IndexDoc indexDoc: docs){
			urlReplaceMap.put(indexDoc.getUrl(),indexDoc);     		     		 
		}



		replaceUrlForElement(urlReplaceMap,doc, "area", "href", "view");
		replaceUrlForElement(urlReplaceMap,doc, "img", "src", "downloadRaw");
		replaceUrlForElement(urlReplaceMap,doc, "body", "background", "downloadRaw");             	     	 
		replaceUrlForElement(urlReplaceMap,doc, "link", "href", "view");
		replaceUrlForElement(urlReplaceMap,doc, "script", "src", "downloadRaw");
		replaceUrlForElement(urlReplaceMap,doc, "td", "background", "downloadRaw");    	 
		replaceStyleBackground(urlReplaceMap,doc, "a", "style", "downloadRaw",url);


		//This are not resolved until clicket
		rewriteUrlForElement(doc, "a" ,"href",arc.getCrawlDate());
		rewriteUrlForElement(doc, "form" ,"action",arc.getCrawlDate());

		log.info("Number of resolves:"+urlSet.size() +" total time:"+(System.currentTimeMillis()-start));    	 

		return doc.toString();
	}


	public static void rewriteUrlForElement(Document doc,String element, String attribute , String type, String crawlDate) throws Exception{

		for (Element e : doc.select(element)) {
			String url = e.attr("abs:"+attribute);

			if (url == null  || url.trim().length()==0){
				continue;
			}    		     		 
			IndexDoc indexDoc = SolrClient.getInstance().findClosestHarvestTimeForUrl(url, crawlDate);   
			if (indexDoc!=null){     		    		    			
				String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?arcFilePath="+indexDoc.getArc_full() +"&offset="+indexDoc.getOffset();    			 
				e.attr(attribute,newUrl);    			     		 
			}
			else{
				log.info("No harvest found for:"+url);
			}

		}
	}



	public static void replaceUrlForElement( HashMap<String,IndexDoc>  map,Document doc,String element, String attribute , String type) throws Exception{

		for (Element e : doc.select(element)) {    		 
			String url = e.attr("abs:"+attribute);

			if (url == null  || url.trim().length()==0){
				continue;
			}    		     		 
			IndexDoc indexDoc = map.get(url);   
			if (indexDoc!=null){    		    			 
				String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?arcFilePath="+indexDoc.getArc_full() +"&offset="+indexDoc.getOffset();    			 
				e.attr(attribute,newUrl);    			     		 
			}
			else{
				log.info("No harvest found for:"+url);
			}

		}
	}

	public static void replaceStyleBackground( HashMap<String,IndexDoc>  map,Document doc,String element, String attribute , String type, String baseUrl) throws Exception{

		for (Element e : doc.select(element)) {

			String style = e.attr(attribute);

			if (style == null  || style.trim().length()==0){
				continue;
			}    		  

			String urlUnresolved = getStyleMatch(style);   		   		

			if ( urlUnresolved != null){
				URL base = new URL(baseUrl);
				String resolvedUrl = new URL( base ,urlUnresolved).toString();			
				IndexDoc indexDoc = map.get(resolvedUrl);   
				if (indexDoc!=null){    		    			 
					String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?arcFilePath="+indexDoc.getArc_full() +"&offset="+indexDoc.getOffset();    			     		
					String styleFixed=style.replaceAll(urlUnresolved,newUrl);    			     
					e.attr(attribute,styleFixed); 
				}
				else{
					log.info("No harvest found for:"+resolvedUrl);
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
				set.add(resolvedUrl.toString());
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

	public static void collectRewriteUrlsForElement(HashSet<String> set, Document doc,String element, String attribute ) throws Exception{

		for (Element e : doc.select(element)) {
			String url = e.attr("abs:"+attribute);

			if (url == null  || url.trim().length()==0){
				continue;
			}    		     	
			set.add(url);   		    		 		
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
			String urlEncoded=URLEncoder.encode(url, "UTF-8");
			String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+"viewhref?url="+urlEncoded+"&crawlDate="+crawlDate;    			
			e.attr("href",newUrl);    			     		 
		}   		  		
	}



	public static HashMap<String,String> test(String html,String url) throws Exception{

		HashMap<String,String> imagesSet = new HashMap<String,String>();  // To remove duplicates

		Document doc = Jsoup.parse(html,url); //TODO baseURI?

		//System.out.println(doc);

		return imagesSet;
	}





}
