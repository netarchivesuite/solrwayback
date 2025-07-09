package dk.kb.netarchivesuite.solrwayback.interfaces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adjust the given input WARC path using regexp/pattern. Resolving to files and HTTP(S) URLs are supported.
 *
 * This is the default WARC resolver. All properties are optional and the default is the identity:
 * The input WARC paths are used directly as-is.
 *
 * Optional properties:
 *   {@code warc.file.resolver.class=RewriteLocationResolver}
 *   {@code warc.file.resolver.parameters.path.regexp=<regexp for the input>}
 *   {@code warc.file.resolver.parameters.path.replacement=<replacement definition for the regexp result>}
 * The regexp and the pattern defaults to {@code .*} and {@code $0} respectively (the identity).
 *
 * Sample config for requesting the WARCs from a remote server by stripping the path and using the filename only:
 *   {@code warc.file.resolver.class=RewriteLocationResolver}
 *   {@code warc.file.resolver.parameters.path.regexp=.*([^/]*)}
 *   {@code warc.file.resolver.parameters.path.replacement=http://example.com/warcstore/$1}
 *
 * This resolver class will be activated by the InitialContextLoader.
 *
 * Note: In order for HTTP(S) to be efficient as a delivery mechanism for WARC content, the server must support
 * HTTP range requests: https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
 * It is not recommended to set readfallback to true as this will result is excessive overhead, unless the
 * backing WARC files are tiny (a few megabytes), if the server does not support range requests.
 */
public class RewriteLocationResolver implements ArcFileLocationResolverInterface {
    private static final Logger log = LoggerFactory.getLogger(RewriteLocationResolver.class);

    public static final String REGEXP_KEY = "path.regexp";
    public static final String REGEXP_DEFAULT = ".*";  // Match everything
    public static final String REPLACEMENT_KEY = "path.replacement";
    public static final String REPLACEMENT_DEFAULT = "$0"; // The full match

    private Pattern regexp = Pattern.compile(REGEXP_DEFAULT);
    private String replacement = REPLACEMENT_DEFAULT;

    public RewriteLocationResolver() { }

    @Override
    public void setParameters(Map<String, String> parameters) {
        setPathPattern(parameters.getOrDefault(REGEXP_KEY, REGEXP_DEFAULT));
        setPathReplacement(parameters.getOrDefault(REPLACEMENT_KEY, REPLACEMENT_DEFAULT));
    }

    public void setPathPattern(String inputPattern) {
        this.regexp = Pattern.compile(inputPattern);
    }

    public void setPathReplacement(String replacement) {
        this.replacement = replacement;
    }

    @Override
    public void initialize() {
        log.info("Initialized " + this);
    }
  
    @Override
    public ArcSource resolveArcFileLocation(String source_file_path) {
        Matcher m = regexp.matcher(source_file_path);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Unable to match '" + source_file_path + "' against the pattern '" + regexp.pattern() + "'");
        }
        String rewritten = m.replaceFirst(replacement);
        log.debug("Rewrote (W)ARCFileLocation '{}' to '{}'", source_file_path, rewritten);
        return ArcSource.create(rewritten);
    }

    @Override
    public String toString() {
        return "RewriteLocationResolver(" +
               "regexp='" + regexp + "'" +
               ", replacement='" + replacement + "'" +
               ')';
    }
}
