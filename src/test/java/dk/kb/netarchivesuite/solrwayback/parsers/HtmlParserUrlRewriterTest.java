package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    // TODO: Check canonicalization
    @Before
    public void invalidateProperties() {
        // We need this so that we know what the Solr server is set to
        PropertiesLoader.initProperties();
        PropertiesLoader.WAYBACK_BASEURL = "http://localhost:0000/solrwayback/";
    }

    @Test
    public void testSimpleRewriting() throws Exception {
        assertRewrite("simple", 11);
    }

    // No verification of result, only count of replaced
    @Test
    public void testSimpleRewritingCount() throws Exception {
        assertCount("simple", 11);
    }

    @Test
    public void testMultiSourceRewriting() throws Exception {
        assertRewrite("multisource", 16);
    }

    @Test
    public void testCSSRewriting() throws Exception {
        assertRewrite("css", 13);
    }

    @Test
    public void testCSSImportRewriting() throws Exception {
        assertRewrite("css_import", 3);
    }

    @Test
    public void testStyleElement() throws Exception {
        assertRewrite("style_element", 6);
    }

    @Test
    public void testScriptRewriting() throws Exception {
        // TODO: Make a better counter for replaced
        assertRewrite("script", 0);
    }

    @Test
    public void testScript2Rewriting() throws Exception {
        assertRewrite("script2", 0);
    }

    @Test
    public void testScriptEscaping() throws Exception {
        assertRewrite("script_escape", 0);
    }

    /* *************************************************************************************
     * Helpers below
     ************************************************************************************* */

    private void assertRewrite(String testPrefix, int expectedReplaced) throws Exception {
        final String input = RewriteTestHelper.fetchUTF8("example_rewrite/" + testPrefix + ".html");
        final String expected = RewriteTestHelper.fetchUTF8("example_rewrite/" + testPrefix + "_expected.html").
                replaceAll(" +\n", "\n");

        ParseResult rewritten = HtmlParserUrlRewriter.replaceLinks(
                input, "http://example.com/somefolder/", "2020-04-30T13:07:00",
                RewriteTestHelper.createOXResolver());

        assertEquals("The result should be as expected for test '" + testPrefix + "'",
                     expected, rewritten.getReplaced().replaceAll(" +\n", "\n"));
        assertEquals("The number of replaced links should be as expected",
                     expectedReplaced, rewritten.getNumberOfLinksReplaced());
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
