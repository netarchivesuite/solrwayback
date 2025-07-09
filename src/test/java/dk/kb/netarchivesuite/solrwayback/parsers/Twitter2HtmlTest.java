package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;

// TODO REWRITE ME
public class Twitter2HtmlTest extends UnitTestUtils{
    /*@Test
    public void testFormatTweetText() throws Exception {
        PropertiesLoader.initProperties(getFile("properties/solrwayback.properties").getPath());
        //First load and parse a tweet
        String content = new String(Files.readAllBytes(Paths.get("src/test/resources/example_twitter/twitter2.json")));
        TwitterParser2 p = new TwitterParser2(content);

        //Test before text. Text has hashtag #math
        String before = p.getText();
        String expectedBefore = "Test with links https://t.co/ABC123DEFG filler text for no reason but to fill\n" +
                "There is even one link in this tweet https://t.co/W1ldUr7w0W. The text goes even further beyond what" +
                " is thought possible! What is this math? https://t.co/rABCDEFGHI #math  https://t.co/ABCDEFGHIJ";
        assertEquals(expectedBefore, before);

        //Test replacing of hashtags and urls with links
        String textAfterFormatting = Twitter2Html.formatTweetText(before, p.getTweetMinDisplayTextRange(), p.getHashtags(), p.getMentions(), p.getURLs());
        String expectedAfter = "Test with links <span><a href='https://twitter.com/i/web/status/1234'>twitter.com/i/web/status/1…</a></span>" +
                " filler text for no reason but to fill<br>There is even one link in this tweet" +
                " <span><a href='https://twitter.com/i/web/status/1234'>twitter.com/i/web/status/1…</a></span>." +
                " The text goes even further beyond what is thought possible! What is this math?" +
                " <span><a href='http://thomas-egense.dk/math/'>thomas-egense.dk/math/</a></span>" +
                " <span><a href='http://localhost:8080/solrwayback/search?query=keywords%3Amath AND type%3A\"Twitter Tweet\"'>#math</a></span>  ";
        assertEquals(expectedAfter, textAfterFormatting);
    }*/

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
