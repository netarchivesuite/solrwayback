package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

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
public class HtmlParserUrlRewriterTest {
    private static final Logger log = LoggerFactory.getLogger(HtmlParserUrlRewriterTest.class);

    @Before
    public void invalidateProperties()  throws Exception{

        // Need this to ensure that the normaliser has a known setting
        PropertiesLoader.initProperties(UnitTestUtils.getFile("properties/solrwayback.properties").getPath());
        Normalisation.setTypeFromConfig();

        // We need this so that we know what the Solr server is set to
        PropertiesLoader.WAYBACK_BASEURL = "http://localhost:0000/solrwayback/";
    }

    @Test
    public void testSimpleRewriting() throws Exception {
        assertRewrite("simple");
    }

    // No verification of result, only count of replaced
    @Test
    public void testSimpleRewritingCount() throws Exception {
        assertCount("simple", 13);
    }

    @Test
    public void testMultiSourceRewriting() throws Exception {
        // The -1 is due to the "substring trickery" entry
        assertRewrite("multisource", count("_o[0-9]+", "multisource")-1);
    }

    @Test
    public void testCSSRewriting() throws Exception {
        assertRewrite("css");
    }

    @Test
    public void testCSS2Rewriting() throws Exception {
        // TODO: FIXME: This uses the port from ~/solrwayback.properties instead of port 0000 stated in @Before
        assertRewrite("css2", 3, 2);
    }

    @Test
    public void testCSSImportRewriting() throws Exception {
        assertRewrite("css_import");
    }

    @Test
    public void testStyleElement() throws Exception {
        assertRewrite("style_element");
    }

    @Test
    public void testCDATA() throws Exception {
        assertRewrite("cdata");
    }

    @Test
    public void testScriptRewriting() throws Exception {
        // TODO: Make a better counter for replaced
        assertRewrite("script", 0);
    }

    @Test
    public void testEncodingRewriting() throws Exception {
        assertRewrite("encoding", 0);
    }

    @Test
    public void testAmpersandRewriting() throws Exception {
        assertRewrite("url_escape", 0);
    }

    @Test
    public void testScript2Rewriting() throws Exception {
        // TODO: Make a better counter for replaced
        assertRewrite("script2", 0);
    }

    @Test
    public void testScriptEscaping() throws Exception {
        // TODO: Make a better counter for replaced
        assertRewrite("script_escape", 0);
    }

    /* *************************************************************************************
     * Helpers below
     ************************************************************************************* */

    /**
     * Count the number of times regexp matches the source file.
     */
    private int count(String regexp, String source) throws IOException {
        final String input = RewriteTestHelper.fetchUTF8("example_rewrite/" + source + ".html");
        Matcher matcher = Pattern.compile(regexp).matcher(input);

        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    // All links must contain {@code _oX} where X is an integer.
    private void assertRewrite(String testPrefix) throws Exception {
        assertRewrite(testPrefix, count("_o[0-9]+", testPrefix), -1);
    }

    // All links must contain {@code _oX} where X is an integer.
    private void assertRewrite(String testPrefix, int expectedReplaced) throws Exception {
        assertRewrite(testPrefix, expectedReplaced, -1);
    }

    // Only links containing {@code _oX} where X is an integer, will be replaced.
    private void assertRewrite(String testPrefix, int expectedReplaced, int expectedNotFound) throws Exception {
        final String input = RewriteTestHelper.fetchUTF8("example_rewrite/" + testPrefix + ".html");
        final String expected = RewriteTestHelper.fetchUTF8("example_rewrite/" + testPrefix + "_expected.html").
                replaceAll(" +\n", "\n");

        ParseResult rewritten = HtmlParserUrlRewriter.replaceLinks(
                input, "http://example.com/somefolder/", "2020-04-30T13:07:00",
                RewriteTestHelper.createOXResolver(expectedNotFound >= 0));

        assertEquals("The result should be as expected for test '" + testPrefix + "' ",
                     normalise(expected), normalise(rewritten.getReplaced()));
        assertEquals("The number of replaced links should be as expected",
                     expectedReplaced, rewritten.getNumberOfLinksReplaced());
        if (expectedNotFound >= 0) {
            assertEquals("The number of not found links should be as expected",
                         expectedNotFound, rewritten.getNumberOfLinksNotFound());
        }
    }

    /**
     * @param text multiline text.
     * @return the text where leading and trailing spaces has been removed from all lines and empty lines
     * has been removed.
     */
    private String normalise(String text) {
        return Arrays.stream(
                text.replace("> <!--", ">\n<!--") // JSoup won't put comments on their own lines
                        .replace("</style> <a", "</style>\n<a") // JSoup strangeness with lines starting with <a...>
                        .replace("</a> <a", "</a>\n<a")
                        .replace("--> <a", "-->\n<a")
                        .split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    private void assertCount(String testPrefix, int expectedReplaced) throws Exception {
        final String input = RewriteTestHelper.fetchUTF8("example_rewrite/" + testPrefix + ".html");

        ParseResult rewritten = HtmlParserUrlRewriter.replaceLinks(
                input, "http://example.com/somefolder/", "2020-04-30T13:07:00",
                RewriteTestHelper.createIdentityResolver());

        assertEquals("The number of replaced links should be as expected",
                     expectedReplaced, rewritten.getNumberOfLinksReplaced());
    }

}
