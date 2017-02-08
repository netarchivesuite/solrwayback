package dk.kb.netarchivesuite.solrwayback.parsers;

import junit.framework.TestCase;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
public class ProximityHtmlParserTest extends TestCase {

    public void testPatternDebug() {
        Pattern all = ProximityHtmlParser.createAllPattern(new HashSet<>(Arrays.asList("foo", "bar")));
        //Pattern both = Pattern.compile("\\b(foo|bar)\\b");
        Matcher matcher = all.matcher("<base hh href=\"mybase\" ...>hellofoo-bar:foo?<img src=\"myimg\"/>foobar");
        while (matcher.find()) {
            System.out.println(matcher.group(0) + " -> " + matcher.group(1) + " -> " + matcher.group(2) + " -> " + matcher.group(3));
        }
    }

    public void testRelativeURL() throws MalformedURLException, URISyntaxException {
//        System.out.println(new URI("http://foo.bar/zoo/zoo2/").resolve(new URI("../gbi")).normalize());
        testURL("http://foo/bar", "http://foo/", "bar");
        testURL("http://example.com/bar", "http://example.com/sub/", "../bar");
        testURL("http://example.com/bar", "http://example.com", "/bar");
        testURL("http://example.com/bar", "http://example.com/sub/subb/subbb", "/bar");
    }
    private void testURL(String expected, String base, String local) throws MalformedURLException {
        assertEquals(expected, ProximityHtmlParser.resolveURL(new URL(base), local).toExternalForm());
    }

    public void testHTML1() throws Exception {
        List<ProximityHtmlParser.WeightedImage> images = ProximityHtmlParser.getImageUrls(
                new URL("http://kimse.rovfisk.dk/katte/"), 0.77, HTML, new HashSet<>(Arrays.asList("rovfisk")), 99);
        for (ProximityHtmlParser.WeightedImage image: images) {
            System.out.println(image);
        }
    }

    static final String HTML =
            "<?xml version=\"1.1\" encoding=\"iso-8859-1\"?>"+
            "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\">"+
            "<head>"+
            "   <title>kimse.rovfisk.dk/katte/</title>"+
            "   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\" />"+
            "   <link rel=\"stylesheet\" href=\"/style.css\"  type=\"text/css\" media=\"screen\" />"+
            " <link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\"/rss2.php\" />"+
            "</head>"+
            "<body>"+
            "<a class=\"toplink\" href=\"/\">kimse.rovfisk.dk  </a><a class=\"toplink\" href=\"/katte/\">katte / </a><br />" +
            "<br /><table cellspacing=\"8\"><tr><td></td><td class=\"itemw\">" +
            "<a href=\"/katte/?browse=DSC00175.JPG\"><img class=\"lo\" src=\"/cache/katte/DSC00175.JPG\" /></a></td>"+
            "<td></td><td class=\"itemw\"><a href=\"/katte/?browse=DSC00209.JPG\"><img class=\"lo\"" +
            " src=\"/cache/katte/DSC00209.JPG\" /></a></td>"+
            "</table><br />  </body>"+
            "</html>";


}