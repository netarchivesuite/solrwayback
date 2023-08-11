package dk.kb.netarchivesuite.solrwayback.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SolrUtilsTest {

    @Test
    public void combineFilterQueriesTest(){
        String[] filtersFromFrontend = new String[]{"filter1:value1", "filter2:value2", "foo:bar OR bar:zoo"};
        String filterquery = SolrUtils.combineFilterQueries("content_type", "text/html", filtersFromFrontend);

        assertEquals("(content_type:text/html) AND (filter1:value1) AND (filter2:value2) AND (foo:bar OR bar:zoo)", filterquery);
    }
}
