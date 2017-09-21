package dk.kb.netarchivesuite.solrwayback.parsers;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import com.google.common.net.UrlEscapers;

public class SolrWaybackEncodingUtil {
  
  /**
   * Escapes all non-ASCII-characters to UTF-8 represented as hex-codes. Supports up to 4-byte UTF-8.
   * flødebolle -> fl%C3%B8debolle
   */

  private static Charset UTF8_CHARSET = Charset.forName("UTF-8");
  
  /**
   * Unescapes all hex-escapes to UTF-8
   */


  @SuppressWarnings("StatementWithEmptyBody")
  public static String unEscapeHex(final String str) throws UnsupportedEncodingException {

    ByteArrayOutputStream sb = new ByteArrayOutputStream(str.length()*2);

    final byte[] utf8 = str.getBytes(UTF8_CHARSET);

    int i = 0;
    while (i < utf8.length) {
        int c = utf8[i];
        if (c == '%') {
            if (i < utf8.length-2 && isHex(utf8[i+1]) && isHex(utf8[i+2])) {
                sb.write(Integer.parseInt("" + (char)utf8[i+1] + (char)utf8[i+2], 16));
                i += 3;
            } else {
                sb.write('%');
                i++;
            }
         // https://en.wikipedia.org/wiki/UTF-8
        } else if ((0b10000000 & utf8[i]) == 0) { // ASCII
            sb.write(0xff & utf8[i++]);
        } else if ((0b11100000 & utf8[i]) == 0b11000000) { // 2 byte UTF-8
            sb.write(0xff & utf8[i++]);
            sb.write(0xff & utf8[i++]);
        } else if ((0b11110000 & utf8[i]) == 0b11100000) { // 3 byte UTF-8
            sb.write(0xff & utf8[i++]);
            sb.write(0xff & utf8[i++]);
            sb.write(0xff & utf8[i++]);
        } else if ((0b11111000 & utf8[i]) == 0b11110000) { // 4 byte UTF-8
            sb.write(0xff & utf8[i++]);
            sb.write(0xff & utf8[i++]);
            sb.write(0xff & utf8[i++]);
            sb.write(0xff & utf8[i++]);
        } else {
            throw new IllegalArgumentException(
                    "The input String '" + str + "' does not translate to supported UTF-8");
        }
    }
    return sb.toString("utf-8");
}

private static boolean isHex(byte b) {

    return (b >= '0' && b <= '9') || (b >= 'a' && b <= 'f') || (b >= 'A' && b <= 'F');

}
  
  
  public static String escapeNonAscii(String str) throws UnsupportedEncodingException {
    StringBuffer sb = new StringBuffer();
    sb.setLength(0);
    final byte[] utf8 = str.getBytes(UTF8_CHARSET);

    int i = 0;

    while (i < utf8.length) {
        int c = utf8[i];
        // It is intended * is not included since wget does not normalize this 
        if (c <= 0x20 || c == '%' || c == '>' || c == '<' || c == '^' || c == '#') { // Special characters
           sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
            // https://en.wikipedia.org/wiki/UTF-8
        } else if ((0b10000000 & utf8[i]) == 0) { // ASCII
            sb.append((char) (0xff & utf8[i++]));
        } else if ((0b11100000 & utf8[i]) == 0b11000000) { // 2 byte UTF-8
            sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
            sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
        } else if ((0b11110000 & utf8[i]) == 0b11100000) { // 3 byte UTF-8
            sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
            sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
            sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
        } else if ((0b11111000 & utf8[i]) == 0b11110000) { // 4 byte UTF-8
            sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
            sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
            sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
            sb.append("%").append(String.format("%02X", 0xff & utf8[i++]));
        } else {
            throw new IllegalArgumentException(
                    "The input String '" + str + "' does not translate to supported UTF-8");
        }

    }

    return sb.toString();

}
  
  /*
   * Not used. But this method almost did the combined work of unescapeHex and escapeNonAscii.
   * But it was too agressive and [ and ] was also replaces, and heritrix does not do that. 
   */
  public static String magicEscape(String str) throws UnsupportedEncodingException {
    String magicString = "__PERCENT__";
    String temp = str.replace("%",magicString) ;    
    String encodedString = UrlEscapers.urlFragmentEscaper().escape(temp);
    temp=encodedString.replaceAll(magicString, "%");
    return temp;
  }
}
