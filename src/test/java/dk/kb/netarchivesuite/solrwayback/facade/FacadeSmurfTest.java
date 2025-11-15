package dk.kb.netarchivesuite.solrwayback.facade;

import dk.kb.netarchivesuite.solrwayback.service.dto.smurf.DateCount;
import dk.kb.netarchivesuite.solrwayback.service.dto.smurf.SmurfBuckets;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Facade#generateNetarchiveSmurfData}.
 */
public class FacadeSmurfTest {

    private NetarchiveSolrClient originalInstance;

    /**
     * Sets up the test environment, including restoring the original NetarchiveSolrClient instance
     * and setting up a stub instance that returns deterministic counts per day.
     */
    @Before
    public void setUp() throws Exception {
        // save original instance (may be null)
        Field f = NetarchiveSolrClient.class.getDeclaredField("instance");
        f.setAccessible(true);
        originalInstance = (NetarchiveSolrClient) f.get(null);

        // create a stub instance that returns deterministic counts per day
        NetarchiveSolrClient stub = new NetarchiveSolrClient() {
            @Override
            public Long countTagHtmlForPeriod(String query, String startDate, String endDate) throws Exception {
                // return values varying by startDate
                switch (startDate) {
                    case "2020-01-01": return 5L;
                    case "2020-01-02": return 10L;
                    case "2020-01-03": return 0L;
                    default: return 0L;
                }
            }

            @Override
            public Long countTextHtmlForPeriod(String query, String startDate, String endDate) throws Exception {
                // totals per corresponding day
                switch (startDate) {
                    case "2020-01-01": return 10L;
                    case "2020-01-02": return 20L;
                    case "2020-01-03": return 5L;
                    default: return 0L;
                }
            }
        };

        // set the stub as the singleton instance
        f.set(null, stub);
    }

    /**
     * Tears down the test environment, restoring the original NetarchiveSolrClient instance.
     */
    @After
    public void tearDown() throws Exception {
        // restore original instance
        Field f = NetarchiveSolrClient.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, originalInstance);
    }

    /**
     * Verifies SmurfBuckets are generated with expected counts and percentages.
     */
    @Test
    public void testGenerateNetarchiveSmurfData_daily() throws Exception {
        LocalDate start = LocalDate.of(2020,1,1);
        LocalDate end = LocalDate.of(2020,1,3);
        String scale = "DAY";

        SmurfBuckets buckets = Facade.generateNetarchiveSmurfData("sometag", start, end, scale);

        assertNotNull(buckets);
        assertFalse("Should not be empty result", buckets.isEmptyResult());

        List<DateCount> counts = buckets.getCountsTotal();
        assertEquals(3, counts.size());

        // verify per-day counts and totals
        assertEquals("2020-01-01", counts.get(0).getDate());
        assertEquals(5L, counts.get(0).getCount());
        assertEquals(10L, counts.get(0).getTotal());

        assertEquals("2020-01-02", counts.get(1).getDate());
        assertEquals(10L, counts.get(1).getCount());
        assertEquals(20L, counts.get(1).getTotal());

        assertEquals("2020-01-03", counts.get(2).getDate());
        assertEquals(0L, counts.get(2).getCount());
        assertEquals(5L, counts.get(2).getTotal());

        List<Double> perc = buckets.getCountPercent();
        assertEquals(3, perc.size());

        // percentages are 50.0, 50.0, 0.0 (within small delta)
        assertEquals(50.0, perc.get(0), 0.0001);
        assertEquals(50.0, perc.get(1), 0.0001);
        assertEquals(0.0, perc.get(2), 0.0001);
    }
}
