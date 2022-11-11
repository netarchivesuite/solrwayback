package dk.kb.netarchivesuite.solrwayback.util;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class ProcessingTest {

    @Test
    public void testBasicBatching() {
        List<Callable<Integer>> callables = new ArrayList<>();
        for (int i = 0 ; i < 100 ; i++) {
            final int value = i;
            callables.add(() -> value);
        }
        List<Integer> results = Processing.batch(callables.stream()).collect(Collectors.toList());

        assertEquals("The number of result should match the number of callables",
                     callables.size(), results.size());

        for (int i = 0 ; i < 100 ; i++) {
            assertEquals("Result #" + i + " should be as expected",
                         Integer.valueOf(i), results.get((i)));
        }
    }

    @Test
    public void testFastSet() {
        IntOpenHashSet set = new IntOpenHashSet();
        assertTrue(set.add(-704855335));
    }

    @Test
    public void testInfiniteInputStream() {
        Stream<Callable<String>> callables = Stream.generate(() -> () -> "a"); // Infinite
        long processed = Processing.batch(callables).
                limit(5000).
                count();
        assertEquals("The number of processed jobs should match the limit",
                     5000, processed);
    }
}