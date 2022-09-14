package dk.kb.netarchivesuite.solrwayback.normalise;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NormalisationAbstract {

    private static Log log = LogFactory.getLog( NormalisationAbstract.class );
    
    private static Charset UTF8_CHARSET = Charset.forName("UTF-8");
    protected static Pattern DOMAIN_ONLY = Pattern.compile("https?://[^/]+");
    private final static byte[] HEX = "0123456789abcdef".getBytes(UTF8_CHARSET); // Assuming lowercase
    
    
    // Requires valid %-escapes (as produced by fixEscapeErrorsAndUnescapeHighOrderUTF8) and UTF-8 bytes
    protected static String escapeUTF8(final byte[] utf8, boolean escapeHighOrder, boolean normaliseLowOrder) {
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
    
 // Normalisation to UTF-8 form
   protected static byte[] fixEscapeErrorsAndUnescapeHighOrderUTF8(final String url) {
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

    

   private static boolean isHex(byte b) {
       return (b >= '0' && b <= '9') || (b >= 'a' && b <= 'f') || (b >= 'A' && b <= 'F');
   }
   
    private static void hexEscape(int codePoint, ByteArrayOutputStream sb) {
        sb.write('%');
        sb.write(HEX[codePoint >> 4]);
        sb.write(HEX[codePoint & 0xF]);
    }
    
    // Some low-order characters must always be escaped
    // TODO: Consider adding all unwise characters from https://www.ietf.org/rfc/rfc2396.txt : {|}\^[]`
    private static boolean mustEscape(int codePoint) {
        return codePoint == ' ' || codePoint == '%' || codePoint == '\\';
    }

    // If the codePoint is already escaped, keep the escaping
    private static boolean keepEscape(int codePoint) {
        return codePoint == '#';
    }
    
    
}


