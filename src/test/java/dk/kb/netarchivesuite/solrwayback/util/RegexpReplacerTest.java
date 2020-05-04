package dk.kb.netarchivesuite.solrwayback.util;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

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
}
