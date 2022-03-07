package dk.kb.netarchivesuite.solrwayback.parsers;

import org.apache.commons.logging.LogFactory;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String- and URL-normalisation helper class.
 *
 * TODO: It seems that https://github.com/iipc/urlcanon is a much better base for normalisation.
 * That should be incorporated here instead of the AggressiveUrlCanonicalizer and the custom code.
 */
public class NormalisationWWWremove {
    private static Log log = LogFactory.getLog( Normalisation.class );

    private static Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static AggressiveUrlCanonicalizer canon = new AggressiveUrlCanonicalizer();

    /**
     * Ensures that a value read from a WARC-header is usable. This means checking whether the value is
     * encapsulated in {@code <} or {@code >} and if so, removing these signs.
     * See <a href="https://github.com/ukwa/webarchive-discovery/issues/159">webarchive-discovery issues 159</a>.
     * A warning is logged if there is exactly 1 of either leading {@code <} or trailing {@code >}.
     * @param value the second part of a WARC-header key-value pair.
     * @return the value not encapsulated in {@code <>}.
     */
    public static String sanitiseWARCHeaderValue(String value) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("<")) {
            if (value.endsWith(">")) {
                return value.substring(1, value.length()-1);
            }
            log.warn("sanitiseWARCHeaderValue: The value started with '<' but did not end in '>': '" + value + "'");
        } else if (value.endsWith(">")) {
            log.warn("sanitiseWARCHeaderValue: The value ended with '>' but did not start with '<': '" + value + "'");
        }
        return value;
    }

    public static String canonicaliseHost(String host) throws URIException {
        return canon.urlStringToKey(host.trim()).replace("/", "");
    }

    /**
     * Default and very aggressive normaliser. Shorthand for {@code canonicaliseURL(url, true, true)}.
     */
    public static String canonicaliseURL(String url) {
        return canonicaliseURL(url, true, true);
    }

    /**
     * Corrects errors in URLs. Currently only handles faulty escapes, such as "...wine 12% proof...".
     */
    public static String fixURLErrors(String url) {
        return canonicaliseURL(url, false, false);
    }

    /**
     * Resolved one URL relative to another, e.g.
     * 'foo/bar.html' relative to 'http://example.com/zoo/' is 'http://example.com/zoo/foo/bar.html'.
     * Always normalises the result. Use {@link #resolveRelative(String, String, boolean)} to choose otherwise.
     * @param url       base URL.
     * @param relative  resolved relative to url.
     * @return the fully resolved version of the relative URL.
     * @throws IllegalArgumentException if an unrecoverable unvalid URL was encountered,
     */
    public static String resolveRelative(String url, String relative) throws IllegalArgumentException {
        return resolveRelative(url, relative, true);
    }
    /**
     * Resolved one URL relative to another, e.g.
     * 'foo/bar.html' relative to 'http://example.com/zoo/' is 'http://example.com/zoo/foo/bar.html'.
     * @param url       base URL.
     * @param relative  resolved relative to url.
     * @param normalise if true the resulting URL is also normalised.
     * @return the fully resolved version of the relative URL.
     * @throws IllegalArgumentException if an unrecoverable unvalid URL was encountered,
     */
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
        if (PropertiesLoader.NORMALISE_URLS==false) { // Set only to false if using warc-indexer before version 3.0. (see solrwayback.properties)
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
    private static Pattern DOMAIN_ONLY = Pattern.compile("https?://[^/]+");
    private static Pattern WWW_PREFIX = Pattern.compile("([a-z]+://)(?:www[0-9]*|ww2|ww)[.](.+)");

    // Normalisation to UTF-8 form
    private static byte[] fixEscapeErrorsAndUnescapeHighOrderUTF8(final String url) {
        ByteArrayOutputStream sb = new ByteArrayOutputStream(url.length()*2);
        final byte[] utf8 = url.getBytes(UTF8_CHARSET);
        int i = 0;
        while (i < utf8.length) {
            int c = utf8[i];
            if (c == '%') {
                if (i < utf8.length-2 && isHex(utf8[i+1]) && isHex(utf8[i+2])) {
                    int u = Integer.parseInt("" + (char)utf8[i+1] + (char)utf8[i+2], 16);
                    if ((0b10000000 & u) == 0) { // ASCII, so don't touch!
                        sb.write('%'); sb.write(utf8[i+1]); sb.write(utf8[i+2]);
                    } else { // UTF-8, so write raw byte
                        sb.write(0xFF & u);
                    }
                    i += 3;
                } else { // Faulty, so fix by escaping percent
                    sb.write('%'); sb.write('2'); sb.write('5');
                    i++;
                }
                // https://en.wikipedia.org/wiki/UTF-8
            } else { // Not part of escape, just pass the byte
                sb.write(0xff & utf8[i++]);
            }
        }
        return sb.toByteArray();
    }

    // Requires valid %-escapes (as produced by fixEscapeErrorsAndUnescapeHighOrderUTF8) and UTF-8 bytes
    private static String escapeUTF8(final byte[] utf8, boolean escapeHighOrder, boolean normaliseLowOrder) {
        ByteArrayOutputStream sb = new ByteArrayOutputStream(utf8.length*2);
        int i = 0;
        boolean paramSection = false; // Affects handling of space and plus
        while (i < utf8.length) {
            int c = 0xFF & utf8[i];
            paramSection |= c == '?';
            if (paramSection && c == ' ') { // In parameters, space becomes plus
                sb.write(0xFF & '+');
            } else if (c == '%') {
                int codePoint = Integer.parseInt("" + (char) utf8[i + 1] + (char) utf8[i + 2], 16);
                if (paramSection && codePoint == ' ') { // In parameters, space becomes plus
                    sb.write(0xFF & '+');
                } else if (mustEscape(codePoint) || keepEscape(codePoint) || !normaliseLowOrder) { // Pass on unmodified
                    hexEscape(codePoint, sb);
                } else { // Normalise to ASCII
                    sb.write(0xFF & codePoint);
                }
                i += 2;
            } else if ((0b10000000 & c) == 0) { // ASCII
                if (mustEscape(c)) {
                    hexEscape(c, sb);
                } else {
                    sb.write(0xFF & c);
                }
            } else if ((0b11000000 & c) == 0b10000000) { // Non-first UTF-8 byte as first byte
                hexEscape(c, sb);
            } else if ((0b11100000 & c) == 0b11000000) { // 2 byte UTF-8
                if (i >= utf8.length-1 || (0b11000000 & utf8[i+1]) != 0b10000000) { // No byte or wrong byte follows
                    hexEscape(c, sb);
                } else if (escapeHighOrder) {
                    hexEscape(0xff & utf8[i++], sb);
                    hexEscape(0xff & utf8[i], sb);
                } else {
                    sb.write(utf8[i++]);
                    sb.write(utf8[i]);
                }
            } else if ((0b11110000 & utf8[i]) == 0b11100000) { // 3 byte UTF-8
                if (i >= utf8.length-2 || (0b11000000 & utf8[i+1]) != 0b10000000 ||
                    (0b11000000 & utf8[i+2]) != 0b10000000) { // Too few or wrong bytes follows
                    hexEscape(c, sb);
                } else {
                    hexEscape(0xff & utf8[i++], sb);
                    hexEscape(0xff & utf8[i++], sb);
                    hexEscape(0xff & utf8[i], sb);
                }
            } else if ((0b11111000 & utf8[i]) == 0b11110000) { // 4 byte UTF-8
                if (i >= utf8.length-3 || (0b11000000 & utf8[i+1]) != 0b10000000 || // Too few or wrong bytes follows
                    (0b11000000 & utf8[i+2]) != 0b10000000 || (0b11000000 & utf8[i+3]) != 0b10000000) {
                    hexEscape(c, sb);
                } else {
                    hexEscape(0xff & utf8[i++], sb);
                    hexEscape(0xff & utf8[i++], sb);
                    hexEscape(0xff & utf8[i++], sb);
                    hexEscape(0xff & utf8[i], sb);
                }
            } else {  // Illegal first byte for UTF-8
                hexEscape(c, sb);
                log.debug("Sanity check: Unexpected code path encountered.: The input byte-array did not translate" +
                          " to supported UTF-8 with invalid first-byte for UTF-8 codepoint '0b" +
                          Integer.toBinaryString(c) + "'. Writing escape code for byte " + c);
            }
            i++;
        }
        try {
            return sb.toString("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Internal error: UTF-8 must be supported by the JVM", e);
        }
    }

    private static void hexEscape(int codePoint, ByteArrayOutputStream sb) {
        sb.write('%');
        sb.write(HEX[codePoint >> 4]);
        sb.write(HEX[codePoint & 0xF]);
    }
    private final static byte[] HEX = "0123456789abcdef".getBytes(UTF8_CHARSET); // Assuming lowercase

    // Some low-order characters must always be escaped
    // TODO: Consider adding all unwise characters from https://www.ietf.org/rfc/rfc2396.txt : {|}\^[]`
    private static boolean mustEscape(int codePoint) {
        return codePoint == ' ' || codePoint == '%' || codePoint == '\\';
    }

    // If the codePoint is already escaped, keep the escaping
    private static boolean keepEscape(int codePoint) {
        return codePoint == '#';
    }

    private static boolean isHex(byte b) {
        return (b >= '0' && b <= '9') || (b >= 'a' && b <= 'f') || (b >= 'A' && b <= 'F');
    }


}
