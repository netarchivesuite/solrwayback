/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.util.CountingMap;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import dk.kb.netarchivesuite.solrwayback.util.RegexpReplacer;
import dk.kb.netarchivesuite.solrwayback.util.URLAbsoluter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Base class for rewrites of URLs in content to archived versions.
 *
 * Overall principle is that the content is processes in three steps:
 * 1) First pass collects URLs
 * 2) The collected URLs are recolved to archived versions, if possible
 * 3) Second pass rewrites URLs to their archived versions
 */
public abstract class RewriterBase {
    private static Log log = LogFactory.getLog(RewriterBase.class);

    /**
	 * The packaging of content affects how it will be escaped after its links has been adjusted.
	 */
	public enum PACKAGING {
		/**
		 * No escaping will be performed.
		 */
		identity,
        /**
         * The content is inlined on a HTML page (e.g. {@code <script>foo();</script>}.
		 * Slashes {@code /}, ampersand {@code &}, less than {@code <} etc. will be explicitly preserved.
		 */
		inline,
		/**
         * The content is attribute content (e.g. {@code <body onload="foo();">}.
		 * Slashes {@code /}, less than {@code <} and quotes {@code "} will be escaped.
		 */
		attribute}

	/**
	 * The SolrWayback service to call when content is to be delivered.
 	 */
	public enum SOLRWAYBACK_SERVICE {
		/**
		 * Transform resources during delivery (typically links to HTML pages, CSS and JavaScript).
		 * If the linked resource cannot be located in the archive, {@link SOLRWAYBACK_SERVICE_FALLBACK} is used.
		 */
		view,
		/**
		 * Deliver directly, without any transformation.
		 * If the linked resource cannot be located in the archive, {@link SOLRWAYBACK_SERVICE_FALLBACK} is used.
		 */
		downloadRaw,
		/**
		 * No rewriting of links: Deliver the original link unmodified.
		 */
		identity,
		/**
		 * Rewrite links to their absolute and normalised form.
		 */
		normalised,
		/**
		 * Fail all resolving intentionally, so that the {@link SOLRWAYBACK_SERVICE_FALLBACK} is triggered.
		 */
		fail
	}

	/**
	 * If linked content cannot be resolved in the archive, the given action is performed on the URL.
	 */
	public enum SOLRWAYBACK_SERVICE_FALLBACK {
		/**
		 * Rewrite links to SolrWayback links where the resolving of the resource in the archive is delayed until
		 * the browser activated the link.
		 */
		delay,
		/**
		 * Insert {@link HtmlParserUrlRewriter#NOT_FOUND_LINK} as link for all non-resolvable resources.
		 */
		error
	}


	/**
	 * If no explicit {@link PACKAGING} is stated for the method calls, use this.
 	 * @return
	 */
	protected abstract PACKAGING getDefaultPackaging();

	/**
	 * Extracts the content from the ArcEntry and replaces links and other URLs with the archived versions that are
	 * closest to the ArcEntry in time.
	 * Uses {@link #getDefaultPackaging()} to determine packaging.
	 * @param arc an arc-entry.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws Exception if link-resolving failed.
	 */
    public ParseResult replaceLinks(ArcEntry arc) throws Exception{
    	return replaceLinks(arc, getDefaultPackaging());
   	}
	/**
	 * Extracts the content from the ArcEntry and replaces links and other URLs with the archived versions that are
	 * closest to the ArcEntry in time.
	 * @param arc an arc-entry.
	 * @param packaging how the rewritten content should be escaped before returning.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws Exception if link-resolving failed.
	 */
    public ParseResult replaceLinks(ArcEntry arc, PACKAGING packaging) throws Exception{
    	ParseResult parseResult = new ParseResult();

   		final long startgetContentMS = System.currentTimeMillis();
   		String content = arc.getBinaryContentAsStringUnCompressed();
   		parseResult.addTiming("getContent", System.currentTimeMillis()-startgetContentMS);

		replaceLinks(
				content, arc.getUrl(), arc.getCrawlDate(),
				(urls, timeStamp) -> NetarchiveSolrClient.getInstance().
						findNearestHarvestTimeForMultipleUrlsFewFields(urls, timeStamp),
				parseResult, packaging
		);
		log.info(String.format(
				"replaceLinks.%s(<arc-entry of length %d bytes>, packaging=%s, base='%s', date=%s) completed: %s",
				getClass().getSimpleName(), content.length(), packaging, arc.getUrl(), arc.getCrawlDate(), parseResult));
		return parseResult;
   	}
	/**
	 * Replaces links and other URLs with the archived versions that are closest to the ArcEntry in time.
	 * @param content JavaScript, CSS or similar resource, depending on the extending class.
	 * @param baseURL the URL for the original content location (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param nearestResolver handles url -> archived-resource lookups based on smallest temporal distance to crawlDate.
	 * @param packaging how the content was stated originally. This controls escaping.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws RuntimeException if link-resolving failed.
	 */
	// TODO: Read and apply https://www.w3schools.com/Tags/tag_script.asp
	public ParseResult replaceLinks(
			String content, String baseURL, String crawlDate, HtmlParserUrlRewriter.NearestResolver nearestResolver,
			PACKAGING packaging) {
		ParseResult result;
		try {
			result = replaceLinks(content, baseURL, crawlDate, nearestResolver, new ParseResult(), packaging);
		} catch (Exception e) {
			throw new RuntimeException(String.format(
					"replaceLinks.%s(<content of length %d bytes>, packaging=%s, base='%s', date=%s) failed",
					getClass().getSimpleName(), content.length(), packaging, baseURL, crawlDate), e);
		}
		log.info(String.format(
				"replaceLinks.%s(<content of length %d bytes>, packaging=%s, base='%s', date=%s) completed: %s",
				getClass().getSimpleName(), content.length(), packaging, baseURL, crawlDate, result));
		return result;
	}

	/**
	 * Replaces links and other URLs with the alternatives extracted using {@link #getResourceURLs} and resolved
	 * to archived versions using nearestResolver.
	 * @param content probably JavaScript, CSS or similar.
	 * @param baseURL the URL for the content (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param nearestResolver handles url -> archived-resource lookups based on smallest temporal distance to crawlDate.
	 * @param packaging how the content was stated originally. This controls escaping.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws Exception if the provided urlSet could not be translated to archived versions.
	 */
	public ParseResult replaceLinks(
			String content, String baseURL, String crawlDate, HtmlParserUrlRewriter.NearestResolver nearestResolver,
			ParseResult parseResult, PACKAGING packaging) throws Exception {

		final long startResourcesMS = System.currentTimeMillis();
		Set<String> urlSet = getResourceURLs(content, baseURL);
		parseResult.addTiming("getResourceURLs", System.currentTimeMillis()-startResourcesMS);
		return replaceLinks( content, baseURL, crawlDate, nearestResolver, parseResult, urlSet, packaging);
	}

	/**
	 * Replaces links and other URLs with the alternatives in urlSet.
	 * @param content probably JavaScript, CSS or similar.
	 * @param baseURL the URL for the content (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param nearestResolver handles url -> archived-resource lookups based on smallest temporal distance to crawlDate.
	 * @param urlSet set of extracted resource URLs from content, normally retrieved with {@link #getResourceURLs}.
	 * @param packaging how the content was stated originally. This controls escaping.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws Exception if the provided urlSet could not be translated to archived versions.
	 */
	public ParseResult replaceLinks(
			String content, String baseURL, String crawlDate, HtmlParserUrlRewriter.NearestResolver nearestResolver,
			ParseResult parseResult, Set<String> urlSet, PACKAGING packaging) throws Exception {

		long resolveStartMS = System.currentTimeMillis();
		List<IndexDocShort> docs = nearestResolver.findNearestHarvestTime(urlSet, crawlDate);
		parseResult.addTiming("findNearest", System.currentTimeMillis()-resolveStartMS);

		// We keep track of whether a link resolves or not, so only call get once per URL!
		final CountingMap<String, IndexDocShort> countingUrlReplaceMap = new CountingMap<>();
		for (IndexDocShort indexDoc: docs){
			countingUrlReplaceMap.put(indexDoc.getUrl_norm(), indexDoc);
		}

		long replaceStartMS = System.currentTimeMillis();
		parseResult.setReplaced(replaceLinks(content, baseURL, crawlDate, countingUrlReplaceMap));
		parseResult.addTiming("replaceURLs", System.currentTimeMillis()-replaceStartMS);
		parseResult.addStats(countingUrlReplaceMap.getFoundCount(), countingUrlReplaceMap.getFailCount());

		escapeContent(parseResult, packaging);
		return parseResult;
	}

	/**
	 * Replaces links and other URLs with the alternatives in urlMap.
	 * @param content probably JavaScript, CSS or similar.
	 * @param baseURL the URL for the content (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param urlMap map from resource URLs to archived versions, normally retrieved with {@link #getResourceURLs}.
	 * @param packaging how the content was stated originally. This controls escaping.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws Exception generic Exception.
	 */
	public ParseResult replaceLinks(
			String content, String baseURL, String crawlDate, Map<String, IndexDocShort> urlMap, PACKAGING packaging)
			throws Exception {
		return replaceLinks(content, baseURL, crawlDate, urlMap, packaging, false);
	}

	/**
	 * Replaces links and other URLs with the alternatives in urlMap.
	 * @param content probably JavaScript, CSS or similar.
	 * @param baseURL the URL for the content (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param urlMap map from resource URLs to archived versions, normally retrieved with {@link #getResourceURLs}.
	 * @param packaging how the content was stated originally. This controls escaping.
	 * @param markSpecialChars if true, ampersand and newlines are marked as {@link #AMPERSAND_PLACEHOLDER}
	 *                         and {@link #NEWLINE_PLACEHOLDER}.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws Exception generic Exception.
	 */
	public ParseResult replaceLinks(
			String content, String baseURL, String crawlDate, Map<String, IndexDocShort> urlMap, PACKAGING packaging,
			boolean markSpecialChars) throws Exception {

		ParseResult result = new ParseResult(replaceLinks(content, baseURL, crawlDate, urlMap));
		escapeContent(result, packaging, markSpecialChars);
		return result;
	}

	/**
	 * Escape the given content with regard to the packaging.
	 * @param parseResult holds the content.
	 * @param packaging   how the content is to be represented.
	 */
	public static void escapeContent(ParseResult parseResult, PACKAGING packaging) {
		escapeContent(parseResult, packaging, false);
	}
	
	/**
	 * Escape the given content with regard to the packaging.
	 * @param parseResult holds the content.
	 * @param packaging   how the content is to be represented.
	 * @param markSpecialChars if true, ampersand and newlines are marked as {@link #AMPERSAND_PLACEHOLDER}
	 *                         and {@link #NEWLINE_PLACEHOLDER}.
	 */
	public static void escapeContent(ParseResult parseResult, PACKAGING packaging, boolean markSpecialChars) {
		if (parseResult.getReplaced() == null) {
			return;
		}
		switch (packaging) {
			case inline: {
				parseResult.replace(ESCAPE2_PATTERN, ESCAPE2_PLACEHOLDER);
				parseResult.replace(ESCAPE_SLASH_PATTERN, ESCAPE_SLASH_PLACEHOLDER);
//				parseResult.replace(SLASH_PATTERN, SLASH_REPLACEMENT);
// '<' and '>' unicode-escaping proved to introduce more errors than it fixed, so now we just preserve as-is
				parseResult.replace(LT_PATTERN, LT_PLACEHOLDER);
				parseResult.replace(GT_PATTERN, GT_PLACEHOLDER);
				parseResult.replace(AMPERSAND_PATTERN, AMPERSAND_PLACEHOLDER);
//				parseResult.setReplaced(COMMENT_PATTERN.matcher(parseResult.getReplaced()).replaceAll(COMMENT_REPLACEMENT_ENCODE)); // Must be before SLASH_PATTERN
				break;
			}
			case attribute: {
//				parseResult.setReplaced(SLASH_PATTERN.matcher(parseResult.getReplaced()).replaceAll(SLASH_REPLACEMENT));
				parseResult.replace(ESCAPE_SLASH_PATTERN, ESCAPE_SLASH_PLACEHOLDER);
				parseResult.setReplaced(LT_PATTERN.matcher(parseResult.getReplaced()).replaceAll(LT_REPLACEMENT));
				parseResult.setReplaced(parseResult.getReplaced().replace("\"", "&quot;"));
				break;
			}
			case identity: break;
			default: throw new UnsupportedOperationException("PACKAGING '" + packaging + "' is unsupported");
		}
		if (!markSpecialChars) {
			parseResult.setReplaced(unescape(parseResult.getReplaced()));
		}
	}
	static final Pattern AMPERSAND_PATTERN = Pattern.compile("[&]");
	static final String AMPERSAND_PLACEHOLDER ="_STYLE_AMPERSAND_REPLACE_";

	static final Pattern ESCAPE2_PATTERN = Pattern.compile("[\\\\][\\\\]");
	static final String ESCAPE2_PLACEHOLDER ="_ESCAPE2_REPLACE_";

	static final Pattern ESCAPE_SLASH_PATTERN = Pattern.compile("[\\\\][/]");
	static final String ESCAPE_SLASH_PLACEHOLDER ="_ESCAPE_SLASH_REPLACE_";

	static final Pattern COMMENT_PATTERN = Pattern.compile("(//)(.*)");
	static final String COMMENT_PLACEHOLDER ="_COMMENT_REPLACE_";
	static final String COMMENT_REPLACEMENT_ENCODE ="_COMMENT_REPLACE_$2";

	static final String NEWLINE_PLACEHOLDER = "_REWRITER_NEWLINE_REPLACE_";

	static final Pattern SLASH_PATTERN = Pattern.compile("(\\\\)?(/[^/])");
	static final String SLASH_REPLACEMENT = "\\\\$1$2";

	static Pattern LT_PATTERN = Pattern.compile("<");
	static final String LT_PLACEHOLDER = "_LESS_THAN_";
	static final String LT_REPLACEMENT = "\\\\u003C"; // Why do we need double escape? (Unit tests shows we do)

	static Pattern GT_PATTERN = Pattern.compile(">");
	static final String GT_PLACEHOLDER = "_GREATER_THAN_";
	static final String GT_REPLACEMENT = "\\\\u003E"; // Why do we need double escape? (Unit tests shows we do)

	/**
	 * Takes a previously processed String that contains special markers for strings like {@code //}, {@code &} and
	 * newline and converts them back to their original form.
	 * @param in previously processed String.
	 * @return the String ready for external delivery.
	 */
	public static String unescape(String in) {
		return in.replace(AMPERSAND_PLACEHOLDER, "&").
				replace(NEWLINE_PLACEHOLDER, "\n").
				replace(ESCAPE2_PLACEHOLDER, "\\\\").
				replace(ESCAPE_SLASH_PLACEHOLDER, "\\/").
				replace(COMMENT_PLACEHOLDER, "//").
				replace(LT_PLACEHOLDER, "<").
				replace(GT_PLACEHOLDER, ">");
	}

	/**
	 * Replaces links and other URLs with the alternatives in urlMap.
	 * @param content probably JavaScript, CSS or similar.
	 * @param baseURL the URL for the content (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param urlMap map from resource URLs to archived versions, normally retrieved with {@link #getResourceURLs}.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws Exception generic Exception.
	 */
	protected abstract String replaceLinks(
			String content, String baseURL, String crawlDate, Map<String, IndexDocShort> urlMap) throws Exception;

	/**
	 * Generic transformer creator that absolutes & normalises the incoming URL and return a link to an archived
	 * version, if such a version exists. Else a {@code notfound} link is returned.
	 * @param baseURL       if defined, incoming URLs are made absolute using this.
	 * @param crawlDate     f defined and delayed resolving is used, it will be relative to baseTime.
	 *                      ISO-Format: {@code YYYY-mm-ddTHH:MM:ssZ}.
	 * @param normalise     if true, incoming URLs are normalised using {@link Normalisation#canonicaliseURL(String)}.
	 * @param urlReplaceMap a map of archived versions for normalised URLs on the page.
	 * @param service       the type of SolrWayback service to call when secondary content is to be delivered.
	 * @param extraParams   optional extra parameters for the URL to return.
	 * @return an URL to an archived version of the resource that the URL designates or a {@code notfound} URL.
	 */
	public static UnaryOperator<String> createURLTransformer(
			String baseURL, String crawlDate, boolean normalise,
			SOLRWAYBACK_SERVICE service, SOLRWAYBACK_SERVICE_FALLBACK fallback,
			String extraParams, Map<String, IndexDocShort> urlReplaceMap) {
		final String waybackDate = crawlDate == null ? null : DateUtils.convertUtcDate2WaybackDate(crawlDate);

		if (service == SOLRWAYBACK_SERVICE.identity) {
			return url -> url;
		}
		final URLAbsoluter absoluter = new URLAbsoluter(baseURL, normalise);
        return (String sourceURL) -> {
			if ((sourceURL = absoluter.apply(sourceURL)) == null) {
				return null;
			}

        	if (service == SOLRWAYBACK_SERVICE.normalised || sourceURL.startsWith("data:")) {
        		return sourceURL;
			}

        	if (service != SOLRWAYBACK_SERVICE.fail) { // view or downloadRaw
				IndexDocShort indexDoc = urlReplaceMap.get(sourceURL);
				final String serviceCall;
				switch (service) { // Decouple the service call from the enum.toString as the enum might be renamed
					case view: {
						serviceCall = "view";
						break;
					}
					case downloadRaw: {
						serviceCall = "downloadRaw";
						break;
					}
					default: throw new UnsupportedOperationException(
							"The SOLRWAYBACK_SERVICE " + service + " is not supported yet");
				}
				if (indexDoc != null) {
					return PropertiesLoader.WAYBACK_BASEURL + "services/" + serviceCall +
						   "?source_file_path=" + indexDoc.getSource_file_path() +
						   "&offset=" + indexDoc.getOffset() +
						   (extraParams == null ? "" : extraParams);
				}
				log.info("No harvest found for: '" + sourceURL + "'");
			}
        	switch (fallback) {
				case error: return NOT_FOUND_LINK;
				case delay: return PropertiesLoader.WAYBACK_BASEURL + "services/web/" + waybackDate + "/" + sourceURL;
				default: throw new UnsupportedOperationException(
						"The SOLRWAYBACK_SERVICE_FALLBACK " + fallback + " is not supported yet");
			}
		};
	}
	private static final String NOT_FOUND_LINK = PropertiesLoader.WAYBACK_BASEURL + "services/notfound/";

	/**
	 * Takes zero or more ordered patterns and creates a chain of {@link RegexpReplacer}s based on the patterns,
	 * where each pattern must match the result from the previous pattern, ending in the provided processor.
	 * @param processor the inner handler for the {@code .group(1)}-matches from the patterns.
	 * @param patterns  zero or more patterns, used for building a {@link RegexpReplacer} chain.
	 * @return a {@link RegexpReplacer} chain ending in the provided processor.
	 */
	public static UnaryOperator<String> wrapInnerRegexp(UnaryOperator<String> processor, Pattern... patterns) {
		for (int i = patterns.length-1 ; i >= 0 ; i--) {
			processor = new RegexpReplacer(patterns[i], processor);
		}
		return processor;
	}

	/**
	 * Takes zero or more patterns and creates a list of {@link RegexpReplacer}s based on the patterns.
	 * The input is processed by each entry in the list with calls to processor for each {@code .group(1)}.
	 * @param processor the inner handler for the {@code .group(1)}-matches from the patterns.
	 * @param patterns  zero or more patterns, used for building a {@link RegexpReplacer} list.
	 * @return a compound operator that iterates {@link RegexpReplacer}s and calls processor.
	 */
	public static UnaryOperator<String> wrapIndependentRegexp(
			final UnaryOperator<String> processor, Pattern... patterns) {
		final List<RegexpReplacer> replacers = Arrays.stream(patterns).
				map(pattern -> new RegexpReplacer(pattern, processor)).collect(Collectors.toList());
		return content -> {
			for (RegexpReplacer replacer: replacers) {
				String newContent = replacer.apply(content);
				if (newContent != null) {
					content = newContent;
				}
			}
			return content;
		};
	}

	/**
	 * Extracts all URLs from the RewriterBase supported structure, makes them absolute (using baseURL) and
	 * normalises them.
	 * @param content probably JavaScript, CSS or similar.
	 * @param baseURL the URL for the content on the web, used for deriving absolute URLs from the content.
	 * @return the URLs in the content.
	 */
	public Set<String> getResourceURLs(String content, String baseURL) {
		final URLAbsoluter absoluter = new URLAbsoluter(baseURL, true);
		Set<String> rawURLs = getResourceURLs(content);
		return rawURLs.stream().
				map(absoluter::apply).
				filter(Objects::nonNull).
				collect(Collectors.toSet());
	}

	/**
	 * Extracts all URLs from the RewriterBase supported structure. The URLs are expected to be raw (not made absolute,
	 * not normalised).
	 * @param content probably JavaScript, CSS or similar.
	 * @return the URLs in the content.
	 */
	public abstract Set<String> getResourceURLs(String content);
}
