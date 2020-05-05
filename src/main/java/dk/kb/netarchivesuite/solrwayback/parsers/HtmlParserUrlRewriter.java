package dk.kb.netarchivesuite.solrwayback.parsers;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.kb.netarchivesuite.solrwayback.util.RegexpReplacer;
import dk.kb.netarchivesuite.solrwayback.util.URLAbsoluter;
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
	
	private static final String CSS_IMPORT_PATTERN_STRING =
			"(?s)\\s*@import\\s+(?:url)?[(]?\\s*['\"]?([^'\")]*\\.css[^'\") ]*)['\"]?\\s*[)]?.*";
	private static Pattern  CSS_IMPORT_PATTERN = Pattern.compile(CSS_IMPORT_PATTERN_STRING);

	private static final String CSS_IMPORT_PATTERN_STRING2 =
			"(?s)\\s*@import\\s+(?:url)?[(]?\\s*['\"]?([^'\")]*\\.css[^'\") ]*)['\"]?\\s*[)]?";
	private static Pattern CSS_IMPORT_PATTERN2 = Pattern.compile(CSS_IMPORT_PATTERN_STRING2);

	private static Pattern STYLE_ELEMENT_BACKGROUND_PATTERN = Pattern.compile(
			"background(?:-image)?\\s*:([^;}]*)");
	private static Pattern CSS_URL_PATTERN = Pattern.compile(
			"url\\s*\\(\\s*[\"']?([^)\"']*)[\"']?\\s*\\)");

	//replacing urls that points into the world outside solrwayback because they are never harvested
    private static final String NOT_FOUND_LINK=PropertiesLoader.WAYBACK_BASEURL+"services/notfound/";
	
	public static void main(String[] args) throws Exception{
//		String css= new String(Files.readAllBytes(Paths.get("/home/teg/gamespot.css")));

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
//	      System.exit(1);



	}


	/* CSS can start with the following and need to be url rewritten also.
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
		log.debug("#unique urlset to resolve for arc-url '" + url + "' :" + urlSet.size());

		long resolveMS = -System.currentTimeMillis();
		List<IndexDoc> docs = nearestResolver.findNearestHarvestTime(urlSet, crawlDate);
		resolveMS += System.currentTimeMillis();

		//Rewriting to url_norm, so it can be matched when replacing.
		final HashMap<String, IndexDoc> urlReplaceMap = new HashMap<String,IndexDoc>();
		for (IndexDoc indexDoc: docs){
			urlReplaceMap.put(indexDoc.getUrl_norm(), indexDoc);
		}

        // Replace URLs in the document with URLs for archived versions.
		UnaryOperator<String> rewriterRaw = createTransformer(
				urlReplaceMap, "downloadRaw", "", numberOfLinksReplaced, numberOfLinksNotFound);
        processElement(doc, "img",    "abs:src", rewriterRaw);
        processElement(doc, "embed",  "abs:src", rewriterRaw);
        processElement(doc, "source", "abs:src", rewriterRaw);
		processElement(doc, "script", "abs:src", rewriterRaw);
		processElement(doc, "body",   "abs:background", rewriterRaw);
		processElement(doc, "table",  "abs:background", rewriterRaw);
		processElement(doc, "td",     "abs:background", rewriterRaw);

		// link elements are mostly used to reference stylesheets, which must be transformed before use
		UnaryOperator<String> rewriterView = createTransformer(
				urlReplaceMap, "view", "", numberOfLinksReplaced, numberOfLinksNotFound);
		processElement(doc, "link", "abs:href", rewriterView);

		// Don't show SolrWayback bar in frames
		UnaryOperator<String> rewriterViewNoBar = createTransformer(
				urlReplaceMap, "view", "&showToolbar=false", numberOfLinksReplaced, numberOfLinksNotFound);
		processElement(doc, "frame",  "abs:src", rewriterViewNoBar);
        processElement(doc, "iframe", "abs:src", rewriterViewNoBar);

		// Links to external resources are not resolved until clicked
		UnaryOperator<String> rewriterRawNoResolve = (sourceURL) ->
				PropertiesLoader.WAYBACK_BASEURL + "services/web/" + crawlDate + "/" + sourceURL;
        processElement(doc, "a",    "abs:href", rewriterRawNoResolve);
        processElement(doc, "area", "abs:href", rewriterRawNoResolve);
        processElement(doc, "form", "abs:action", rewriterRawNoResolve);

        // Multi value elements
        processMultiAttribute(doc, "img", "srcset", rewriterRaw);
        processMultiAttribute(doc, "img", "data-srcset", rewriterRaw);
		processMultiAttribute(doc, "source", "srcset", rewriterRaw);

		// Full content processing
		// TODO: Why the raw rewrite? Shouldn't this be view?
		UnaryOperator<String> rewriterRawAmpersand = (sourceURL) -> {
			sourceURL = rewriterRaw.apply(sourceURL);
			return sourceURL == null ? null : sourceURL.replace("&", AMPERSAND_REPLACE);
		};
		processElementRegexp(doc, "style", null, rewriterRawAmpersand, CSS_IMPORT_PATTERN2);

		processElementRegexp(doc, "*", "style", rewriterRaw, STYLE_ELEMENT_BACKGROUND_PATTERN, CSS_URL_PATTERN);

		log.info(String.format(
				"replaceLinks('%s', %s): Links unique=%d, replaced=%d, not_found=%d. " +
				"Time total=%dms, nearest_query=%dms",
	            url, crawlDate, urlSet.size(), numberOfLinksReplaced.get(), numberOfLinksNotFound.get(),
				System.currentTimeMillis()-start, resolveMS));

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
            Map<String, IndexDoc> urlReplaceMap, String type, String extraParams,
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

        processElement(doc, "img",    "abs:src", collector);
        processElement(doc, "embed",  "abs:src", collector);
        processElement(doc, "source", "abs:src", collector);
		processElement(doc, "script", "abs:src", collector);
		processElement(doc, "body",   "abs:background", collector);
		processElement(doc, "td",     "abs:background", collector);
		processElement(doc, "table",  "abs:background", collector);
		processElement(doc, "area",   "abs:href", collector); // Why is this collected? It is not replaced later on

		processElement(doc, "link",   "abs:href", collector);

		processElement(doc, "frame",  "abs:src", collector);
        processElement(doc, "iframe", "abs:src", collector);
        
		processMultiAttribute(doc, "img", "srcset", collector);
		processMultiAttribute(doc, "img", "data-srcset", collector);
		processMultiAttribute(doc, "source", "srcset", collector);

		processElementRegexp(doc, "style", null, collector, CSS_IMPORT_PATTERN2);
		processElementRegexp(doc, "*", "style", collector, STYLE_ELEMENT_BACKGROUND_PATTERN, CSS_URL_PATTERN);
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

	/**
	 * Iterates all matching element+attribute, then all outerRegexp {@code .group(1)}-matching content is applied to
	 * innerRegexp and the group(1)-matches from that is sent throught the transformer.
	 * Note1: The content of the matching innerRegexp group 1 is expected to be an URL and will be made absolute before
	 *        being transformed.
	 * Note2: Content returned from transformer will be entity encoded by JSOUP, if attribute is null.
	 *        If an ampersand {@code &} is to remain non-encoded, replace it with {@link #AMPERSAND_REPLACE} in the
	 *        content before returning it.
	 * @param doc         a JSOUP document, representing part on a HTML page.
	 * @param element     an HTML element.
	 * @param attribute   an attribute for the HTML element.
*                    If the attribute is null, the content of the element is used.
	 * @param regexps     the content of the matching nodes will be matched by the first regexp and {@code .group(1)}
	 *                    will be fed to the next regexp and so forth. When there are no more regexps, the content
	 *                    will be processed by transformer.
	 * @param transformer takes the regexp matching content of the attribute and provides the new content.
	 *                    If null is returned, the content will not be changed.
	 */
	public static void processElementRegexp(
			Document doc, String element, String attribute, UnaryOperator<String> transformer, Pattern... regexps) {
		final URLAbsoluter absoluter = new URLAbsoluter(doc.baseUri());
		UnaryOperator<String> processor = url ->
				// TODO: Should canonicalization not be the responsibility of the collector?
				transformer.apply(Normalisation.canonicaliseURL(absoluter.apply(url)));
		for (int i = regexps.length-1 ; i >= 0 ; i--) {
			processor = new RegexpReplacer(regexps[i], processor);
		}
		processElement(doc, element, attribute, processor);
	}

    /**
     * Iterates all matching element+attribute and applies the transformer on the content.
	 * Expects URLs extracted from the attribute to be delivered as absolute by JSOUP.
	 * Note: If the attribute is null, the content of the element will be used. When assigning new content to the
	 *       element, it will be entity escaped.
     * @param doc         a JSOUP document, representing part on a HTML page.
     * @param element     an HTML element.
     * @param attribute   an attribute for the HTML element.
	 *                    If the attribute is null, the content of the element is used.
	 *                    If the attribute is prefixed with {@code abs:}, JSOUP will attempt to make is an absolute URL.
     * @param transformer takes the content of the attribute and provides the new content.
     *                    If null is returned, the content will not be changed.
     */
	public static void processElement(
	        Document doc, String element, String attribute, UnaryOperator<String> transformer) {
		for (Element e : doc.select(element)) {
			String content = attribute != null && !attribute.isEmpty() ?
					e.attr(attribute) :
					e.data();
			if (content == null  || content.trim().isEmpty()){
				continue;
			}
            String newContent = transformer.apply(content);
			if (newContent != null && !newContent.equals(content)) {
				if (attribute != null && !attribute.isEmpty()) {
					e.attr(attribute.replaceFirst("abs:", ""), newContent);
				} else {
					e.html(newContent);
				}
            }
		}
	}

    /**
     * Iterates all matching element+attribute, splits the content on {@code ,} and subsequently {@code } (space),
	 * applying the transformer on the extracted content.
	 * If the urls in the content are not absolute then {@code doc.baseUri()} is used for making it absolute.
     * @param doc         a JSOUP document, representing part on a HTML page.
     * @param element     an HTML element.
     * @param attribute   an attribute for the HTML element.
     * @param transformer takes the sub-content of the attribute and provides the new content.
     *                    If null is returned, the content will not be changed.
     */
	public static void processMultiAttribute(
	        Document doc, String element, String attribute, UnaryOperator<String> transformer) {
		URLAbsoluter absoluter = new URLAbsoluter(doc.baseUri());
		for (Element e : doc.select(element)) {
			String urlString = e.attr(attribute);
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

				abs = absoluter.apply(abs);

				String newUrl = transformer.apply(abs);
				sb.append(newUrl == null ? abs : newUrl);
				if (tokens.length == 2) {
					sb.append(" ").append(tokens[1]);
				}
			}

			// Replace if changed
			String newURLString = sb.toString();
			if (!newURLString.equals(urlString)) {
				e.attr(attribute.replaceFirst("abs:", ""), newURLString);
			}
		}
	}
	private static final Pattern COMMA_SPLITTER =  Pattern.compile(", *");
	private static final Pattern SPACE_SPLITTER =  Pattern.compile(" +");



	public static HashMap<String,String> test(String html,String urlString) {
		HashMap<String,String> imagesSet = new HashMap<String,String>();  // To remove duplicates
		Document doc = Jsoup.parse(html,urlString); //TODO baseURI?
		//System.out.println(doc);
		return imagesSet;
	}

}
