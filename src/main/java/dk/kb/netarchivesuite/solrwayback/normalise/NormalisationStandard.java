package dk.kb.netarchivesuite.solrwayback.normalise;

import org.apache.commons.logging.LogFactory;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.apache.commons.logging.Log;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String- and URL-normalisation helper class.
 *
 * TODO: It seems that https://github.com/iipc/urlcanon is a much better base for normalisation.
 * That should be incorporated here instead of the AggressiveUrlCanonicalizer and the custom code.
 */
public class NormalisationStandard extends NormalisationAbstract{
    private static Log log = LogFactory.getLog(NormalisationStandard.class );
    
    private static AggressiveUrlCanonicalizer canon = new AggressiveUrlCanonicalizer();
    private static Pattern WWW_PREFIX = Pattern.compile("([a-z]+://)(?:www[0-9]*|ww2|ww)[.](.+)");
    

    /**
     * Default and very aggressive normaliser. Shorthand for {@code canonicaliseURL(url, true, true)}.
     */
    public static String canonicaliseURL(String url) {
        return canonicaliseURL(url, true, true);
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
                
        if (url == null || url.isEmpty()) {
            return url;
        }
        // Basic normalisation, as shared with Heritrix, Wayback et al
        url = canon.canonicalize(url);

        // Protocol: https → http
        url = url.startsWith("https://") ? "http://" + url.substring(8) : url;

     // www. prefix
        if (createUnambiguous) {
            Matcher wwwMatcher = WWW_PREFIX.matcher(url);
            if (wwwMatcher.matches()) {
                url = wwwMatcher.group(1) + wwwMatcher.group(2);
            }
        }
        
        // TODO: Consider if this should only be done if createUnambiguous == true
        // Trailing slashes: http://example.com/foo/ → http://example.com/foo
        while (url.endsWith("/")) { // Trailing slash affects the URL semantics
            url = url.substring(0, url.length() - 1);
        }

        // If the link is domain-only (http://example.com), is _must_ end with slash
        if (DOMAIN_ONLY.matcher(url).matches()) {
            url += "/";
        }

        // Create temporary url with %-fixing and high-order characters represented directly
                          
        byte[] urlBytes = fixEscapeErrorsAndUnescapeHighOrderUTF8(url);
        // Normalise


        // Hex escapes, including faulty hex escape handling:
        // http://example.com/all%2A rosé 10%.html → http://example.com/all*%20rosé%2010%25.html or
        // http://example.com/all%2A rosé 10%.html → http://example.com/all*%20ros%C3%A9%2010%25.html if produceValidURL
        url = escapeUTF8(urlBytes, !allowHighOrder, createUnambiguous);

        return url;
    }


  

    
    public static String resolveRelative(String url, String relative, boolean normalise) throws IllegalArgumentException {
      try {
          URL rurl = new URL(url);
          String resolved = new URL(rurl, relative).toString();
          return normalise ? canonicaliseURL(resolved) : resolved;
      } catch (Exception e) {
          throw new IllegalArgumentException(String.format(
                  "Unable to resolve '%s' relative to '%s'", relative, url), e);
      }
  }

}
