package dk.kb.netarchivesuite.solrwayback.playback;

/**
 * Shared test values for the {@link PlaybackHandler} tests.
 */
final class PlaybackTestData {

    static final String BASEURL = "http://localhost:0000/solrwayback/";
    static final String CRAWL_DATE = "2020-04-30T13:07:00Z";
    static final String WAYBACK_DATE = "20200430130700";
    static final String SOURCE_FILE = "/warcs/example.warc.gz";
    static final long OFFSET = 1234L;

    private PlaybackTestData() {
    }
}
