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

import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Rewrites URLs in scripts to archived versions. Handles both inline and external scripts.
 */
public class ScriptRewriter extends RewriterBase {
    private static Log log = LogFactory.getLog(ScriptRewriter.class);

	// TODO: How about escaped " in the values?
	private static Pattern JSON_KEY_PATTERN = Pattern.compile(
			"(?s)\"(?:uri|url|playable_url_dash|playable_url|playable_url_quality_hd)\"\\s*:\\s*\"([^\"]+)\"");
	private static Pattern JSON_XML_BASEURL_PATTERN = Pattern.compile(
			"(?s)(?:<|\\\\u003[cC]|&lt;)BaseURL(?:>|&gt;)(.+?)(?:<|\\\\u003[cC]|&lt;)\\\\?/BaseURL(?:>|&gt;)");

	private static ScriptRewriter instance = null;
	public static ScriptRewriter getInstance() {
		if (instance == null) {
			instance = new ScriptRewriter();
		}
		return instance;
	}

	@Override
	protected PACKAGING getDefaultPackaging() {
		return PACKAGING.identity;
	}

	@Override
	protected String replaceLinks(String content, String baseURL, String crawlDate, Map<String, IndexDocShort> urlMap) {
		UnaryOperator<String> rawURLTransformer =
				createURLTransformer(baseURL, true, SOLRWAYBACK_SERVICE.downloadRaw, null, urlMap);
		UnaryOperator<String> rawProcessor = createProcessorChain(rawURLTransformer);
		return rawProcessor.apply(content);
	}

	@Override
	public Set<String> getResourceURLs(String content) {
		final Set<String> urls = new HashSet<>();
		UnaryOperator<String> collector = createProcessorChain(
				url -> {
					urls.add(url);
					return null;
				});
		collector.apply(content);
		return urls;
	}

	/**
	 * To avoid parser problems, content in JavaScript often have slashes {@code /} escaped with backslash {@code \/}.
	 * In order to process the content uniformly, it can help to start by unescaping those slashes.
	 * @param content script content.
	 * @return unescaped content.
	 */
	private static String unescape(String content) {
		return SLASH_PATTERN.matcher(content).replaceAll("/");
	}

	/**
	 * Runs the content through all patterns supported by ScriptRewriter, unescapes extracted URLs and runs them
	 * through the processor.
	 * @param processor collects or transforms the URLs.
	 * @return the processed content.
	 */
	private UnaryOperator<String> createProcessorChain(UnaryOperator<String> processor) {
		return wrapIndependentRegexp(url -> processor.apply(unescape(url)),
									 JSON_KEY_PATTERN,
									 JSON_XML_BASEURL_PATTERN);
	}
}
