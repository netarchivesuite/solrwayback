package dk.kb.netarchivesuite.solrwayback.solr;

import junit.framework.TestCase;
import org.junit.Test;

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
public class CachingSolrClientTest extends TestCase {

    @Test
    public void testCacheSetting() {
        final int CACHE_TIME_SECONDS = 123;
        CachingSolrClient caching = new CachingSolrClient(null, -1, CACHE_TIME_SECONDS, 3);
        assertEquals("The query cache time should match creation time",
                CACHE_TIME_SECONDS*1000, caching.queryCache.getMaxAgeMS());
        assertEquals("The named cache (linked from query cache) time should match creation time",
                CACHE_TIME_SECONDS*1000, caching.namedCache.getMaxAgeMS());
    }

    // Failes before 2023-05-01 due to a non-working implicit cast of int->long in CachingSolrClient
    public void testCacheSettingOverflowInt() {
        final int CACHE_TIME_SECONDS = 365*24*60*60;
        CachingSolrClient caching = new CachingSolrClient(null, -1, CACHE_TIME_SECONDS, 3);
        assertEquals("The query cache time should match creation time",
                CACHE_TIME_SECONDS*1000L, caching.queryCache.getMaxAgeMS());
        assertEquals("The named cache (linked from query cache) time should match creation time",
                CACHE_TIME_SECONDS*1000L, caching.namedCache.getMaxAgeMS());
    }
}