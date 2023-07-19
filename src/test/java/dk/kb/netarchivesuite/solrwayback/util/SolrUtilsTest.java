package dk.kb.netarchivesuite.solrwayback.util;

import org.junit.Test;

public class SolrUtilsTest {

    @Test
    public void combineFilterQueriesTest(){
        String[] filtersFromFrontend = new String[]{"filter1:value1", "filter2:value2"};
        String filterquery = SolrUtils.combineFilterQueries("content_type", "text/html", filtersFromFrontend);

        System.out.println(filterquery);
    }
}
