package dk.kb.netarchivesuite.solrwayback.normalise;

import org.apache.commons.logging.LogFactory;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.apache.commons.logging.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String- and URL-normalisation helper class.
 *
 * TODO: It seems that https://github.com/iipc/urlcanon is a much better base
 * for normalisation. That should be incorporated here instead of the
 * AggressiveUrlCanonicalizer and the custom code.
 */
public class NormalisationStandard extends NormalisationAbstract {

    private static Log log = LogFactory.getLog(NormalisationStandard.class);

    private static AggressiveUrlCanonicalizer canon = new AggressiveUrlCanonicalizer();
    private static Pattern WWW_PREFIX = Pattern.compile("([a-z]+://)(?:www[0-9]*|ww2|ww)[.](.+)");

    public static void main(String[] args) {
        System.out.println(canonicaliseURL("http://home4.inet.tele.dk:80/tlas4700/"));
    }

    /**
     * Default and very aggressive normaliser. Shorthand for
     * {@code canonicaliseURL(url, true, true)}.
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
     * If NormaliseType.NORMAL port will also be removed. For NormaliseType.LEGACY port is kept since this is slight better but still bugged playback. 
     * 
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
        // Basic normalisation, as shared with Heritrix, Wayback et al. But not for IA that does not remove port
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

        if (!url.startsWith("http://")){
            url = "http://" + url;
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

        //remove port, but not for legacy normalisation
        if (Normalisation.getType()== Normalisation.NormaliseType.NORMAL) {        
          try {
              url=removePort(url);
          }
          catch(Exception e) {         
              return url; //can also happen for relative urls, so this is expected.
          }
        }
        return url;
    }

    public static String resolveRelative(String url, String relative, boolean normalise) throws IllegalArgumentException {
        try {
            URL rurl = new URL(url);
            String resolved = new URL(rurl, relative).toString();
            return normalise ? canonicaliseURL(resolved) : resolved;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Unable to resolve '%s' relative to '%s'", relative, url), e);
        }
    }

    /**
     * Will remove port from url starting with http or https.
     * If it is a relative url such url such as /books/index.html it will be returned as is. 
     * 
     * @param inputUrl String that must not be null. 
     */
    public static String removePort(String inputUrl) throws MalformedURLException {
        
        //If not http or http return as is
        if (!(inputUrl.toLowerCase().startsWith("http://") || inputUrl.toLowerCase().startsWith("https://"))) {
            return inputUrl;
        }
        
        URL url = new URL(inputUrl);
        int port = url.getPort();

        // If the port is -1 (i.e., not specified), return the URL as-is
        if (port == -1) {
            return inputUrl;
        }

        // Reconstruct URL without the port. Important this cover all cases.
        String cleanedUrl = url.getProtocol() + "://" +
                            url.getHost() +
                            url.getPath();

        if (url.getQuery() != null) {
            cleanedUrl += "?" + url.getQuery();
        }

        if (url.getRef() != null) {
            cleanedUrl += "#" + url.getRef();
        }

        return cleanedUrl;
    }

}
