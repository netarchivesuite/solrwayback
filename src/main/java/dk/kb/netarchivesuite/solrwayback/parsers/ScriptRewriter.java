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

    // ... existing JSON_KEY_PATTERN, JSON_XML_BASEURL_PATTERN, instance, getInstance() unchanged ...

    // --- location / navigation rewriting (JS-source level) ---
    // Rewrites direct references to the unforgeable `location` object so they resolve to
    // `_WB_wombat_location` at runtime instead - a plain, patchable object injected by the
    // wayback toolbar. This is necessary because Location.prototype.href/assign/replace are
    // spec-mandated [LegacyUnforgeable] and cannot be monkey-patched at runtime in a
    // standards-compliant browser (confirmed empirically: Firefox throws
    // "TypeError: can't redefine non-configurable property \"href\"").
    //
    // Order matters: each pattern is applied to the result of the previous one, so
    // window./document./this.location are consumed first, leaving only genuinely bare
    // `location` tokens for the remaining two patterns.
    private static final Pattern LOC_WINDOW_PATTERN     = Pattern.compile("\\bwindow\\.location\\b");
    private static final Pattern LOC_DOCUMENT_PATTERN   = Pattern.compile("\\bdocument\\.location\\b");
    private static final Pattern LOC_THIS_PATTERN       = Pattern.compile("\\bthis\\.location\\b");
    private static final Pattern LOC_BARE_PROP_PATTERN  = Pattern.compile("\\blocation\\.");
    private static final Pattern LOC_BARE_ASSIGN_PATTERN = Pattern.compile("\\blocation\\s*=(?!=)");

    /**
     * Rewrites direct JS-source references to {@code location} (window/document/this/bare) so
     * they resolve to the {@code _WB_wombat_location} shim at runtime instead of the real,
     * unforgeable {@code Location} object. This lets page-jump-style widgets - and any other
     * script performing client-side navigation via {@code location.href = ...} or
     * {@code location.assign(...)} - be redirected back into playback instead of leaking to
     * the live web.
     * <p>
     * Best-effort regex substitution, not a full JS parse - shares pywb's known limitation:
     * it will not catch access via an alias (e.g. {@code var l = window.location; l.href = x}),
     * only direct textual references to {@code location}.
     * @param content JavaScript source.
     * @return content with location references rewritten to the wombat shim.
     */
    public static String rewriteLocationReferences(String content) {
        content = LOC_WINDOW_PATTERN.matcher(content).replaceAll("window._WB_wombat_location");
        content = LOC_DOCUMENT_PATTERN.matcher(content).replaceAll("document._WB_wombat_location");
        content = LOC_THIS_PATTERN.matcher(content).replaceAll("this._WB_wombat_location");
        content = LOC_BARE_PROP_PATTERN.matcher(content).replaceAll("_WB_wombat_location.");
        content = LOC_BARE_ASSIGN_PATTERN.matcher(content).replaceAll("_WB_wombat_location.href =");
        return content;
    }
    
	// TODO: How about escaped " in the values?
	private static Pattern JSON_KEY_PATTERN = Pattern.compile(
			"(?s)\"?(?:href|uri|url|playable_url_dash|playable_url|playable_url_quality_hd)\"?\\s*[=:]\\s*\"([^\"]+)\"");
	// Look for <BaseUrl>foo</BaseUrl> with different representations of '<' and '>'
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
             createURLTransformer(baseURL, crawlDate, true,
                                 SOLRWAYBACK_SERVICE.fail, SOLRWAYBACK_SERVICE_FALLBACK.delay,
                                 null, urlMap);
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
	public static String unescape(String content) {
		return UNSLASH_PATTERN.matcher(content).replaceAll("/");
	}
	static final Pattern UNSLASH_PATTERN = Pattern.compile("\\\\[/]");

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
