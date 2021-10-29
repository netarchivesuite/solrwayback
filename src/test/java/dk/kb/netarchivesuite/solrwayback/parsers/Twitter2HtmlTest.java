package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class Twitter2HtmlTest extends UnitTestUtils{
    @Test
    public void testReplaceTags() throws Exception {
        PropertiesLoader.initProperties(getFile("properties/solrwayback.properties").getPath());
        //First load and parse a tweet
        String content = new String(Files.readAllBytes(Paths.get("src/test/resources/example_twitter/twitter2.json")));
        TwitterParser2 p = new TwitterParser2(content);

        //Test before text. Text has hashtag #math
        String before = p.getText();
        String expectedBefore = "Test full text with tag and link: #math https://t.co/ABCDE";
        assertEquals(expectedBefore, before);

        //Test replace hashtags with links
        String otherSearchParam = "&test=test123";
        String replacedText = "";//Twitter2Html.formatHashtags(p.getText(), p.getHashtags(), otherSearchParam); // TODO RBKR FIX
        String expectedAfter = "Test full text with tag and link: <span><a href='" + PropertiesLoader.WAYBACK_BASEURL
                + "?query=keywords%3Amath&test=test123'>#math</a></span> https://t.co/ABCDE";
        assertEquals(expectedAfter, replacedText);
    }

    @Test
    public void testStuff() throws IOException {
        // TODO consider making tests of below input - only problem is how..
        String text = "Are You Ready for Today? \ud83d\ude0d\nhttps://t.co/osoFEsfpIq is Open\nAll top certified brands in stock.\n#Europe #Austria #Norge #Sverige #Suomi #Denmark #Spain #Greece #Poland #Croatia #Belgium #Italia #Germany #Australia #canada #USA #malaga #SuomiAreena2021 #medvapeshop #Portugal #Oslo https://t.co/cwqw3VDfwy";
        String text2 = "Ingen har lyst til at \u00f8del\u00e6gge den gode stemning i DK med nye restriktioner selv om smitten er ved at l\u00f8be l\u00f8bsk.  Dem der tror, at de unge og de vaccinerede ikke blir s\u00e5 syge, kan lige l\u00e6se her. #COVID19dk #dkpol  https://t.co/zefrB4lTY2";
        String text3 = "Er i Frankrig (gr\u00f8nt), s\u00e5 if\u00f8lge reglerne skal man ikke vise en negativ test for at flyve til DK. Vi er dog lidt i tvivl om flyselskabet kan kr\u00e6ve det ved boarding eller om de bare f\u00f8lger reglerne til det land, man skal flyve til. Nogen der har fl\u00f8jet hjem fra \ud83c\uddeb\ud83c\uddf7? #twitterhjerne";
        System.out.println(text3.length());
        StringBuilder sb = new StringBuilder(text3);
        JSONObject json = new JSONObject("{\"hashtags\": [{\"indices\": [94, 101], \"text\": \"Europe\"}, {\"indices\": [102, 110], \"text\": \"Austria\"}, {\"indices\": [111, 117], \"text\": \"Norge\"}, {\"indices\": [118, 126], \"text\": \"Sverige\"}, {\"indices\": [127, 133], \"text\": \"Suomi\"}, {\"indices\": [134, 142], \"text\": \"Denmark\"}, {\"indices\": [143, 149], \"text\": \"Spain\"}, {\"indices\": [150, 157], \"text\": \"Greece\"}, {\"indices\": [158, 165], \"text\": \"Poland\"}, {\"indices\": [166, 174], \"text\": \"Croatia\"}, {\"indices\": [175, 183], \"text\": \"Belgium\"}, {\"indices\": [184, 191], \"text\": \"Italia\"}, {\"indices\": [192, 200], \"text\": \"Germany\"}, {\"indices\": [201, 211], \"text\": \"Australia\"}, {\"indices\": [212, 219], \"text\": \"canada\"}, {\"indices\": [220, 224], \"text\": \"USA\"}, {\"indices\": [225, 232], \"text\": \"malaga\"}, {\"indices\": [233, 249], \"text\": \"SuomiAreena2021\"}, {\"indices\": [250, 262], \"text\": \"medvapeshop\"}, {\"indices\": [263, 272], \"text\": \"Portugal\"}, {\"indices\": [273, 278], \"text\": \"Oslo\"}]}");
        JSONObject json2 = new JSONObject("{\"hashtags\": [{\"indices\": [196, 206], \"text\": \"COVID19dk\"}, {\"indices\": [207, 213], \"text\": \"dkpol\"}]}");
        JSONObject json3 = new JSONObject("{\"hashtags\": [{\"indices\": [265, 279], \"text\": \"twitterhjerne\"}]}");
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
