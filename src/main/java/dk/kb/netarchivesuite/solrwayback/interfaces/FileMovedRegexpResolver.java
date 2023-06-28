package dk.kb.netarchivesuite.solrwayback.interfaces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional {@link ArcFileLocationResolverInterface}.
 *
 * This class rewrites WARC file locations based on a regexp.
 * It is used for consolidating multiple WARC collections into one or for moving a subtree of
 * folders with WARCs to another location, e.g.
 * {code /home/harvest/warcs/<WARC-files>} → {@code /netarchive/warcs/<WARC-files>}.

 * To activate it, set this in {@code solrwayback.properties}:
 * <pre>
 warc.file.resolver.class=FileMovedRegexpResolver
 warc.file.resolver.filemovedregexpresolver.regexp=<regexp>
 warc.file.resolver.filemovedregexpresolver.replacement=<replacement>
 </pre>
 *
 * The {@code regexp} is handled by {@link Pattern} while {@code replacement} is used with
 * {@link Matcher#replaceFirst(String)}. Note that {@code $1} designates the content of the
 * first capturing group, {@code $2} designates the second and so on.
 * 
 * To handle the reorganization mentioned above, the configurations would be
 * <pre>
 warc.file.resolver.class=FileMovedRegexpResolver
 warc.file.resolver.filemovedregexpresolver.regexp=/home/harvest/(.*)
 warc.file.resolver.filemovedregexpresolver.replacement=/netarchive/$1
 </pre>
 *
 * This resolver class will be activated by the InitialContextLoader
 */
public class FileMovedRegexpResolver implements ArcFileLocationResolverInterface {
    private static final Logger log = LoggerFactory.getLogger(FileMovedRegexpResolver.class);

    public static final String REGEXP_KEY = "warc.file.resolver.filemovedregexpresolver.regexp";
    public static final String REPLACEMENT_KEY = "warc.file.resolver.filemovedregexpresolver.replacement";

    private Pattern regexp;
    private String replacement;

    /**
     * Empty constructor as per the contract.
     * {@link #setParameters(Map)} must be called before calling {@link #resolveArcFileLocation(String)}.
     */
    public FileMovedRegexpResolver() { }

    @Override
    public void setParameters(Map<String, String> parameters) {
        if (!parameters.containsKey(REGEXP_KEY)) {
            throw new NullPointerException("The key '" + REGEXP_KEY + "' is missing from the configuration");
        }
        if (!parameters.containsKey(REPLACEMENT_KEY)) {
            throw new NullPointerException("The key '" + REPLACEMENT_KEY + "' is missing from the configuration");
        }
        
        regexp = Pattern.compile(parameters.get(REGEXP_KEY));
        replacement = parameters.get(REPLACEMENT_KEY);

        log.info("Initialized file regexp resolver with regexp='{}', pattern='{}'", regexp, replacement);
    }

    @Override
    public void initialize() {
        // Do nothing
    }

    @Override
    public ArcSource resolveArcFileLocation(String source_file_path){
        Matcher m = regexp.matcher(source_file_path);
        if (!m.matches()) {
            log.warn("Unable to match WARC path '{}' with regexp '{}', returning original path", 
                     source_file_path, regexp.pattern());
            return ArcSource.fromFile(source_file_path);
        }
        return ArcSource.fromFile(m.replaceFirst(replacement));
    }
}
