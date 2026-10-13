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
package dk.kb.netarchivesuite.solrwayback.parsers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Guards against a known jsoup regression, introduced in jsoup 1.23.2 (issue #2577), where
 * {@code <title>} and {@code <textarea>} became stricter RCDATA elements per the HTML5 spec:
 * if there is no matching closing tag, EVERYTHING from that point to end-of-document is
 * swallowed as literal text inside the {@code <title>}/{@code <textarea>} element, instead of
 * the rest of the page being recovered as normal markup (which is what jsoup 1.19.1 through
 * 1.23.1 - and real browsers' "be liberal" quirks in practice - did).
 * <p>
 * A missing {@code </title>} or {@code </textarea>} is a very common authoring mistake on old,
 * hand-written web pages (SolrWayback's corpus goes back to the mid-1990s), so this isn't an
 * obscure edge case: it means the entire body of an affected archived page silently disappears
 * during playback. See the comment on the jsoup dependency in {@code pom.xml} - jsoup must not
 * be upgraded past 1.23.1 without first confirming this test still passes.
 */
public class JsoupVersionRegressionTest {

    @Test
    public void testUnclosedTitleDoesNotSwallowPageBody() {
        // A minimal reproduction of the real-world pattern: <title> is never closed before
        // <body> starts. Deliberately has no </title> anywhere in the document.
        String html =
                "<html>\n" +
                "<head>\n" +
                "<title>Broken Page - missing title close tag\n" +
                "<body>\n" +
                "<h1>Broken Page</h1>\n" +
                "<p>This paragraph should still be part of the page body.</p>\n" +
                "</body>\n" +
                "</html>\n";

        Document doc = Jsoup.parse(html, "http://oldsite.example.org/brokentitle.html");

        assertEquals(
                "jsoup parsed more or fewer than 1 <h1> element into the body. This suggests " +
                "the real page content was swallowed into the unclosed <title> element instead " +
                "of being recovered as markup - see the class javadoc for background. Do not " +
                "upgrade jsoup past 1.23.1 without re-checking this test.",
                1, doc.body().select("h1").size());

        assertTrue(
                "jsoup swallowed the page body into the unclosed <title> element: the expected " +
                "paragraph text is missing from <body>. This means the jsoup version in use has " +
                "regressed to (or past) the RCDATA/EOF-swallowing behavior introduced in jsoup " +
                "1.23.2 (issue #2577). Do not upgrade jsoup past 1.23.1 - see the comment on the " +
                "jsoup dependency in pom.xml.",
                doc.body().text().contains("This paragraph should still be part of the page body"));
    }
}
