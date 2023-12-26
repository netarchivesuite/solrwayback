package dk.kb.netarchivesuite.solrwayback.util;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
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

    // Derived from 144571 primarily Danish tweets
    private final static String HASHTAG_PATTERN = "(#[\\p{L}][\\p{L}\\p{Digit}_ाोे]+)";

    @Test
    public void testHashtags() {
        final String TEST = "Some text with #COVID and #COVID19 hashtags. #under_score " +
                            "Also comma #TAG1, #TAG2 and tag at #theend";
        final String EXPECTED = "Some text with <a href=\"taglink:COVID\">#COVID</a> and " +
                                "<a href=\"taglink:COVID19\">#COVID19</a> hashtags. " +
                                "<a href=\"taglink:under_score\">#under_score</a> Also comma " +
                                "<a href=\"taglink:TAG1\">#TAG1</a>, <a href=\"taglink:TAG2\">#TAG2</a> and tag at" +
                                " <a href=\"taglink:theend\">#theend</a>";

        RegexpReplacer replacer = new RegexpReplacer(HASHTAG_PATTERN,
                                                     tag -> {
                                                         tag = tag.substring(1);
                                                         return  "<a href=\"taglink:" + tag + "\">#" + tag + "</a>";
                                                     });

        assertEquals(EXPECTED, replacer.apply(TEST));
    }

    @Test
    public void testHashtagLetters() {
        // Extracted letters in hashtags from 144571 primarily Danish tweets with
        // zcat *.json.gz | jq -r '.extended_tweet.entities.hashtags[].text' 2> /dev/null | sed 's/\(.\)/\1\n/g' | sort | uniq -c | sort -rn | sed 's/[^0-9]*[0-9]\+[^0-9]//' | tr -d '\n' ; echo ""
        final String LETTERS =
                "edoiarknlstpmguchvbSyCDfAw1I9EOVUzKPTWjHFøMサBRNGL20_æıxYå4üqJ3ö6XZاğşQä5İ7с8íаοαλلéεرçиنоςιمØнσي" +
                "ÖηδркρودóятπνهτعáувκحبšлдυίكفسŞñčΠΕΔقÆ黑警祐田桑料佳ाцμΜشختأÜÍâ質相無濫星問中सыРмжегωχΤΝθήέάةآəèÇāی香集鄭" +
                "送贜説計観視衛結紀箱禍異留画理火港流法死欧次権査林更日新文散政插探捕打愛思布崩小家完安学嫁坂国図向名匿北化募全光値倍人交二" +
                "ोेवलरभपजकСНјбώόΟΚΖγβΒΑغطضصجúãà\ucc87";
        final RegexpReplacer replacer = new RegexpReplacer(HASHTAG_PATTERN, tag -> "Matched");

        StringBuilder nonMatched = new StringBuilder(LETTERS.length());
        for (Character c: LETTERS.toCharArray()) {
            // We prepend 'a' as digit-only tags are not allowed
            if (!"Matched".equals(replacer.apply("#a" + c))) {
                nonMatched.append(c);
            }
        }

        assertEquals("All letters extracted from Twitter JSON hashtags should match. Non-matching letters were: " +
                     nonMatched.toString(), 0, nonMatched.length());
    }

    @Test
    public void testIllegalHashtags() {
        final String[] ILLEGALS = new String[] {
                "#123",
                "# ",
                "#",
                "#a-b",
                "#a b",
                "#a\u00a0b", // Non-breaking space
                "#a!",
                "#1a"
        };
        final RegexpReplacer replacer = new RegexpReplacer(HASHTAG_PATTERN, tag -> "Matched: ");

        for (String illegal: ILLEGALS) {
            // We prepend 'a' as digit-only tags are not allowed
            assertNotSame("Matched: " + illegal, replacer.apply(illegal));
        }
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
