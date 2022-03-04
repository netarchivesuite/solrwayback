package dk.kb.netarchivesuite.solrwayback.interfaces;

import dk.kb.netarchivesuite.solrwayback.util.SkippingHTTPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adjusts the given input using regexp/pattern and expects the result to be a {@code HTTP} or {@code HTTPS} URL.
 * This implementation supports efficient skips, if the server has HTTP Range Request support for type {@code bytes}
 *
 * To activate it, set this in solrwayback.properties:
 *   {@code warc.file.resolver.class=ArcHTTPResolver}
 *   {@code warc.file.resolver.parameters.path.regexp=<regexp for the input>}
 *   {@code warc.file.resolver.parameters.path.replacement=<replacement definition for the regexp result>}
 *   {@code warc.file.resolver.parameters.readfallback=<true if servers without HTTP Range Request support are acceptable>}
 * The regexp and the pattern defaults to {@code .*} and {@code $0} respectively (the identity).
 *
 * Sample config for requesting the WARCs from a remote server by stripping the path and using the filename only:
 *   {@code warc.file.resolver.parameters.path.regexp=.*([^/]*)}
 *   {@code warc.file.resolver.parameters.path.replacement=http://example.com/warcstore/$1}
 *   {@code warc.file.resolver.parameters.readfallback=false}
 *
 * This resolver class will be activated by the InitialContextLoader.
 *
 * Note: In order for HTTP(S) to be efficient as a delivery mechanism for WARC content, the server must support
 * HTTP range requests: https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
 * It is not recommended to set readfallback to true as this will result is excessive overhead, unless the
 * backing WARC files are tiny (a few megabytes), if the server does not support range requests.
 */
public class ArcHTTPResolver implements ArcFileLocationResolverInterface {
    private static final Logger log = LoggerFactory.getLogger(ArcHTTPResolver.class);

    public static final String REGEXP_KEY = "path.regexp";
    public static final String REGEXP_DEFAULT = ".*";
    public static final String REPLACEMENT_KEY = "path.replacement";
    public static final String REPLACEMENT_DEFAULT = "$0";
    public static final String READ_FALLBACK_KEY = "readfallback";
    public static final boolean READ_FALLBACK_DEFAULT = false;

    private Pattern regexp = Pattern.compile(".*"); // Match everything
    private String replacement = "$1";                    // The full match
    private boolean readFallback = false;

    public ArcHTTPResolver() { }

    @Override
    public void setParameters(Map<String, String> parameters) {
        setPathPattern(parameters.getOrDefault(REGEXP_KEY, REGEXP_DEFAULT));
        setPathReplacement(parameters.getOrDefault(REPLACEMENT_KEY, REPLACEMENT_DEFAULT));
        setReadFallback(Boolean.parseBoolean(parameters.getOrDefault(READ_FALLBACK_KEY, Boolean.toString(READ_FALLBACK_DEFAULT))));
    }

    public void setPathPattern(String inputPattern) {
        this.regexp = Pattern.compile(inputPattern);
    }

    public void setPathReplacement(String replacement) {
        this.replacement = replacement;
    }

    public void setReadFallback(boolean readFallback) {
        this.readFallback = readFallback;
    }

    @Override
    public void initialize() {
        log.info("Initialized " + this);
    }
  
    @Override
    public ArcSource resolveArcFileLocation(String source_file_path) {
        Matcher m = regexp.matcher(source_file_path);
        if (!m.matches()) {
            throw new IllegalArgumentException("Unable to match '" + source_file_path + "'");
        }
        String rewritten = m.replaceFirst(replacement);
        if (!rewritten.startsWith("http")) {
            throw new IllegalArgumentException(
                    "The source '" + rewritten + "' derived from '" + source_file_path + "' is not a HTTP URL");
        }
        return makeHTTPSource(rewritten);
    }

    /**
     * Parse the given urlString to an URL and deliver the content as a {@link SkippingHTTPInputStream}.
     * @param urlString the URL to retrieve the content for.
     * @return an InputStream for the resource that supports efficient skipping.
     */
    private ArcSource makeHTTPSource(String urlString) {
        final URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Unable to construct URL from '" + urlString + "'", e);
        }

        return new ArcSource(urlString, () -> {
            try {
                return new SkippingHTTPInputStream(url, readFallback);
            } catch (IOException e) {
                throw new RuntimeException("Unable to open stream for '" + urlString + "'", e);
            }
        });
    }

    @Override
    public String toString() {
        return "ArcHTTPResolver(" +
               "regexp=" + regexp +
               ", replacement='" + replacement + '\'' +
               ", readFallback=" + readFallback +
               ')';
    }
}
