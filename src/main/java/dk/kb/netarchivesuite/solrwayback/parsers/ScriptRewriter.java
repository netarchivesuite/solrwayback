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

import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.util.RegexpReplacer;
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
			"(?s)\"(?:url|playable_url_dash)\"\\s*:\\s*\"([^\"]+)\"");

	@Override
	protected PACKAGING getDefaultPackaging() {
		return PACKAGING.identity;
	}

	@Override
	public String replaceLinks(String content, String baseURL, String crawlDate, Map<String, IndexDoc> urlMap) {
		UnaryOperator<String> rawURLTransformer =
				createURLTransformer(baseURL, true, SOLRWAYBACK_SERVICE.downloadRaw, null, urlMap);
		UnaryOperator<String> rawProcessor = wrapMultiRegexp(
				url -> rawURLTransformer.apply(unescapeURL(url)),
				JSON_KEY_PATTERN);
		return rawProcessor.apply(content);
	}

	@Override
	public Set<String> getResourceURLs(String content) {
		final Set<String> urls = new HashSet<>();
		UnaryOperator<String> collector = createProcessorChain(
				url -> {
					urls.add(unescapeURL(url));
					return null;
				});
		collector.apply(content);
		return urls;
	}

	private static String unescapeURL(String url) {
		return SLASH_PATTERN.matcher(url).replaceAll("/");
	}

	private UnaryOperator<String> createProcessorChain(UnaryOperator<String> processor) {
		return wrapMultiRegexp(processor,
							   JSON_KEY_PATTERN);
	}
}
