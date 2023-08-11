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
public class TimeCacheTest extends TestCase {

    public void testTimeBasedCaching() throws InterruptedException {
        TimeCache<Integer> timeCache = new TimeCache<>(100, 500);
        timeCache.get("first", () -> 1);
        Thread.sleep(10);
        assertTrue("After 10 ms the cache should hold the key/value",
                timeCache.containsKey("first"));
        Thread.sleep(500);
        assertTrue("After 510 ms the cache should no longer hold the key/value",
                timeCache.containsKey("first"));
    }

    public void testTimeBasedCachingNegative() throws InterruptedException {
        TimeCache<Integer> timeCache = new TimeCache<>(100, -500);
        timeCache.get("first", () -> 1);
        assertFalse("First entry should never be available as maxAge is < 0",
                timeCache.containsKey("first"));
    }

    public void testCountBasedCaching() {
        TimeCache<Integer> timeCache = new TimeCache<>(1, 50000);
        timeCache.get("first", () -> 1);
        assertTrue("First entry should be available initially",
                timeCache.containsKey("first"));
        timeCache.get("second", () -> 2);
        assertTrue("Second entry should be available after addition",
                timeCache.containsKey("second"));
        assertFalse("First entry should not be available after second has been added",
                timeCache.containsKey("first"));
    }
}