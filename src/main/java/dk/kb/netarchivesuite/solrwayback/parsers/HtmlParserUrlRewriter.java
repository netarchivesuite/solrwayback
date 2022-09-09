package dk.kb.netarchivesuite.solrwayback.parsers;

import java.net.URL;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.kb.netarchivesuite.solrwayback.util.CountingMap;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import dk.kb.netarchivesuite.solrwayback.util.RegexpReplacer;
import dk.kb.netarchivesuite.solrwayback.util.URLAbsoluter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;

// TODO: Support https://www.w3schools.com/TAGs/tag_base.asp
// TODO: Refactor to extend RewriterBase for better re-use
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
    public static final String NOT_FOUND_LINK = PropertiesLoader.WAYBACK_BASEURL + "services/notfound/";

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

		System.out.println(" ");
		System.exit(1);
*/
//	      System.exit(1);



	}


	/* CSS can start with the following and need to be url rewritten also.
	 * @import "mystyle.css";
	 * @import url(slidearrows.css);
	 * @import url(shadow_frames.css) print;
	 */
	// TODO: Switch to RegexpReplacer for this
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
				IndexDoc indexDoc = NetarchiveSolrClient.getInstance().findClosestHarvestTimeForUrl(resolvedUrl, arc.getWaybackDate());
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
	public static ParseResult replaceLinks(ArcEntry arc) throws Exception{
		final long startMS = System.currentTimeMillis();
		return replaceLinks(
				arc.getBinaryContentAsStringUnCompressed(), arc.getUrl(), arc.getCrawlDate(),
				(urls, timeStamp) -> NetarchiveSolrClient.getInstance().findNearestHarvestTimeForMultipleUrlsFewFields(urls, timeStamp),
				startMS);
	}

	/**
	 * Replaces links and other URLs with the archived versions that are closest to the links in the html in time.
	 * @param html the web page to use as basis for replacing links.
	 * @param url the URL for the html (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param nearestResolver handles url -> archived-resource lookups based on smallest temporal distance to crawlDate.
	 * @throws Exception if link resolving failed.
	 */
	public static ParseResult replaceLinks(
			String html, String url, String crawlDate, NearestResolver nearestResolver) throws Exception {
		return replaceLinks(html, url, crawlDate, nearestResolver, System.currentTimeMillis());
	}
	// startMS used to measure total time, including resolving of the HTML
	private static ParseResult replaceLinks(
			String html, String url, String crawlDate,
			NearestResolver nearestResolver, long startMS) throws Exception {
		final long preReplaceMS = System.currentTimeMillis()-startMS;
		long replaceMS = -System.currentTimeMillis();

		final String waybackDate = DateUtils.convertUtcDate2WaybackDate(crawlDate);
		Document doc = Jsoup.parse(html, url);

		// Collect URLs and resolve archived versions for them 
		Set<String> urlSet = getUrlResourcesForHtmlPage(doc, url);
		log.debug("#unique urlset to resolve for arc-url '" + url + "' :" + urlSet.size());

		long resolveMS = -System.currentTimeMillis();
		List<IndexDocShort> docs = nearestResolver.findNearestHarvestTime(urlSet, crawlDate);
		resolveMS += System.currentTimeMillis();

		// Rewriting to url_norm, so it can be matched when replacing.
		final CountingMap<String, IndexDocShort> urlReplaceMap = new CountingMap<>();
		for (IndexDocShort indexDoc: docs){
			urlReplaceMap.put(indexDoc.getUrl_norm(), indexDoc);
		}

        // Replace URLs in the document with URLs for archived versions.
		UnaryOperator<String> rewriterRaw = createTransformer(
				urlReplaceMap, "downloadRaw", "");
        processElement(doc, "img",    "abs:src", rewriterRaw);
        processElement(doc, "embed",  "abs:src", rewriterRaw);
        processElement(doc, "source", "abs:src", rewriterRaw);
		processElement(doc, "script", "abs:src", rewriterRaw);
		processElement(doc, "body",   "abs:background", rewriterRaw);
		processElement(doc, "table",  "abs:background", rewriterRaw);
		processElement(doc, "td",     "abs:background", rewriterRaw);

		// link elements are mostly used to reference stylesheets, which must be transformed before use
		UnaryOperator<String> rewriterView = createTransformer(
				urlReplaceMap, "view", "");
		processElement(doc, "link", "abs:href", rewriterView);

		// Don't show SolrWayback bar in frames
		UnaryOperator<String> rewriterViewNoBar = createTransformer(
				urlReplaceMap, "view", "&showToolbar=false");
		processElement(doc, "frame",  "abs:src", rewriterViewNoBar);
        processElement(doc, "iframe", "abs:src", rewriterViewNoBar);

		// Links to external resources are not resolved until clicked
		UnaryOperator<String> rewriterRawNoResolve = (sourceURL) ->
				PropertiesLoader.WAYBACK_BASEURL + "services/web/" + waybackDate + "/" + sourceURL;
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
		// TODO: Move this to ScriptRewriter
		processElementRegexp(doc, "style", null, rewriterRawAmpersand, CSS_IMPORT_PATTERN2);

		processElementRegexp(doc, "*", "style", rewriterRaw, STYLE_ELEMENT_BACKGROUND_PATTERN, CSS_URL_PATTERN);

		// Script content is handled by ScriptRewriter
		rewriteInlineScripts(doc, crawlDate, urlReplaceMap);

		replaceMS += System.currentTimeMillis();
		/*
		log.debug(String.format(
				"replaceLinks('%s', %s): Links unique=%d, replaced=%d, not_found=%d. " +
				"Time total=%dms (resolveHTML=%dms, analysis+adjustment=%dms, resolveResources=%dms)",
	            url, crawlDate, urlSet.size(), urlReplaceMap.getFoundCount(), urlReplaceMap.getFailCount(),
				preReplaceMS+replaceMS, preReplaceMS, replaceMS-resolveMS, resolveMS));
        */

		String html_output= doc.toString();
		html_output = RewriterBase.unescape(html_output);

		ParseResult res = new ParseResult();
		res.setReplaced(html_output);
		res.setNumberOfLinksReplaced(urlReplaceMap.getFoundCount());
		res.setNumberOfLinksNotFound(urlReplaceMap.getFailCount());
		return res;
	}

	private static void rewriteInlineScripts(
			Document doc, String crawlDate, Map<String, IndexDocShort> urlReplaceMap) {
		processElement(doc, "script", null, (content) -> {
			try {
				ParseResult scriptResult = ScriptRewriter.getInstance().replaceLinks(
						content, doc.baseUri(), crawlDate, urlReplaceMap, RewriterBase.PACKAGING.inline, true);
				return scriptResult.getReplaced();
			} catch (Exception e) {
				log.warn("Exception while parsing inline script for " + doc.baseUri() + " " + crawlDate, e);
				return content;
			}
		});
	}

	/**
	 * Generic transformer creator that normalises the incoming URL and return a link to an archived version,
	 * if such a version exists. Else a {@code notfound} link is returned.
	 * If the URL us a {@code data:}-URL, it is returned unmodified.
	 * @param urlReplaceMap         a map of archived versions for normalised URLs on the page.
	 * @param type                  view or downloadRAW.
	 * @param extraParams           optional extra parameters for the URL to return.
	 * @return an URL to an archived version of the resource that the URL designates or a {@code notfound} URL.
	 */
	private static UnaryOperator<String> createTransformer(
            Map<String, IndexDocShort> urlReplaceMap, String type, String extraParams) {
        return (String sourceURL) -> {
			    if (sourceURL.startsWith("data:")) {
					return sourceURL;
				}
                sourceURL =  sourceURL.replace("/../", "/");

                IndexDocShort indexDoc = urlReplaceMap.get(Normalisation.canonicaliseURL(sourceURL));
                if (indexDoc != null){
                    return PropertiesLoader.WAYBACK_BASEURL + "services/" + type +
                           "?source_file_path=" + indexDoc.getSource_file_path() +
                           "&offset=" + indexDoc.getOffset() +
                           (extraParams == null ? "" : extraParams);
                }
                log.debug("No harvest found for: {}", sourceURL);
                return NOT_FOUND_LINK;
            };
    }

    /**
     * Collect URLs for resources on the page, intended for later replacement with links to archived versions.
     * @param doc a JSOUP document.
     * @param baseURL baseURL for the web page, used for resolving relative URLs.
     * @return a Set of URLs found on the page.
     */
	public static HashSet<String> getUrlResourcesForHtmlPage(Document doc, String baseURL) {
		URLAbsoluter absoluter = new URLAbsoluter(baseURL, true);
        final HashSet<String> urlSet = new HashSet<>();
        UnaryOperator<String> collector = (String sourceURL) -> {
            urlSet.add(absoluter.apply(sourceURL));
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

		// Get URLs from the ScriptRewriter
		processElement(doc, "script", null, (content) -> {
			urlSet.addAll(ScriptRewriter.getInstance().getResourceURLs(content, baseURL));
			return null;
		});

        return urlSet;
	}



	public static String generatePwid(ArcEntry arc) throws Exception{

      long start = System.currentTimeMillis();
      String html = new String(arc.getBinary(),arc.getContentEncoding());
      String url=arc.getUrl();

       String collectionName = PropertiesLoader.PID_COLLECTION_NAME;
      Document doc = Jsoup.parse(html,url);

     
       HashSet<String> urlSet =  getUrlResourcesForHtmlPage(doc, url);
           
      log.info("#unique urlset to resolve:"+urlSet.size());

      ArrayList<IndexDocShort> docs = NetarchiveSolrClient.getInstance().findNearestHarvestTimeForMultipleUrlsFewFields(urlSet,arc.getCrawlDate());

      StringBuffer buf = new StringBuffer();
      for (IndexDocShort indexDoc: docs){
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


      Document doc = Jsoup.parse(html,url);

      
      HashSet<String> urlSet =  getUrlResourcesForHtmlPage(doc, url);
                 
     return urlSet;
    }


	/**
	 * Resolves instances of documents based on time distance.
	 */
	public interface NearestResolver {
		/**
		 * Locates one instance of each url, as close to timeStamp as possible.
		 * @param urls the URLs to resolve.
		 * @param isoTime a timestamp formatted as {@code YYYY-MM-ddTHH:MM:SSZ}.
		 * @return  IndexDocs for the located URLs containing at least
		 *          {@code url_norm, url, source_file, source_file_offset} for each document.
		 */
		List<IndexDocShort> findNearestHarvestTime(Collection<String> urls, String isoTime) throws Exception;
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
		final URLAbsoluter absoluter = new URLAbsoluter(doc.baseUri(), true);
		UnaryOperator<String> processor = url ->
				// TODO: Should canonicalization not be the responsibility of the collector?
				transformer.apply(absoluter.apply(url));
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
			String content = attribute == null || attribute.isEmpty() ? e.data() : e.attr(attribute);
			if (content == null  || content.trim().isEmpty()){
				continue;
			}
            String newContent = transformer.apply(content);
			if (newContent != null && !newContent.equals(content)) {
				if (attribute == null || attribute.isEmpty()) {
					e.html(newContent.replace("\n", RewriterBase.NEWLINE_PLACEHOLDER));
				} else {
					e.attr(attribute.replaceFirst("abs:", ""), newContent);
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
		URLAbsoluter absoluter = new URLAbsoluter(doc.baseUri(), false);
		processElementRegexp(doc, element, attribute,
							 url ->transformer.apply(absoluter.apply(url)),
							 COMMA_SEPARATED_PATTERN, SPACE_SEPARATED_PATTERN);
	}
	private static final Pattern COMMA_SEPARATED_PATTERN = Pattern.compile("([^,]+),?");
	private static final Pattern SPACE_SEPARATED_PATTERN = Pattern.compile("([^ ]+) ?.*");


}
