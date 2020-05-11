package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
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
public class ScriptRewriterTest {

    @Before
    public void invalidateProperties() {
        // We need this so that we know what the Solr server is set to
        PropertiesLoader.initProperties();
        PropertiesLoader.WAYBACK_BASEURL = "http://localhost:0000/solrwayback/";
    }

    // TODO: Make &quot; work in input
    @Test
    public void testAttributeScript() throws Exception {
        // Espected to be enclosed in single pings {@code '}
        final String SCRIPT = "return {\"url\": \"\\/foo/bar_o1.jpg\" };";
        final String EXPECTED = "return {&quot;url&quot;: &quot;http://localhost:0000/solrwayback/services/downloadRaw?source_file_path=somesourcefile&offset=1&quot; };";

        ScriptRewriter rewriter = new ScriptRewriter();
        String actual = rewriter.replaceLinks(
                SCRIPT, RewriterBase.PACKAGING.attribute,
                "http://example.com", "20200511132200",
                RewriteTestHelper.createMockResolver()).getReplaced();
        assertEquals(EXPECTED, actual);
    }

}
