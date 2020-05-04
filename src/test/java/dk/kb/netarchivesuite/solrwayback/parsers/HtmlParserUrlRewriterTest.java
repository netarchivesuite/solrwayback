package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
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
    public void invalidateProperties() {
        // We need this so that we know what the Solr server is set to
        PropertiesLoader.initProperties();
        PropertiesLoader.WAYBACK_BASEURL = "http://localhost:0000/solrwayback/";
    }

    @Test
    public void testBasicRewriting() throws Exception {
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

    // Disabled for now as it is under construction
    public void testScriptRewriting() throws Exception {
        assertRewrite("script");
    }

    /* *************************************************************************************
     * Helpers below
     ************************************************************************************* */

    private void assertRewrite(String testPrefix) throws Exception {
        final String input = fetchUTF8("example_rewrite/" + testPrefix + ".html");
        final String expected = fetchUTF8("example_rewrite/" + testPrefix + "_expected.html").
                replaceAll(" +\n", "\n");

        String rewritten = HtmlParserUrlRewriter.replaceLinks(
                input, "http://example.com/somefolder/", "2020043030700", mockNearestResolver).
                getHtmlReplaced().replaceAll(" +\n", "\n");

        assertEquals("The result should be as expected for test '" + testPrefix + "'", expected, rewritten);
    }

    private static final HtmlParserUrlRewriter.NearestResolver mockNearestResolver =
            (urls, timeStamp) -> urls.stream().
                    map(url -> makeIndexDoc(url, timeStamp)).
                    filter(Objects::nonNull).
                    collect(Collectors.toList());

    // Fake url_norm, url, source_file, source_file_offset
    private static IndexDoc makeIndexDoc(String url, String timeStamp) {
        if (!url.startsWith("http")) {
            log.warn("mockResolver is skipping '" + url + "' as it does not start with 'http'");
            return null;
        }
        IndexDoc doc = new IndexDoc();
        doc.setUrl(url);
        doc.setUrl_norm(Normalisation.canonicaliseURL(url));
        doc.setSource_file_path("somesourcefile");
        doc.setOffset(Math.abs((url+timeStamp).hashCode() % 10000));
        return doc;
    }

    public static String fetchUTF8(String resource) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url == null) {
            Path path = Paths.get(resource);
            if (!Files.exists(path)) {
                throw new FileNotFoundException("Unable to locate '" + resource + "'");
            }
            url = path.toUri().toURL();
        }

        try (InputStream in = url.openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream(1024);) {
            byte[] buffer = new byte[1024];
            int len = in.read(buffer);
            while (len != -1) {
                out.write(buffer, 0, len);
                len = in.read(buffer);
            }
            return out.toString("utf-8");
        }
    }
}
