package dk.kb.netarchivesuite.solrwayback.parsers;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class SolrWaybackEncodingUtil {

  
  
  /**

   * Escapes all non-ASCII-characters to UTF-8 represented as hex-codes. Supports up to 4-byte UTF-8.

   * flødebolle -> fl%C3%B8debolle

   */

  private static Charset UTF8_CHARSET = Charset.forName("UTF-8");
  
  /**

   * Unescapes all hex-escapes to UTF-8

   */

  public static String unEscapeHex(String str) throws UnsupportedEncodingException {
      return java.net.URLDecoder.decode(str, "UTF-8");
  }
  
  public static String escapeNonAscii(String str) throws UnsupportedEncodingException {
    StringBuffer sb = new StringBuffer();
    sb.setLength(0);
    final byte[] utf8 = str.getBytes(UTF8_CHARSET);

    int i = 0;

    while (i < utf8.length) {
        int c = utf8[i];
        if (c <= 0x20 || c == '%') { // Special characters
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
  
}
