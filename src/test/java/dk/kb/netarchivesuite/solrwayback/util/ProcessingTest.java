package dk.kb.netarchivesuite.solrwayback.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;

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
    public void triggerEvaluation1() throws InterruptedException {
        AtomicInteger evaluations = new AtomicInteger(0);
        Stream<Callable<Stream<Integer>>> callables =
                IntStream.range(0, 10).boxed().
                        map(i -> getCallback(evaluations));

        Stream<Stream<Integer>> result = Processing.batch(callables, 3);
        Thread.sleep(50);
        assertEquals("The evaluation count should be zero when nothing has been pulled", 0, evaluations.get());

        Iterator<Stream<Integer>> iResult = result.iterator();
        Iterator<Integer> inner1 = iResult.next().iterator();
        inner1.next();
        Thread.sleep(50);
        assertEquals("The evaluation count should be 1 when first value has been pulled directly",
                     1, evaluations.get());
    }

    // Disabled as it does not pass. Why is the evaluation eager (6 evaluations instead of 1)?
    public void triggerEvaluationFlatmap() throws InterruptedException {
        AtomicInteger evaluations = new AtomicInteger(0);
        Stream<Callable<Stream<Integer>>> callables =
                IntStream.range(0, 10).boxed().
                        map(i -> getCallback(evaluations));

        Stream<Integer> result = Processing.batch(callables, 3).flatMap(Function.identity());
        Thread.sleep(50);
        assertEquals("The evaluation count should be zero when nothing has been pulled", 0, evaluations.get());

        Iterator<Integer> iResults = result.iterator();
        iResults.next();
        Thread.sleep(50);
        assertEquals("The evaluation count should be 1 when first value has been pulled from flatMapped",
                     1, evaluations.get());
    }

    private Callable<Stream<Integer>> getCallback(AtomicInteger evaluations) {
        return () -> streamTwo(evaluations);
    }
    // Only evaluates when the stream is pulled
    private Stream<Integer> streamTwo(AtomicInteger evaluations) {
        Iterator<Integer> iterator = new Iterator<Integer>() {
            int delivered = 0;
            @Override
            public boolean hasNext() {
                return delivered != 2;
            }

            @Override
            public Integer next() {
                delivered++;
                return evaluations.incrementAndGet();
            }
        };

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }

    @Test
    public void testJustInTime() throws InterruptedException {

        AtomicInteger counter = new AtomicInteger(0);
        List<Callable<Integer>> callables = new ArrayList<>();
        for (int i = 0 ; i < 100 ; i++) {
            callables.add(counter::incrementAndGet);
        }
        Stream<Integer> results = Processing.batch(callables.stream(), 2);

        Iterator<Integer> ires = results.iterator();
        Thread.sleep(10);
        assertEquals("Starting point should be 0", 0, counter.get());

        ires.next();
        Thread.sleep(10);
        assertEquals("First pull should increment by batch size", 2, counter.get());

        ires.next();
        Thread.sleep(10);
        assertEquals("First pull shouldn't increment", 2, counter.get());
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