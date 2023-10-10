package dk.kb.netarchivesuite.solrwayback.util;

import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class CollectionUtilsTest {

    @Test
    public void interleave() {
        List<String> interleaved = CollectionUtils.interleave(
                Stream.of("a", "b", "c", "d", "e"), 
                Stream.of("1", "2", "3")).collect(Collectors.toList());
        assertEquals("Interleaving with default ratios should yield the expected result",
                     "[a, 1, b, 2, c, 3, d, e]", interleaved.toString());
    }

    @Test
    public void interleaveRatio() {
        List<String> interleaved = CollectionUtils.interleave(
                Arrays.asList(
                        Stream.of("a", "b", "c", "d", "e"),
                        Stream.of("1", "2", "3")
                ),
                Arrays.asList(2, 1)
        ).collect(Collectors.toList());
        assertEquals("Interleaving with default ratios should yield the expected result",
                     "[a, b, 1, c, d, 2, e, 3]", interleaved.toString());
    }

    @Test
    public void testPeekableIterator() {
        Iterator<Integer> pi = Arrays.asList(1, 2, 3).iterator();
        CollectionUtils.PeekableIterator<Integer> pit =CollectionUtils.PeekableIterator.of(pi);

        assertEquals("First peek should yield the expected result", Integer.valueOf(1), pit.peek());
        assertEquals("Second peek should yield the expected result", Integer.valueOf(1), pit.peek());
        assertEquals("First get should yield the expected result", Integer.valueOf(1), pit.next());
        assertEquals("Third peek should yield the expected result", Integer.valueOf(2), pit.peek());
        assertTrue("First hasNext() should yield the expected result", pit.hasNext());
        assertEquals("Second get should yield the expected result", Integer.valueOf(2), pit.next());
        assertTrue("Second hasNext() should yield the expected result", pit.hasNext());
        assertEquals("Fourth peek should yield the expected result", Integer.valueOf(3), pit.peek());
        assertEquals("Third get should yield the expected result", Integer.valueOf(3), pit.next());
        assertFalse("Third hasNext() should yield the expected result", pit.hasNext());
    }

    @Test
    public void testReducingIterator() {
        Iterator<Integer> pi = Arrays.asList(1, 2, 3, 4, 5, 6, 7).iterator();
        Iterator<Integer> eveni = CollectionUtils.ReducingIterator.of(pi, i -> (i&0x1) == 0);
        List<Integer> evenl = new ArrayList<>();
        eveni.forEachRemaining(evenl::add);
        assertEquals("The reduced iterator should produce only even numbers",
                     "[2, 4, 6]", evenl.toString());
    }

    @Test
    public void testExpandingIterator() {
        Iterator<Integer> pi = Arrays.asList(1, 3, 5).iterator();

        Iterator<Integer> plusonei = CollectionUtils.ExpandingIterator.of(
                pi, i -> Arrays.stream(new Integer[]{i, i+1}).iterator());
        List<Integer> plusonel = new ArrayList<>();
        plusonei.forEachRemaining(plusonel::add);
        assertEquals("The expanding iterator should produce the missing numbers",
                     "[1, 2, 3, 4, 5, 6]", plusonel.toString());
    }

    @Test
    public void testPeekableIteratorEmpty() {
        Iterator<Integer> pi = new ArrayList<Integer>().iterator();
        CollectionUtils.PeekableIterator<Integer> pit =CollectionUtils.PeekableIterator.of(pi);

        assertEquals("First peek should yield the expected result", null, pit.peek());
        assertFalse("First hasNext() should yield the expected result", pit.hasNext());
    }

    @Test
    public void testSharedConstraintIterator() throws InterruptedException {
        Semaphore gatekeeper = new Semaphore(1);
        gatekeeper.acquire();
        Iterator<Integer> pi = Arrays.asList(1, 2, 3).iterator();
        Iterator<Integer> scit = CollectionUtils.SharedConstraintIterator.of(pi, gatekeeper);

        long replyTime = -System.currentTimeMillis();
        new Thread(() -> {
            try {
                Thread.sleep(200);
                gatekeeper.release();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        assertEquals("First call to next() should yield the expected result", Integer.valueOf(1), scit.next());
        replyTime += System.currentTimeMillis();

        assertTrue("It should take at least 200ms for first element, but it took " + replyTime, replyTime >= 200);
    }

    @Test
    public void testBufferingIterator() throws InterruptedException {
        Executor executor = Executors.newCachedThreadPool();
        AtomicBoolean continueProcessing = new AtomicBoolean(true);
        CountingIterator<Integer> pic = new CountingIterator<>(Arrays.asList(1, 2, 3).iterator());
        Iterator<Integer> pib = CollectionUtils.BufferingIterator.of(pic, executor, 2, continueProcessing);
        Thread.sleep(100); // Leave time for reading ahead
        assertEquals("The right number of elements should have been read ahead", 2, pic.delivered);
        assertTrue("First hasNext() should provide the expected result", pib.hasNext());
        pib.next();
        assertTrue("Second hasNext() should provide the expected result", pib.hasNext());
        pib.next();
        assertTrue("Third hasNext() should provide the expected result", pib.hasNext());
        pib.next();
        assertFalse("Fourth hasNext() should provide the expected result", pib.hasNext());
    }

    @Test
    public void testBufferingSlowIterator() {
        Executor executor = Executors.newCachedThreadPool();
        AtomicBoolean continueProcessing = new AtomicBoolean(true);
        CountingIterator<Integer> pic = new CountingIterator<>(Arrays.asList(1, 2, 3).iterator());
        DelayingIterator<Integer> pid = new DelayingIterator<>(pic, 50);
        Iterator<Integer> pib = CollectionUtils.BufferingIterator.of(pid, executor, 20, continueProcessing);
        // Start reading result immediately
        assertEquals("The right number of elements should have been read ahead", 0, pic.delivered);
        assertTrue("First hasNext() should provide the expected result", pib.hasNext());
        pib.next();
        assertTrue("Second hasNext() should provide the expected result", pib.hasNext());
        pib.next();
        assertTrue("Third hasNext() should provide the expected result", pib.hasNext());
        pib.next();
        assertFalse("Fourth hasNext() should provide the expected result", pib.hasNext());
    }

    @Test
    public void testBufferingIteratorStop() throws InterruptedException {
        Executor executor = Executors.newCachedThreadPool();
        AtomicBoolean continueProcessing = new AtomicBoolean(true);
        CountingIterator<Integer> pic = new CountingIterator<>(Arrays.asList(1, 2, 3).iterator());
        Iterator<Integer> pib = CollectionUtils.BufferingIterator.of(pic, executor, 2, continueProcessing);
        Thread.sleep(100); // Leave time for reading ahead
        assertEquals("The right number of elements should have been read ahead", 2, pic.delivered);
        assertTrue("First hasNext() should provide the expected result", pib.hasNext());
        continueProcessing.set(false);
        assertFalse("Second hasNext() should provide the expected result", pib.hasNext());
    }

    @Test
    public void testBufferingIteratorEmpty() throws InterruptedException {
        Executor executor = Executors.newCachedThreadPool();
        AtomicBoolean continueProcessing = new AtomicBoolean(true);
        CountingIterator<Integer> pic = new CountingIterator<>(new ArrayList<Integer>().iterator());
        Iterator<Integer> pib = CollectionUtils.BufferingIterator.of(pic, executor, 2, continueProcessing);
        Thread.sleep(100); // Leave time for reading ahead
        assertEquals("The right number of elements should have been read ahead", 0, pic.delivered);
        assertFalse("First hasNext() should provide the expected result", pib.hasNext());
    }

    @Test
    public void testMergeIterators() {
        Iterator<Integer> i1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> i2 = Arrays.asList(2, 3, 4).iterator();
        Iterator<Integer> merged = CollectionUtils.mergeIterators(Arrays.asList(i1, i2), Integer::compare);
        List<Integer> mergedL = new ArrayList<>();
        merged.forEachRemaining(mergedL::add);
        assertEquals("[1, 2, 3, 3, 4, 5]", mergedL.toString());
    }

    @Test
    public void testBufferingMerge() throws InterruptedException {
        Executor executor = Executors.newCachedThreadPool();
        Semaphore gatekeeper = new Semaphore(2);
        CountingIterator<Integer> pic1 = new CountingIterator<>(Arrays.asList(1, 3, 5).iterator());
        CountingIterator<Integer> pic2 = new CountingIterator<>(Arrays.asList(2, 4).iterator());
        try (CollectionUtils.CloseableIterator<Integer> ci = CollectionUtils.mergeIteratorsBuffered(
                Arrays.asList(pic1, pic2), Integer::compareTo, executor, gatekeeper, 1)) {
            while(ci.hasNext()) {
                System.out.println(ci.next());
            }
        };
    }


    private static class CountingIterator<T> implements Iterator<T> {
        private final Iterator<T> inner;
        private int delivered = 0;

        public CountingIterator(Iterator<T> inner) {
            this.inner = inner;
        }

        @Override
        public boolean hasNext() {
            return inner.hasNext();
        }

        @Override
        public T next() {
            T result = inner.next();
            delivered++;
            return result;
        }

        public int getDelivered() {
            return delivered;
        }
    }

    private static class DelayingIterator<T> implements Iterator<T> {
        private final Iterator<T> inner;
        private final long sleepMS;

        public DelayingIterator(Iterator<T> inner, long sleepMS) {
            this.inner = inner;
            this.sleepMS = sleepMS;
        }

        @Override
        public boolean hasNext() {
            return inner.hasNext();
        }

        @Override
        public T next() {
            try {
                Thread.sleep(sleepMS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return inner.next();
        }
    }
}