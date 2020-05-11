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
        assertRewrite("simple");
    }

    @Test
    public void testMultiSourceRewriting() throws Exception {
        assertRewrite("multisource");
    }

    @Test
    public void testCSSRewriting() throws Exception {
        assertRewrite("css");
    }

    @Test
    public void testCSSImportRewriting() throws Exception {
        assertRewrite("css_import");
    }

    @Test
    public void testStyleBackground() throws Exception {
        assertRewrite("style_element");
    }

    @Test
    public void testScriptRewriting() throws Exception {
        assertRewrite("script");
    }

    /* *************************************************************************************
     * Helpers below
     ************************************************************************************* */

    private void assertRewrite(String testPrefix) throws Exception {
        final String input = RewriteTestHelper.fetchUTF8("example_rewrite/" + testPrefix + ".html");
        final String expected = RewriteTestHelper.fetchUTF8("example_rewrite/" + testPrefix + "_expected.html").
                replaceAll(" +\n", "\n");

        String rewritten = HtmlParserUrlRewriter.replaceLinks(
                input, "http://example.com/somefolder/", "20200430130700", "2020-04-30T13:07:00",
                RewriteTestHelper.createMockResolver()).getReplaced().replaceAll(" +\n", "\n");

        assertEquals("The result should be as expected for test '" + testPrefix + "'", expected, rewritten);
    }

}
