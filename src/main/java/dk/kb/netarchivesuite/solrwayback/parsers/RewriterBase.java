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

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.util.RegexpReplacer;
import dk.kb.netarchivesuite.solrwayback.util.URLAbsoluter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
		 * Slashes {@code /} and less than {@code <} will be escaped.
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
		 * Transform embedded links.
		 */
		view,
		/**
		 * Deliver directly, without any transformation.
		 */
		downloadRaw
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
				content, packaging, arc.getUrl(), arc.getWaybackDate(),
				(urls, timeStamp) -> NetarchiveSolrClient.getInstance().findNearestHarvestTimeForMultipleUrls(urls, timeStamp),
				parseResult);
		log.info(String.format(
				"replaceLinks.%s(<arc-entry of length %d bytes>, packaging=%s, base='%s', date=%s) completed: %s",
				getClass().getSimpleName(), content.length(), packaging, arc.getUrl(), arc.getWaybackDate(), parseResult));
		return parseResult;
   	}
	/**
	 * Replaces links and other URLs with the archived versions that are closest to the ArcEntry in time.
	 * @param content JavaScript, CSS or similar resource, depending on the extending class.
	 * @param packaging how the content was stated originally. This controls escaping.
	 * @param baseURL the URL for the original content location (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param nearestResolver handles url -> archived-resource lookups based on smallest temporal distance to crawlDate.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws RuntimeException if link-resolving failed.
	 */
	// TODO: Read and apply https://www.w3schools.com/Tags/tag_script.asp
	public ParseResult replaceLinks(
			String content, PACKAGING packaging, String baseURL, String crawlDate,
			HtmlParserUrlRewriter.NearestResolver nearestResolver) {
		ParseResult result;
		try {
			result = replaceLinks(content, packaging, baseURL, crawlDate, nearestResolver, new ParseResult());
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
	 * @param packaging how the content was stated originally. This controls escaping.
	 * @param baseURL the URL for the content (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param nearestResolver handles url -> archived-resource lookups based on smallest temporal distance to crawlDate.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws Exception if the provided urlSet could not be translated to archived versions.
	 */
	public ParseResult replaceLinks(
			String content, PACKAGING packaging, String baseURL, String crawlDate,
			HtmlParserUrlRewriter.NearestResolver nearestResolver, ParseResult parseResult) throws Exception {

		final long startResourcesMS = System.currentTimeMillis();
		Set<String> urlSet = getResourceURLs(content, baseURL);
		parseResult.addTiming("getResourceURLs", System.currentTimeMillis()-startResourcesMS);
		return replaceLinks(content, packaging, baseURL, crawlDate, nearestResolver, parseResult, urlSet);
	}

	/**
	 * Replaces links and other URLs with the alternatives in urlSet.
	 * @param content probably JavaScript, CSS or similar.
	 * @param packaging how the content was stated originally. This controls escaping.
	 * @param baseURL the URL for the content (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param nearestResolver handles url -> archived-resource lookups based on smallest temporal distance to crawlDate.
	 * @param urlSet set of extracted resource URLs from content, normally retrieved with {@link #getResourceURLs}.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws Exception if the provided urlSet could not be translated to archived versions.
	 */
	public ParseResult replaceLinks(
			String content, PACKAGING packaging, String baseURL, String crawlDate,
			HtmlParserUrlRewriter.NearestResolver nearestResolver, ParseResult parseResult, Set<String> urlSet)
			throws Exception {

		long resolveStartMS = System.currentTimeMillis();
		List<IndexDoc> docs = nearestResolver.findNearestHarvestTime(urlSet, crawlDate);
		parseResult.addTiming("findNearest", System.currentTimeMillis()-resolveStartMS);

		// We keep track of whether a link resolves or not, so only call get once per URL!
		final Map<String, IndexDoc> countingUrlReplaceMap = new HashMap<String, IndexDoc>() {
			@Override
			public IndexDoc get(Object o) {
				IndexDoc indexDoc = super.get(o);
				if (indexDoc == null) {
					parseResult.incNotFound();
				} else {
					parseResult.incReplaced();
				}
				return indexDoc;
			}
		};
		for (IndexDoc indexDoc: docs){
			countingUrlReplaceMap.put(indexDoc.getUrl_norm(), indexDoc);
		}

		long replaceStartMS = System.currentTimeMillis();
		parseResult.setReplaced(replaceLinks(content, baseURL, crawlDate, countingUrlReplaceMap));
		parseResult.addTiming("replaceURLs", System.currentTimeMillis()-replaceStartMS);

		escapeContent(parseResult, packaging);
		return parseResult;
	}

	/**
	 * Escape the given content with regard to the packaging.
	 * @param parseResult holds the content.
	 * @param packaging   how the content is to be represented.
	 */
	public static void escapeContent(ParseResult parseResult, PACKAGING packaging) {
		if (parseResult.getReplaced() == null) {
			return;
		}
		switch (packaging) {
			case inline: {
				parseResult.setReplaced(SLASH_PATTERN.matcher(parseResult.getReplaced()).replaceAll(SLASH_REPLACEMENT));
				parseResult.setReplaced(LT_PATTERN.matcher(parseResult.getReplaced()).replaceAll(LT_REPLACEMENT));
				break;
			}
			case attribute: {
				parseResult.setReplaced(SLASH_PATTERN.matcher(parseResult.getReplaced()).replaceAll(SLASH_REPLACEMENT));
				parseResult.setReplaced(LT_PATTERN.matcher(parseResult.getReplaced()).replaceAll(LT_REPLACEMENT));
				parseResult.setReplaced(parseResult.getReplaced().replace("\"", "&quot;"));
				break;
			}
			case identity: break;
			default: throw new UnsupportedOperationException("PACKAGING '" + packaging + "' is unsupported");
		}
		// Always replace special placeholder for &
		parseResult.setReplaced(parseResult.getReplaced().
				replace(AMPERSAND_REPLACE, "&").
				replace(NEWLINE_REPLACE, "\n"));
	}
	static final String AMPERSAND_REPLACE="_STYLE_AMPERSAND_REPLACE_";
	static final String NEWLINE_REPLACE = "_REWRITER_NEWLINE_REPLACE_";

	static final Pattern SLASH_PATTERN = Pattern.compile("(\\\\)?/");
	static final String SLASH_REPLACEMENT = "\\\\/";
	static Pattern LT_PATTERN = Pattern.compile("<");
	static final String LT_REPLACEMENT = "\\u003C";

	/**
	 * Replaces links and other URLs with the alternatives in urlMap.
	 * @param content probably JavaScript, CSS or similar.
	 * @param baseURL the URL for the content (needed for resolving relative links).
	 * @param crawlDate the ideal timestamp for the archived versions to link to.
	 * @param urlMap map from resource URLs to archived versions, normally retrieved with {@link #getResourceURLs}.
	 * @return the content with links to archived versions instead of live web version.
	 * @throws Exception generic Exception.
	 */
	public abstract String replaceLinks(String content, String baseURL, String crawlDate,
										   Map<String, IndexDoc> urlMap) throws Exception;

	/**
	 * Generic transformer creator that absolutes & normalises the incoming URL and return a link to an archived
	 * version, if such a version exists. Else a {@code notfound} link is returned.
	 * @param baseURL       if defined, incoming URLs are made absolute using this.
	 * @param normalise     if true, incoming URLs are normalised using {@link Normalisation#canonicaliseURL(String)}.
	 * @param urlReplaceMap a map of archived versions for normalised URLs on the page.
	 * @param service       the type of SolrWayback service to call when secondary content is to be delivered.
	 * @param extraParams   optional extra parameters for the URL to return.
	 * @return an URL to an archived version of the resource that the URL designates or a {@code notfound} URL.
	 */
	public static UnaryOperator<String> createURLTransformer(
			String baseURL, boolean normalise, SOLRWAYBACK_SERVICE service, String extraParams,
			Map<String, IndexDoc> urlReplaceMap) {
		final URLAbsoluter absoluter = new URLAbsoluter(baseURL, normalise);
        return (String sourceURL) -> {
        	if ((sourceURL = absoluter.apply(sourceURL)) == null) {
				return null;
			}

			IndexDoc indexDoc = urlReplaceMap.get(sourceURL);
			if (indexDoc != null){
				return PropertiesLoader.WAYBACK_BASEURL + "services/" + service +
					   "?source_file_path=" + indexDoc.getSource_file_path() +
					   "&offset=" + indexDoc.getOffset() +
					   (extraParams == null ? "" : extraParams);
			}
			log.info("No harvest found for: '" + sourceURL + "'");
			return NOT_FOUND_LINK;
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
