package dk.kb.netarchivesuite.solrwayback.util;

import com.google.protobuf.Enum;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

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
public class RegexpReplacerTest {

    @Test
    public void testBasic() {
        final String TEST = "foo(42), bar(87), foo(99), zoo(43)";
        final String EXPECTED = "foo(1), bar(87), foo(2), zoo(43)";

        AtomicInteger counter = new AtomicInteger(0);
        RegexpReplacer replacer = new RegexpReplacer("foo\\(([0-9]+)\\)",
                                                     number -> Integer.toString(counter.incrementAndGet()));

        assertEquals(EXPECTED, replacer.apply(TEST));
    }

    @Test
    public void testHashtags() {
        final String TEST = "Some text with #COVID and #COVID19 hashtags. Also comma #TAG1, #TAG2 and tag at #theend";
        final String EXPECTED = "Some text with <a href=\"taglink:COVID\">#COVID</a> and " +
                                "<a href=\"taglink:COVID19\">#COVID19</a> hashtags. Also comma " +
                                "<a href=\"taglink:TAG1\">#TAG1</a>, <a href=\"taglink:TAG2\">#TAG2</a> and tag at" +
                                " <a href=\"taglink:theend\">#theend</a>";

        RegexpReplacer replacer = new RegexpReplacer("(#[\\p{Alpha}][\\p{Alnum}]+)",
                                                     tag -> {
                                                         tag = tag.substring(1);
                                                         return  "<a href=\"taglink:" + tag + "\">#" + tag + "</a>";
                                                     });

        assertEquals(EXPECTED, replacer.apply(TEST));
    }

    @Test
    public void testMultiline() {
        final String TEST = "foo(42), bar(87),\nfoo(99), zoo(43)";
        final String EXPECTED = "foo(1), bar(87),\nfoo(2), zoo(43)";
        AtomicInteger counter = new AtomicInteger(0);
        RegexpReplacer replacer = new RegexpReplacer("foo\\(([0-9]+)\\)",
                                                     number -> Integer.toString(counter.incrementAndGet()));

        assertEquals(EXPECTED, replacer.apply(TEST));
    }

    @Test
    public void testMultilineNotDotall() {
        final String TEST = "foo(42), bar(87),\nfoo(99), zoo(43)";
        final String EXPECTED = "foo(a), bar(87),\nfoo(a), zoo(43)";
        Pattern pattern = Pattern.compile("foo\\(([0-9]+)\\)");
        RegexpReplacer replacer = new RegexpReplacer(pattern, number -> "a");

        assertEquals(EXPECTED, replacer.apply(TEST));
    }

    @Test
    public void testScriptRegexp() {
        final String INPUT =
                "\"video\": \"\\u003CBaseURL>https:\\/\\/video.example.com\\/v88_n.mp4?_nc_cat=102&amp;_nc_sid=5aa_o3&amp;oe=50525\\u003C\\/BaseURL>\"";
        Pattern pattern = Pattern.compile(
                "(?s)(?:<|\\\\u003[cC]|&lt;)BaseURL(?:>|&gt;)(.+?)(?:<|\\\\u003[cC]|&lt;)\\\\?/BaseURL(?:>|&gt;)");
        assertTrue(pattern.matcher(INPUT).find());
    }
}
