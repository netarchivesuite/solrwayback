package dk.kb.netarchivesuite.solrwayback.normalise;

import org.apache.commons.logging.LogFactory;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;

import java.net.URL;


/**
 * The Legacy normalizer should only be used if the index was build with a version 3.1 or earlier of the warc-indexer
 * The Legacy normalizer will keep www,www1 etc prefixes before domains, but ONLY if the whole url is a domain only link with no path.
 * This is required to match the url_norm in the solr-index that was build with the warc-indexer
 * This is the only difference compared to the normal type normalizer
 * 
 * Examples:
 * http://www.example.com/ -> http://www.example.com/   (www is kept)
 * http://www.example.com/index.html -> http://example.com/index.html (www is removed(
 * 
 * 
 * String- and URL-normalisation helper class.
 *
 * TODO: It seems that https://github.com/iipc/urlcanon is a much better base for normalisation.
 * That should be incorporated here instead of the AggressiveUrlCanonicalizer and the custom code.
 */
public class NormalisationLegacy extends NormalisationAbstract{
    private static Log log = LogFactory.getLog( NormalisationLegacy.class );

    private static AggressiveUrlCanonicalizer canon = new AggressiveUrlCanonicalizer();
   
   
    public static String canonicaliseHost(String host) throws URIException {
        return canon.urlStringToKey(host.trim()).replace("/", "");
    }

    /**
     * Default and very aggressive normaliser. Shorthand for {@code canonicaliseURL(url, true, true)}.
     */
    public static String canonicaliseURL(String url) {
        return canonicaliseURL(url, true, true);
    }

    
    public static String resolveRelative(String url, String relative) throws IllegalArgumentException {
        return resolveRelative(url, relative, true);
    }
    
    
    public static String resolveRelative(String url, String relative, boolean normalise) throws IllegalArgumentException {
        try {
            URL rurl = new URL(url);
            String resolved = new URL(rurl, relative).toString();
            return resolved;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format(
                    "Unable to resolve '%s' relative to '%s'", relative, url), e);
        }
    }

    /**
     * Multi-step URL canonicalization. Besides using the {@link AggressiveUrlCanonicalizer} from wayback.org it
     * normalises https → http,
     * removes trailing slashes (except when the url is to domain-level),
     * fixed %-escape errors
     * Optionally normalises %-escapes.
     * @param allowHighOrder if true, high-order Unicode (> code point 127) are represented without escaping.
     *                       This is technically problematic as URLs should be plain ASCII, but most tools handles
     *                       them fine and they are easier to read.
     * @param createUnambiguous if true, all non-essential %-escapes are normalised to their escaping character.
     *                          e.g. http://example.com/%2A.html → http://example.com/*.html
     *                          If false, valid %-escapes are kept as-is.
     */
    public static String canonicaliseURL(String url, boolean allowHighOrder, boolean createUnambiguous) {
              
        // Basic normalisation, as shared with Heritrix, Wayback et al
        url = canon.canonicalize(url);

        // Protocol: https → http
        url = url.startsWith("https://") ? "http://" + url.substring(8) : url;

        

        // Create temporary url with %-fixing and high-order characters represented directly
        byte[] urlBytes = fixEscapeErrorsAndUnescapeHighOrderUTF8(url);
        // Normalise


        // Hex escapes, including faulty hex escape handling:
        // http://example.com/all%2A rosé 10%.html → http://example.com/all*%20rosé%2010%25.html or
        // http://example.com/all%2A rosé 10%.html → http://example.com/all*%20ros%C3%A9%2010%25.html if produceValidURL
        url = escapeUTF8(urlBytes, !allowHighOrder, createUnambiguous);

        // TODO: Consider if this should only be done if createUnambiguous == true
        // Trailing slashes: http://example.com/foo/ → http://example.com/foo
        while (url.endsWith("/")) { // Trailing slash affects the URL semantics
            url = url.substring(0, url.length() - 1);
        }

        // If the link is domain-only (http://example.com), is _must_ end with slash
        if (DOMAIN_ONLY.matcher(url).matches()) {
            url += "/";
        }

        return url;
    }
  
  


}
