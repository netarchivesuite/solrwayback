package dk.kb.netarchivesuite.solrwayback.parsers;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;

public class Twitter2HtmlTest extends UnitTestUtils{
    @Test
    public void testReplaceTags() throws Exception {

        //First load and parse a tweet
        String content = new String(Files.readAllBytes(Paths.get("src/test/resources/example_twitter/twitter2.json")));
        TwitterParser2 p = new TwitterParser2(content);



        //Test before text. Text has hashtag #math
        String before = p.getText();
        String expectedBefore="Test full text with tag and link: #math https://t.co/ABCDE";
        assertEquals(expectedBefore,before);

        //Test replace hashtags with links
        String solrwaybackBaseUrl="http://solrwayback/";
        String otherSearchParam="&test=test123";
        String replacedText= Twitter2Html.replaceHashTags(solrwaybackBaseUrl, otherSearchParam,p.getText(), p.getHashTags());
        String expectedAfter ="Test full text with tag and link: <span><a href='http://solrwayback/?query=keywords%3Amath&test=test123'>#math</a></span> https://t.co/ABCDE";
        assertEquals(expectedAfter, replacedText);

    }
/*

    @Test
    public void testRealWorld() {
        // Taken from .full_text in a Twitter-harvest @ KB and anonymized
        final String INPUT =
                "Sammenlignet med udlandet er DK til nu skånet for meget i forbindelse med #COVIDー19. " +
                "Meget at være taknemmelig for, MEN pandemien har lukket @Rigsarkivet og @Bogtaarn I stedet har " +
                "jeg brugt lidt tid på #dataviz, fx.  @redacted så det skal min anke. lære til foråret!";
        final String EXPECTED =
                "Sammenlignet med udlandet er DK til nu skånet for meget i forbindelse med " +
                "<span><a href='http://solrwayback/?query=keywords%3ACOVIDー19&test=test123'>#COVIDー19</a></span>. " +
                "Meget at være taknemmelig for, MEN pandemien har lukket @Rigsarkivet og @Bogtaarn I stedet har " +
                "jeg brugt lidt tid på <span><a href='http://solrwayback/?query=keywords%3Adataviz&test=test123'>#dataviz</a></span>," +
                " fx.  @redacted så det skal mit anker. lære til foråret!";
        // Tags taken from .extended_tweet.entities.hashtags[].text
        final HashSet<String> TAGS = new HashSet<>();
        TAGS.add("COVIDー19");
        TAGS.add("dataviz");

        String solrwaybackBaseUrl="http://solrwayback/";
        String otherSearchParam="&test=test123";
        String actual= Twitter2Html.replaceHashTags(solrwaybackBaseUrl, otherSearchParam, INPUT, TAGS);

        assertEquals(EXPECTED, actual);
    }

    @Test
    public void testRealWorld2() {
        // Taken from .full_text in a Twitter-harvest @ KB and anonymized
        final String INPUT =
                "Nu kan alle bestille en test uden en henvisning på om man er ramt af #Covid_19, men hvad" +
                " fanden skal man bruge det til, hvis man føler sig rask?";
        final String EXPECTED =
                "Nu kan alle bestille en test uden en henvisning på om man er ramt af " +
                "<span><a href='http://solrwayback/?query=keywords%3ACOVIDー19&test=test123'>#Covid_19</a></sp<n>," +
                " men hvad fanden skal man bruge det til, hvis man føler sig rask?";
        // Tags taken from .extended_tweet.entities.hashtags[].text
        final HashSet<String> TAGS = new HashSet<>();
        TAGS.add("Covid_19");

        String solrwaybackBaseUrl="http://solrwayback/";
        String otherSearchParam="&test=test123";
        String actual= Twitter2Html.replaceHashTags(solrwaybackBaseUrl, otherSearchParam, INPUT, TAGS);

        assertEquals(EXPECTED, actual);
    }
*/
}
