package dk.kb.netarchivesuite.solrwayback.parsers;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
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
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;

public class HtmlParserUrlRewriter {

	private static final Logger log = LoggerFactory.getLogger(HtmlParserUrlRewriter.class);
    
	//Problem is jsoup text(...) or html(...) encodes & and for the <style> @import... </style> the HTML urls must not be encoded. (blame HTML standard)
	//So it can not be set using JSOUP and must be replaced after.
	private static final String AMPERSAND_REPLACE="_STYLE_AMPERSAND_REPLACE_";
	
	// private static Pattern backgroundUrlPattern = Pattern.compile(".*background:url\\([\"']?([^\"')]*)[\"']?\\).*"); is this better?
	
	//TODO use for more CSS replacement
	private static Pattern urlPattern = Pattern.compile("[: ,]url\\([\"']?([^\"')]*)[\"']?\\)");
	
	private static Pattern backgroundUrlPattern_OLD = Pattern.compile(".*background:url\\((.*)\\).*");
	
	// See explanation where it is used.
	private static Pattern backgroundUrlPattern = Pattern.compile(".*background(-image)?:\\s*url\\((.*)\\).*");
	
	
	private static final String CSS_IMPORT_PATTERN_STRING = 
			"(?s)\\s*@import\\s+(?:url)?[(]?\\s*['\"]?([^'\")]*\\.css[^'\") ]*)['\"]?\\s*[)]?.*";
	private static Pattern  CSS_IMPORT_PATTERN = Pattern.compile(CSS_IMPORT_PATTERN_STRING);

	//replacing urls that points into the world outside solrwayback because they are never harvested
    private static final String NOT_FOUND_LINK=PropertiesLoader.WAYBACK_BASEURL+"services/notfound/";
	
	public static void main(String[] args) throws Exception{

	   
	  
	  
		  String css1="@import url('gamespot_white-1b5761d5e5bc48746b24cb1d708223cd-blessed1.css?z=1384381230968');#moderation .user-history div.icon div.approved;"+"\n"+
                "@import url(slidearrows.css);\n"+
                "@import url(shadow_frames.css);\n"+
                "body {";



		String css= new String(Files.readAllBytes(Paths.get("/home/teg/gamespot.css")));

//		System.out.println(css);
/*
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
                " <img  src=\"http://belinda.statsbiblioteket.dk:9721/solrwayback/services/downloadRaw?source_file_path=/netarkiv/0122/filedir/285330-279-20180113095114447-00002-kb-prod-har-001.kb.dk.warc.gz&amp;offset=685207223\" srcset=\"http://skraedderiet.dk/wp-content/uploads/2018/01/saks_watermark-001_transparent.jpg 477w, http://skraedderiet.dk/wp-content/uploads/2018/01/saks_watermark-001_transparent-150x150.jpg 150w, http://skraedderiet.dk/wp-content/uploads/2018/01/saks_watermark-001_transparent-298x300.jpg 298w\" sizes=\"(max-width: 235px) 100vw, 235px\">"+              
                "</table><br />  </body>"+
                "</html>";

		HashSet<String> urlSet = new HashSet<String>();
		
		Document doc = Jsoup.parse(html,"http:/test.dk"); //TODO baseURI?
		
//		collectRewriteUrlsForImgSrcset(urlSet,doc);
	     
	       
	       
		
	      
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
		String css = arc.getBinaryContentAsStringUnCompressed();		
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
				 resolvedUrl =  resolvedUrl.replace("/../", "/");
				IndexDoc indexDoc = NetarchiveSolrClient.getInstance().findClosestHarvestTimeForUrl(resolvedUrl, arc.getCrawlDate());		         
				if (indexDoc!=null){    		    			 
					String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path() +"&offset="+indexDoc.getOffset(); 					
					css=css.replace(cssUrl, newUrl);					
				}else{
	                log.info("could not resolved CSS import url:"+resolvedUrl);  
				    css=css.replace(cssUrl, NOT_FOUND_LINK);				
					log.info("CSS @import url not harvested:"+cssUrl);
				}	    		 	    		 
			}
		}
		
		//Replace URL's
		
		return css;
	}

	/**
	 * Extracts the HTML from the ArcEntry and replaces links and other URLs with the archived versions that are
	 * closest to the ArcEntry in time.
	 * @param arc an arc-entry that is expected to be a HTML page.
	 * @return the page with links to archived versions instead of live web version.
	 * @throws Exception if link-resolving failed.
	 */
	public static HtmlParseResult replaceLinks(ArcEntry arc) throws Exception{
		return replaceLinks(
				arc.getBinaryContentAsStringUnCompressed(), arc.getUrl(), arc.getWaybackDate(),
				(urls, timeStamp) -> NetarchiveSolrClient.getInstance().findNearestHarvestTimeForMultipleUrls(urls, timeStamp));
	}

	/**
	 * Replaces links and other URLs with the archived versions that are closest to the links in the html in time.
	 * @param html the web page to use as basis for replacing links.
	 * @param url the URL for the html (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param nearestResolver handles url -> archived-resource lookups based on smallest temporal distance to crawlDate.
	 * @throws Exception if link resolving failed.
	 */
	public static HtmlParseResult replaceLinks(
			String html, String url, String crawlDate, NearestResolver nearestResolver) throws Exception {
        long start = System.currentTimeMillis();
		AtomicInteger numberOfLinksReplaced = new  AtomicInteger();
		AtomicInteger numberOfLinksNotFound = new  AtomicInteger();
		Document doc = Jsoup.parse(html, url);

		// Collect URLs and resolve archived versions for them 
		HashSet<String> urlSet = getUrlResourcesForHtmlPage(doc, url);
		log.info("#unique urlset to resolve for arc-url '" + url + "' :" + urlSet.size());

		List<IndexDoc> docs = nearestResolver.findNearestHarvestTime(urlSet, crawlDate);

		//Rewriting to url_norm, so it can be matched when replacing.
		final HashMap<String, IndexDoc> urlReplaceMap = new HashMap<String,IndexDoc>();
		for (IndexDoc indexDoc: docs){
			urlReplaceMap.put(indexDoc.getUrl_norm(), indexDoc);
		}
        UnaryOperator<String> rewriterRaw = createTransformer(urlReplaceMap, "downloadRaw", "", numberOfLinksReplaced, numberOfLinksNotFound);
        UnaryOperator<String> rewriterView = createTransformer(urlReplaceMap, "view", "", numberOfLinksReplaced, numberOfLinksNotFound);
        UnaryOperator<String> rewriterViewNoBar = createTransformer(urlReplaceMap, "view", "&showToolbar=false", numberOfLinksReplaced, numberOfLinksNotFound);
        UnaryOperator<String> rewriterRawNoResolve = (sourceURL) -> PropertiesLoader.WAYBACK_BASEURL + "services/web/" + crawlDate + "/" + sourceURL;

        // Replace URLs in the document with URLs for archived versions.
        processElement(doc, "img",    "src",        rewriterRaw);
        processElement(doc, "embed",  "src",        rewriterRaw);
        processElement(doc, "source", "src",        rewriterRaw);
        processElement(doc, "body",   "background", rewriterRaw);
        processElement(doc, "script", "src",        rewriterRaw);
		processElement(doc, "td",     "background", rewriterRaw);

		processElement(doc, "link",   "href",       rewriterView);

		// Don't show SolrWayback bar in frames
		processElement(doc, "frame",  "src",        rewriterViewNoBar);
        processElement(doc, "iframe",  "src",       rewriterViewNoBar);

		// This are not resolved until clicked
        processElement(doc, "a",       "href",      rewriterRawNoResolve);
        processElement(doc, "area",    "href",      rewriterRawNoResolve);
        processElement(doc, "form",    "action",    rewriterRawNoResolve);

        // Multi value elements
        processMultiElement(doc, "img",    "srcset",      rewriterRaw);
        processMultiElement(doc, "img",    "data-srcset", rewriterRaw);
		processMultiElement(doc, "source", "srcset",      rewriterRaw);

        // TODO: Consider *#style
		replaceStyleBackground(urlReplaceMap,doc, "a", "style", "downloadRaw",url,  numberOfLinksReplaced,  numberOfLinksNotFound);
		replaceStyleBackground(urlReplaceMap,doc, "div", "style", "downloadRaw",url,  numberOfLinksReplaced,  numberOfLinksNotFound);
		replaceUrlsForStyleImport(urlReplaceMap,doc,"downloadRaw",url ,  numberOfLinksReplaced,  numberOfLinksNotFound);

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

	/**
	 * Generic transformer creator that normalises the incoming URL and return a link to an archived version,
	 * if such a version exists. Else a {@code notfound} link is returned.
	 * @param urlReplaceMap         a map of archived versions for normalised URLs on the page.
	 * @param type                  view or downloadRAW.
	 * @param extraParams           optional extra parameters for the URL to return.
	 * @param numberOfLinksReplaced incremented with 1 if the incoming URL is matched in urlReplaceMap.
	 * @param numberOfLinksNotFound incremented with 1 if the incoming URL is not matched in urlReplaceMap.
	 * @return an URL to an archived version of the resource that the URL designates or a {@code notfound} URL.
	 */
	private static UnaryOperator<String> createTransformer(
            HashMap<String, IndexDoc> urlReplaceMap, String type, String extraParams,
            AtomicInteger numberOfLinksReplaced, AtomicInteger numberOfLinksNotFound) {
        return (String sourceURL) -> {
                sourceURL =  sourceURL.replace("/../", "/");
    
                IndexDoc indexDoc = urlReplaceMap.get(Normalisation.canonicaliseURL(sourceURL));
                if (indexDoc != null){
                    numberOfLinksReplaced.getAndIncrement();
                    return PropertiesLoader.WAYBACK_BASEURL + "services/" + type +
                           "?source_file_path=" + indexDoc.getSource_file_path() +
                           "&offset=" + indexDoc.getOffset() +
                           (extraParams == null ? "" : extraParams);
                }
                log.info("No harvest found for:"+sourceURL);
                numberOfLinksNotFound.getAndIncrement();;
                return NOT_FOUND_LINK;
            };
    }


    /**
     * Collect URLs for resources on the page, intended for later replacement with links to archived versions.
     * @param doc a JSOUP document.
     * @param url baseURL for the web page, used for resolving relative URLs. 
     * @return a Set of URLs found on the page.
     * @throws Exception if the content could not be processed.
     */
	public static HashSet<String> getUrlResourcesForHtmlPage( Document doc, String url) throws Exception {
        final HashSet<String> urlSet = new HashSet<>();
        UnaryOperator<String> collector = (String sourceURL) -> {
            urlSet.add(Normalisation.canonicaliseURL(sourceURL));
            return null; // We don't want any changes when collecting
        };

        processElement(doc, "img",    "src",        collector);
        processElement(doc, "embed",  "src",        collector);
        processElement(doc, "source", "src",        collector);
        processElement(doc, "body",   "background", collector);
        processElement(doc, "script", "src",        collector);
		processElement(doc, "td",     "background", collector);
		processElement(doc, "area",   "href",       collector); // Why is this collected? It is not replaced later on

		processElement(doc, "link",   "href",       collector);

		processElement(doc, "frame",  "src",        collector);
        processElement(doc, "iframe", "src",        collector);
        
		processMultiElement(doc, "img",    "srcset",      collector);
		processMultiElement(doc, "img",    "data-srcset", collector);
		processMultiElement(doc, "source", "srcset",      collector);

        collectStyleBackgroundRewrite(urlSet , doc, "a", "style",url);
        collectStyleBackgroundRewrite(urlSet , doc, "div", "style",url);
        collectRewriteUrlsForStyleImport(urlSet, doc,url);
        return urlSet;
	}



	/**
	 * Resolves instances of documents based on time distance.
	 */
	public interface NearestResolver {
		/**
		 * Locates one instance of each url, as close to timeStamp as possible.
		 * @param urls the URLs to resolve.
		 * @param timeStamp a timestamp formatted as {@code TODO: state this}
		 * @return  IndexDocs for the located URLs containing at least
		 *          {@code url_norm, url, source_file, source_file_offset} for each document.
		 */
		List<IndexDoc> findNearestHarvestTime(Collection<String> urls, String timeStamp) throws Exception;
	}

	public static String generatePwid(ArcEntry arc) throws Exception{

      long start = System.currentTimeMillis();
      String html = new String(arc.getBinary(),arc.getContentEncoding());
      String url=arc.getUrl();

       String collectionName = PropertiesLoader.PID_COLLECTION_NAME;
      Document doc = Jsoup.parse(html,url); //TODO baseURI?

     
       HashSet<String> urlSet =  getUrlResourcesForHtmlPage(doc, url);
           
      log.info("#unique urlset to resolve:"+urlSet.size());

      ArrayList<IndexDoc> docs = NetarchiveSolrClient.getInstance().findNearestHarvestTimeForMultipleUrls(urlSet,arc.getCrawlDate());

      StringBuffer buf = new StringBuffer();
      for (IndexDoc indexDoc: docs){
          buf.append("<part>\n");        
          buf.append("urn:pwid:"+collectionName+":"+indexDoc.getCrawlDate()+":part:"+indexDoc.getUrl() +"\n");
          buf.append("</part>\n");
          //pwid:netarkivet.dk:time:part:url      
      }
     return buf.toString();
	}

  
	public static HashSet<String> getResourceLinksForHtmlFromArc(ArcEntry arc) throws Exception{

      String html = arc.getBinaryContentAsStringUnCompressed();

      String url=arc.getUrl();


      Document doc = Jsoup.parse(html,url); //TODO baseURI?

      
      HashSet<String> urlSet =  getUrlResourcesForHtmlPage(doc, url);
                 
     return urlSet;
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
				resolvedUrl =  resolvedUrl.replace("/../", "/");
				IndexDoc indexDoc = map.get(Normalisation.canonicaliseURL(resolvedUrl));   
				if (indexDoc!=null){    		    			 
					String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+type+"?source_file_path="+indexDoc.getSource_file_path() +"&offset="+indexDoc.getOffset();    			     		
					String styleFixed=style.replaceAll(urlUnresolved,newUrl);    			     
					e.attr(attribute,styleFixed); 
				     numberOfLinksReplaced.getAndIncrement();
				}
				else{
				  String styleFixed=style.replaceAll(urlUnresolved,NOT_FOUND_LINK);                    
                  e.attr(attribute,styleFixed); 				  			     
                  log.info("No harvest found for(style):"+resolvedUrl);
                  numberOfLinksNotFound.getAndIncrement();
				}


			}

		}   }

	/*
	 * For stupid HTML standard 
	 * <a href="test" style="background:url(img/homeico.png) no-repeat ; width:90px; margin:0px 0px 0px 16px;">
	 * 
	 */
	public static void collectStyleBackgroundRewrite(HashSet<String> set,Document doc,String element, String attribute ,  String baseUrl) throws Exception {

		for (Element e : doc.select(element)) {

			String style = e.attr(attribute);

			if (style == null  || style.trim().length()==0){
				continue;
			}    		     		 
			String unResolvedUrl = getStyleMatch(style);   		   		

			if (unResolvedUrl != null){
	           unResolvedUrl =   unResolvedUrl.replace("/../", "/");
			  URL base = new URL(baseUrl);
				URL resolvedUrl = new URL( base , unResolvedUrl);			
				set.add(Normalisation.canonicaliseURL(resolvedUrl.toString()));
			}

		}
	}

	/*
	 * 
	 * background:url(img/homeico.png) no-repeat ; width:90px
	 * result:  ... ->  img/homeico.png
	 * 
	 * For div-tag:
	 * background-image:url('https://www.proscenium.dk/wp-content/uploads/2018/12/StatensKunstfond-PR-300x169.jpg');
	 * result: https://www.proscenium.dk/wp-content/uploads/2018/12/StatensKunstfond-PR-300x169.jpg
	 * 
	 * Dont know if " is allowed instead of '
	 */
	private static String getStyleMatch(String style){
		//log.info("matching style:"+style);
	  Matcher m = backgroundUrlPattern.matcher(style);
		if (m.matches()){

		  String url= m.group(2); 
		  if (url.startsWith("'") && url.endsWith("'")){
           url=url.substring(1, url.length()-1);		    
		  }
	      //log.info("style found:"+url);
          url =   url.replace("/../", "/");
          return url;
		}
        //log.info("style not found");
		return null;    	
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
                            
              set.add(Normalisation.canonicaliseURL(resolvedUrl.toString()));
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
          

          IndexDoc indexDoc = map.get(Normalisation.canonicaliseURL(resolvedUrl.toString()));   
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


    /**
     * Iterates all matching element+attribute and applies the transformer on the content.
	 * Expects URLs extracted from the attribute to be delivered as absolute by JSOUP.
     * @param doc         a JSOUP document, representing part on a HTML page.
     * @param element     an HTML element.
     * @param attribute   an attribute for the HTML element.
     * @param transformer takes the content of the attribute and provides the new content.
     *                    If null is returned, the content will not be changed.
     */
	public static void processElement(
	        Document doc, String element, String attribute, UnaryOperator<String> transformer) {
		for (Element e : doc.select(element)) {
			String url = e.attr("abs:"+attribute);
			if (url == null  || url.trim().length()==0){
				continue;
			}
       			//String newUrl=PropertiesLoader.WAYBACK_BASEURL+"services/"+"viewhref?url="+urlEncoded+"&crawlDate="+crawlDate;
           //Format is: ?waybackdata=20080331193533/http://ekstrabladet.dk/112/article990050.ece
            String newUrl = transformer.apply(url);
			if (newUrl != null && !newUrl.equals(url)) {
                e.attr(attribute, newUrl);
            }
		}
	}

    /**
     * Iterates all matching element+attribute, splits the content on {@code ,} and subsequently {@code } (space),
	 * applying the transformer on the extracted content.
	 * If the content does not match {@link #IS_ABSOLUTE_URL} then {@code doc.baseUri()} is used for making it absolute.
     * @param doc         a JSOUP document, representing part on a HTML page.
     * @param element     an HTML element.
     * @param attribute   an attribute for the HTML element.
     * @param transformer takes the sub-content of the attribute and provides the new content.
     *                    If null is returned, the content will not be changed.
     */
	public static void processMultiElement(
	        Document doc, String element, String attribute, UnaryOperator<String> transformer) {
		URL baseURL = null;
		try {
			baseURL = new URL(doc.baseUri());
		} catch (MalformedURLException e) {
			log.debug("processMultiElement: Unable to parse baseURL '" + doc.baseUri() + "', unable to use baseURL " +
					  "to create absolute URLs from relative URLs");
		}
		for (Element e : doc.select(element)) {
			String urlString = e.attr("abs:"+attribute);
			if (urlString == null || urlString.isEmpty()){
				continue;
			}
			// "foo.jpg 1x, bar.jpg 2x"

			StringBuilder sb = new StringBuilder();
			for (String urlPair: COMMA_SPLITTER.split(urlString)) {
				if (sb.length() != 0) {
					sb.append(", ");
				}

				// "foo.jpg 1x"
				String[] tokens = SPACE_SPLITTER.split(urlPair, 2);
				String abs = tokens[0].trim().replace("/../", "/");
				if (abs.isEmpty()) {
					sb.append(urlPair);
					continue;
				}

				// "foo.jpg"
				// Ensure the URL is absolute

				try {
					if (baseURL != null && !IS_ABSOLUTE_URL.matcher(abs).matches()) {
						abs = new URL(baseURL, abs).toString();
					}
				} catch (MalformedURLException ex) {
					log.debug("processMultiElement: Unable to create an absolute URL using new URL('" + baseURL + "', '" +
							  abs + "'), the problematic URL will be passed as-is");
				}

				String newUrl = transformer.apply(abs);
				sb.append(newUrl == null ? abs : newUrl);
				if (tokens.length == 2) {
					sb.append(" ").append(tokens[1]);
				}
			}

			// Replace if changed
			String newURLString = sb.toString();
			if (!newURLString.equals(urlString)) {
				e.attr(attribute, newURLString);
			}
		}
	}
	private static final Pattern IS_ABSOLUTE_URL = Pattern.compile("^https?:");
	private static final Pattern COMMA_SPLITTER =  Pattern.compile(", *");
	private static final Pattern SPACE_SPLITTER =  Pattern.compile(" +");


	public static HashMap<String,String> test(String html,String urlString) {
		HashMap<String,String> imagesSet = new HashMap<String,String>();  // To remove duplicates
		Document doc = Jsoup.parse(html,urlString); //TODO baseURI?
		//System.out.println(doc);
		return imagesSet;
	}

}
