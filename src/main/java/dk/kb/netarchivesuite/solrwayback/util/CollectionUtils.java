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
package dk.kb.netarchivesuite.solrwayback.util;

import com.google.common.collect.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Spliterators;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CollectionUtils {
    private static final Logger log = LoggerFactory.getLogger(CollectionUtils.class);

    /**
     * Interleave the given Streams, passing 1 element from each non-depleted stream then starting over until there
     * are no more elements in any stream. This is equivalent to calling {@link #interleave(List, List)} with a list
     * of ratios where each ratio is 1.
     *
     * {code interleave(Stream.of("a", "b", "c", "d", "e"), Stream.of("1", "2", "3"))} becomes
     * {@code "a", "1", "b", "2", "c", "3", "d", "e"}
     * @param streams content from multiple sources that should be interleaved.
     * @return a stream with all the elements from the given streams, interleaved 1 element at a time.
     */
    @SafeVarargs
    public static <T> Stream<T> interleave(Stream<T>... streams) {
        return interleave(Arrays.asList(streams), null);
    }

    /**
     * Interleave the given list of Streams with the given ratio. The ratios define how many elements to deliver from
     * the current stream before switching to the next stream. When the last stream has delivered its ratio, the process
     * starts over with the first stream in the list. When a stream is depleted it is ignored.
     *
     * {code interleave(Arrays.asList(Stream.of("a", "b", "c", "d", "e"), Stream.of("1", "2", "3")), Arrays.asList(2, 1))}
     * becomes {@code "a", "b", "1", "c", "d", "2", "e", "3}
     * @param streams content from multiple sources that should be interleaved.
     * @param ratios the interleaving ratios. If null, a list is created where each entry is {@code 1}.
     * @return a stream with all the elements from the given streams, interleaved as defined by the ratios.
     */
    public static <T> Stream<T> interleave(List<Stream<T>> streams, List<Integer> ratios) {
        List<Iterator<T>> iterators = streams.stream().
                map(BaseStream::iterator).
                collect(Collectors.toList());
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(interleaveIterators(iterators, ratios), 0),
                false);
    }

    /**
     * Interleave the given list of Iterators with the given ratio. The ratios define how many elements to deliver from
     * the current stream before switching to the next iterator. When the last iterator has delivered its ratio, the
     * process starts over with the first iterator in the list. When an iterator is depleted it is ignored.
     *
     * {code interleave(Arrays.asList(("a", "b", "c", "d", "e").iterator(), ("1", "2", "3").iterator()), List.of(2, 1))}
     * becomes {@code "a", "b", "1", "c", "d", "2", "e", "3}
     * @param iterators content from multiple sources that should be interleaved.
     * @param ratios the interleaving ratios. If null, a list is created where each entry is {@code 1}.
     * @return an iterator with all the elements from the given iterators, interleaved as defined by the ratios.
     */
    public static <T> Iterator<T> interleaveIterators(List<Iterator<T>> iterators, List<Integer> ratios) {
        // Validate ratios
        if (ratios == null) {
            ratios = new ArrayList<>(iterators.size());
            for (int i = 0 ; i < iterators.size() ; i++) {
                ratios.add(1);
            }
        }
        ratios.forEach(r -> { if (r < 1) {
            throw new IllegalArgumentException("One ratio was " + r + ". All ratios must be >= 1");
        }});
        if (iterators.size() != ratios.size()) {
            throw new IllegalArgumentException(
                    "Got " + iterators.size() + " streams and " + ratios.size() + " ratios. The counts should be equal");
        }
        if (iterators.isEmpty()) {
            return Collections.emptyIterator();
        }
        final List<Integer> fRatios = ratios;

        // Construct interleaving iterator
        return new Iterator<T>() {
            int iIndex = 0;
            int delivered = 0;

            @Override
            public boolean hasNext() {
                return iterators.stream().anyMatch(Iterator::hasNext);
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new IllegalStateException("next() called when hasNext() == false. No more elements");
                }
                while (true) {
                    if (delivered++ == fRatios.get(iIndex) || !iterators.get(iIndex).hasNext()) {
                        delivered = 0;
                        if (++iIndex == iterators.size()) {
                            iIndex = 0;
                        }
                        continue;
                    }
                    return iterators.get(iIndex).next();
                }
            }
        };
    }

    // splitTo* copy-pasted unchanged from https://github.com/kb-dk/kb-util/

    /**
     * Lazily partition the input to the given partitionSize.
     *
     * All partitions will have exactly partitionSize elements, except for the last partition which will contain
     * {@code input_size % partitionSize} elements.
     *
     * The implementation is fully streaming and only holds the current partition in memory.
     *
     * The implementation does not support parallelism: If source is parallel, it will be sequentialized.
     *
     * If the end result should be a list of lists, use {@code splitToList(myStream, 87).collect(Collectors.toList())}.
     * @param source any stream.
     * @param partitionSize the maximum size for the partitions.
     * @return the input partitioned into lists, each with partitionSize elements.
     */
    public static <T> Stream<List<T>> splitToLists(Stream<T> source, int partitionSize) {
        return splitToStreams(source, partitionSize).map(stream -> stream.collect(Collectors.toList()));
    }

    /**
     * Lazily partition the input to the given partitionSize.
     *
     * All partitions will have exactly partitionSize elements, except for the last partition which will contain
     * {@code input_size % partitionSize} elements.
     *
     * The implementation is fully streaming and only holds the current partition in memory.
     *
     * The implementation does not support parallelism: If source is parallel, it will be sequentialized.
     * @param source any stream.
     * @param partitionSize the maximum size for the partitions.
     * @return the input partitioned into streams, each with partitionSize elements.
     */
    public static <T> Stream<Stream<T>> splitToStreams(Stream<T> source, int partitionSize) {
        // https://stackoverflow.com/questions/32434592/partition-a-java-8-stream
        final Iterator<T> it = source.iterator();
        final Iterator<Stream<T>> partIt = Iterators.transform(Iterators.partition(it, partitionSize), List::stream);
        final Iterable<Stream<T>> iterable = () -> partIt;

        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Iterator-wrapper that buffers a stated amount of elements using a background thread.
     * <p>
     * Use case: If an iterator is staggering, i.e. it has low latency for most of the elements
     * interrupted by high latency or a single element, wrapping it in the {@code BufferingIterator}
     * will make the high latency points go away or at least be less significant by reading ahead.
     * <p>
     * Note: Creating an instance of the {@code BufferingIterator} will immediately result in
     * background read-ahead, even though {@link Iterator#next()} has not been called.
     */
    public static class BufferingIterator<T> implements Iterator<T> {

        static long OFFER_TIMEOUT_MS = 10*1000;
        static long POLL_TIMEOUT_MS = 1000;

        private final AtomicBoolean continueIterating;

        private final BlockingQueue<T> buffer;
        private final AtomicBoolean innerIsEmpty = new AtomicBoolean(false);
        private T nextElement;

        /**
         * Wrap the given {@code inner} {@link Iterable} and start a background pre-fetch of {@code maxBufferSize}
         * elements.
         * @param inner      any iterator.
         * @param executor   used for issuing background requests. This must be unbounded to avoid deadlocks!
         * @param bufferSize the maximum and ideal size of the buffer.
         * @param continueProcessing shared state for multiple iterators.
         *                           Any failed operation on {@code inner} will result in this being set to false.
         *                           If false, all state-sharing iterators should stop processing as soon as convenient.
         */
        public static <T> BufferingIterator<T> of(
                Iterator<T> inner, Executor executor, int bufferSize, AtomicBoolean continueProcessing) {
            return new BufferingIterator<>(inner, executor, bufferSize, continueProcessing);
        }

        /**
         * Wrap the given {@code inner} {@link Iterable} and start a background pre-fetch of {@code maxBufferSize}
         * elements.
         * @param inner      any iterator.
         * @param executor   used for issuing background requests. This must be unbounded to avoid deadlocks!
         * @param bufferSize the maximum and ideal size of the buffer. If {@code < 2} this will be set to {@code 2}.
         * @param continueProcessing shared state for multiple iterators.
         *                           Any failed operation on {@code inner} will result in this being set to false.
         *                           If false, all state-sharing iterators should stop processing as soon as convenient.
         */
        public BufferingIterator(Iterator<T> inner, Executor executor, int bufferSize, AtomicBoolean continueProcessing) {
            this.continueIterating = continueProcessing;
            // bufferSize-1 as we count "the one in the chamber" aka the element being offered to the queue
            buffer = new ArrayBlockingQueue<>(Math.max(1, bufferSize-1));
            executor.execute(() -> {
                while (continueProcessing.get()) {
                    if (!inner.hasNext()) {
                        innerIsEmpty.set(true);
                        return;
                    }

                    T next;
                    try {
                        // Typically a staggering call, e.g. every 100th call requires a remote request
                        next = inner.next();
                    } catch (Exception e) {
                        log.warn("Exception while requesting inner.next(). " +
                                 "Signalling stop to all state sharing iterators", e);
                        continueProcessing.set(false);
                        throw e;
                    }

                    boolean isAdded = false;
                    while (continueProcessing.get() && !isAdded) {
                        try {
                            // The buffer is blocking with fixed size so most of the Thread time will be spend waiting
                            isAdded = buffer.offer(next, OFFER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            // no-op as continueIterating signals whether to continue processing or not
                        } catch (Exception e) {
                            log.warn("Unexpected Exception while offering element. " +
                                     "Signalling stop to all state sharing iterators", e);
                            continueProcessing.set(false);
                            throw e;
                        }
                    }
                }
            });
        }

        /**
         * Blocking call that sets {@link #nextElement}. Setting {@code #nextElement} to null signals no more elements.
         */
        private void ensureElement() {
            if (nextElement != null) {
                return;
            }
            while (nextElement == null && continueIterating.get() && !innerIsEmpty.get()) {
                try {
                    nextElement = buffer.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    // no-op as continueIterating & innerIsEmpty signals whether to continue processing or not
                }
            }
        }

        /**
         * @return the element that will be delivered by the next call to {@link #hasNext}.
         *         If the iterator is depleted, null will be returned.
         */
        public T peek() {
            ensureElement();
            return nextElement;
        }
        @Override
        public boolean hasNext() {
            ensureElement();
            return nextElement != null && continueIterating.get();
        }

        @Override
        public T next() {
            ensureElement();
            if (nextElement == null) {
                throw new IllegalStateException("next() called when hasNext() == false");
            }
            T resultElement = nextElement;
            nextElement = null;
            return resultElement;
        }
    }

    /**
     * Iterator wrapper that takes a source {@link Iterator} and a {@link Predicate} acting as a filter on the
     * iterator.
     */
    public static class ReducingIterator<T> implements Iterator<T> {
        private final Iterator<T> inner;
        private final Predicate<T> predicate;
        private T nextElement = null;

        /**
         * Iterator wrapper that takes a source {@link Iterator} and a {@link Predicate} acting as a filter on the
         * iterator.
         * @param inner     any iterator.
         * @param predicate only elements from inner that satisfies the predicate are passed on.
         */
        public static <T> ReducingIterator<T> of(Iterator<T> inner, Predicate<T> predicate) {
            return new ReducingIterator<>(inner, predicate);
        }

        /**
         * @param inner     any iterator.
         * @param predicate only elements from inner that satisfies the predicate are passed on.
         */
        public ReducingIterator(Iterator<T> inner, Predicate<T> predicate) {
            this.inner = inner;
            this.predicate = predicate;
        }

        /**
         * @return the element that will be delivered by the next call to {@link #hasNext}.
         * If the iterator is depleted, null will be returned.
         */
        public T peek() {
            fillBuffer();
            return nextElement;
        }

        @Override
        public boolean hasNext() {
            fillBuffer();
            return nextElement != null;
        }

        @Override
        public T next() {
            fillBuffer();
            if (nextElement == null) {
                throw new IllegalStateException("next() called when hasNext() == false");
            }
            T resultElement = nextElement;
            nextElement = null;
            return resultElement;
        }

        /**
         * Keep retrieving elements from {@link #inner} and until one satisfies {@link #predicate} or until
         * {@code inner} is empty.
         */
        private void fillBuffer() {
            while (nextElement == null && inner.hasNext()) {
                T innerNext = inner.next();
                if (predicate.test(innerNext)) {
                    nextElement = innerNext;
                }
            }
        }
    }

    /**
     * Iterator wrapper that takes a source {@link Iterator} and a {@link Function} that expands any element from
     * the {@code iterator} to a new {@link Iterator}.
     */
    public static class ExpandingIterator<T> implements Iterator<T> {
        private final Iterator<T> inner;
        private final Function<T, Iterator<T>> expander;
        private Iterator<T> expandedIterator = null; // Only assigned if hasNext()

        /**
         * Construct a wrapper that takes a source {@link Iterator} and a {@link Function} that expands any element from
         * the {@code iterator} to a new {@link Iterator}.
         * @param inner any iterator.
         * @param expander takes an element from {@code inner} and delivers an iterator.
         */
        public static <T> ExpandingIterator<T> of(Iterator<T> inner, Function<T, Iterator<T>> expander) {
            return new ExpandingIterator<>(inner, expander);
        }

        /**
         * Helper constructor that takes a Stream expander and converts it to an Iterator expander,
         * then feeds it ti {@link #of(Iterator, Function)}.
         * @param inner any iterator.
         * @param expander takes an element from {@code inner} and delivers a stream of elements.
         */
        public static <T> Iterator<T> ofStream(Iterator<T> inner, Function<T, Stream<T>> expander) {
            return of(inner, element -> expander.apply(element).iterator());
        }

        /**
         * @param inner any iterator.
         * @param expander takes an element from {@code inner} and delivers an iterator.
         */
        public ExpandingIterator(Iterator<T> inner, Function<T, Iterator<T>> expander) {
            this.inner = inner;
            this.expander = expander;
        }

        @Override
        public boolean hasNext() {
            fillBuffer();
            return expandedIterator != null && expandedIterator.hasNext();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new IllegalStateException("next() called when hasNext() == false");
            }
            return expandedIterator.next();
        }

        /**
         * Keep retrieving elements from {@link #inner} and pass them through {@link #expander} until the result
         * {@link Iterator#hasNext()} or until {@code inner} is empty.
         */
        private void fillBuffer() {
            while ((expandedIterator == null || !expandedIterator.hasNext()) && inner.hasNext()) {
                expandedIterator = expander.apply(inner.next());
            }
        }
    }

    /**
     * Iterator wrapper that takes a source {@link Iterator} and a {@link UnaryOperator} that adjusts elements from
     * the {@code iterator}.
     */
    public static class AdjustingIterator<T> implements Iterator<T> {
        private final Iterator<T> inner;
        private final UnaryOperator<T> adjuster;

        /**
         * Construct an iterator wrapper that takes a source {@link Iterator} and a {@link UnaryOperator} that adjusts
         * elements from the {@code iterator}.
         * @param inner any iterator.
         * @param adjuster adjusts elements {@code inner}.
         */
        public static <T> AdjustingIterator<T> of(Iterator<T> inner, UnaryOperator<T> adjuster) {
            return new AdjustingIterator<>(inner, adjuster);
        }

        /**
         * @param inner any iterator.
         * @param adjuster adjusts elements {@code inner}.
         */
        public AdjustingIterator(Iterator<T> inner, UnaryOperator<T> adjuster) {
            this.inner = inner;                          
            this.adjuster = adjuster;
        }

        @Override
        public boolean hasNext() {
            return inner.hasNext();
        }

        @Override
        public T next() {
            return adjuster.apply(inner.next());
        }
    }

    /**
     * Iterator wrapper that adds the method {@code peek} for peeking the next value of {@code next()}.
     */
    public static class PeekableIterator<T> implements Iterator<T> {
        private final Iterator<T> inner;
        private T nextElement = null;

        /**
         * If the given {@code inner} is already a {@code PeekableIterator}, it is returned unmodified.
         * Else it is wrapped in a {@code PeekableIterator}.
         * @param inner any iterator.
         * @return a {@code PeekableIterator} delivering the elements from {@code inner}.
         */
        public static <T> PeekableIterator<T> of(Iterator<T> inner) {
            return inner instanceof PeekableIterator ? (PeekableIterator<T>)inner : new PeekableIterator<>(inner);
        }

        protected PeekableIterator(Iterator<T> inner) {
            this.inner = inner;
        }

        /**
         * @return the element that will be delivered by the next call to {@link #hasNext}.
         *         If the iterator is depleted, null will be returned.
         */
        public T peek() {
            fillBuffer();
            return nextElement;
        }

        @Override
        public boolean hasNext() {
            fillBuffer();
            return nextElement != null;
        }

        private void fillBuffer() {
            if (nextElement == null && inner.hasNext()) {
                nextElement = inner.next();
            }
        }

        @Override
        public T next() {
            fillBuffer();
            if (nextElement == null) {
                throw new IllegalStateException("next() called when hasNext() == false");
            }
            T resultElement = nextElement;
            nextElement = inner.hasNext() ? inner.next() : null;
            return resultElement;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported for PeekableIterators");
        }
    }

    /**
     * Iterator wrapper where calls to the inner {@link Iterator#hasNext()} and {@link Iterator#next()} shares a
     * constraint with other iterators on the number of concurrent calls.
     */
    public static class SharedConstraintIterator<T> implements Iterator<T> {
        private final Iterator<T> inner;
        private final Semaphore gatekeeper;

        /**
         * Wraps the given {@code inner} Iterator and calls {@link Semaphore#acquire()} in the {@code gatekeeper}
         * on {@code inner methods}.
         * @param inner      any Iterator.
         * @param gatekeeper the {@link Semaphore} controlling concurrent calls.
         */
        public static <T> SharedConstraintIterator<T> of(Iterator<T> inner, Semaphore gatekeeper) {
            return new SharedConstraintIterator<>(inner, gatekeeper);
        }

        /**
         * Wraps the given {@code inner} Iterator and calls {@link Semaphore#acquire()} in the {@code gatekeeper}
         * on {@code inner methods}.
         * @param inner      any Iterator.
         * @param gatekeeper the {@link Semaphore} controlling concurrent calls.
         */
        public SharedConstraintIterator(Iterator<T> inner, Semaphore gatekeeper) {
            this.inner = inner;
            this.gatekeeper = gatekeeper;
        }

        @Override
        public boolean hasNext() {
            acquire("Interrupted while waiting for semaphore in hasNext(). Retrying");

            try {
                return inner.hasNext();
            } finally {
                gatekeeper.release();
            }
        }

        @Override
        public T next() {
            acquire("Interrupted while waiting for semaphore in next(). Retrying");

            try {
                return inner.next();
            } finally {
                gatekeeper.release();
            }
        }

        /**
         * Acqire the semaphore. Loops if interrupted.
         * @param message logged if interrupted.
         */
        private void acquire(String message) {
            while (true) {
                try {
                    gatekeeper.acquire();
                    break;
                } catch (InterruptedException e) {
                    log.warn(message);
                }
            }
        }
    }

    /**
     * Order based merge of {@code streams}. The elements in the {@code streams} must be in the same order as
     * ensured by the provided {@code comparator}. The merge uses a {@link PriorityQueue} for ordering the
     * {@code streams} and have a total processing time of {@code O(n*log(s)} where {@code n} is the total
     * number of elements in all streams combined and {@code s} is the number of streams.
     * <p>
     * This method is a wrapper for {@link #mergeIterators(Collection, Comparator)} that handles conversion
     * from streams to iterators and vice versa.
     * @param streams    0 or more streams where the elements are in {@code comparator} order.
     * @param comparator a comparator matching the order in the {@code streams}.
     * @return a stream delivering all elements in all provided {@code streams} in {@code comparator} order.
     * @param <T> any class.
     * @see #mergeIterators(Collection, Comparator)           
     */
    public static <T> Stream<T> mergeStreams(Collection<Stream<T>> streams, Comparator<T> comparator) {
        Iterator<T> iterator = mergeIterators(
                streams.stream()
                        .map(Stream::iterator)
                        .collect(Collectors.toList()),
                comparator);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }

    /**
     * Order based merge of {@code iterators}. The elements in the {@code iterators} must be in the same order as
     * ensured by the provided {@code comparator}. The merge uses a {@link PriorityQueue} for ordering the
     * {@code iterators} and have a total processing time of {@code O(n*log(s)} where {@code n} is the total
     * number of elements in all iterators combined and {@code s} is the number of iterators.
     * @param iterators  0 or more iterators where the elements are in {@code comparator} order.
     * @param comparator a comparator matching the order in the {@code iterators}.
     * @return a stream delivering all elements in all provided {@code iterators} in {@code comparator} order.
     * @param <T> any class.
     * @see #mergeStreams(Collection, Comparator)            
     */
    public static <T> Iterator<T> mergeIterators(Collection<Iterator<T>> iterators, Comparator<T> comparator) {
        // Wrap iterators as peekable and add them all to a prority queue using the given comparator (also wrapped)
        Comparator<PeekableIterator<T>> peekComparator = (o1, o2) -> comparator.compare(o1.peek(), o2.peek());
        final PriorityQueue<PeekableIterator<T>> pq = new PriorityQueue<>(iterators.size(), peekComparator);
        iterators.stream()
                // All iterators needs at least 1 element to be relevant
                .filter(Iterator::hasNext)
                .map(PeekableIterator::new)
                .forEach(pq::add);

        // Create a new iterator that
        // 1) pop iterator from the top of the priority queue
        // 2) deliver the next()element from the iterator
        // 3) push the iterator back on the queue, if the iterator hasNext()
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return !pq.isEmpty();
            }

            @SuppressWarnings("ConstantConditions")
            @Override
            public T next() {
                PeekableIterator<T> pi = pq.poll();
                T response = pi.next(); // We only insert iterators that hasNext() so we can skip the extra check
                if (pi.hasNext()) {
                    pq.add(pi);
                }
                return response;
            }
        };
    }

    /**
     * Special purpose Iterator used by {@link #mergeIteratorsBuffered(Collection, Comparator, Executor, Semaphore, int)}
     * for signalling cancellation in case of early iterator termination.
     */
    public static class CloseableIterator<T> implements Iterator<T>, Closeable {
        private final Iterator<T> inner;
        private final AtomicBoolean continueProcessing;

        public static <T> CloseableIterator<T> of(Iterator<T> inner, AtomicBoolean continueProcessing) {
            return new CloseableIterator<>(inner, continueProcessing);
        }

        public CloseableIterator(Iterator<T> inner, AtomicBoolean continueProcessing) {
            this.inner = inner;
            this.continueProcessing = continueProcessing;
        }

        /**
         * Send termination signal to the provider of the iterator content.
         */
        @Override
        public void close() {
            continueProcessing.set(false);
        }

        @Override
        public boolean hasNext() {
            return inner.hasNext();
        }

        @Override
        public T next() {
            return inner.next();
        }

        @Override
        public void remove() {
            inner.remove();
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            inner.forEachRemaining(action);
        }
    }

    /**
     * Order based merge of {@code iterators}. The elements in the {@code iterators} must be in the same order as
     * ensured by the provided {@code comparator}. The merge uses a {@link PriorityQueue} for ordering the
     * {@code iterators} and have a total processing time of {@code O(n*log(s)} where {@code n} is the total
     * number of elements in all iterators combined and {@code s} is the number of iterators.
     * <p>
     * This merger wraps the provided {@code iterators} in {@link BufferingIterator} and
     * {@link SharedConstraintIterator} then uses {@link #mergeIterators(Collection, Comparator)}.
     * <p>
     * <strong>Important:</strong> to avoid thread leaking, the caller of this method MUST ensure that the
     * iterator is either depleted or that {@link CloseableIterator#close()} is called. The recommended way
     * is to use auto-closeable try wrapping:
     * <pre>
     * Executor executor = Executors.newCachedThreadPool();
     * Semaphore gatekeeper = new Semaphore(2);
     * CountingIterator<Integer> pic1 = new CountingIterator<>(Arrays.asList(1, 3, 5).iterator());
     * CountingIterator<Integer> pic2 = new CountingIterator<>(Arrays.asList(2, 4).iterator());
     * try (CollectionUtils.CloseableIterator<Integer> ci = CollectionUtils.mergeIteratorsBuffered(
     *      Arrays.asList(pic1, pic2), Integer::compareTo, executor, gatekeeper, 1)) {
     *   while(ci.hasNext()) {
     * System.out.println(ci.next());
     *     }
     * };     
     * </pre>
     * @param iterators  0 or more iterators where the elements are in {@code comparator} order.
     * @param comparator a comparator matching the order in the {@code iterators}.
     * @param executor   the Executor for starting background reading. This must be unbounded to avoid deadlocks.
     *                   It is recommended to create a persistent Executor and use it for all calls.
     * @param gatekeeper controls the amound of concurrent calls to the {@code iterators}. It is recommended to create
     *                   a persistent gateKeeper and use it for all calls of the same type, to avoid multiple
     *                   concurrent requests from users overwhelming the system.
     * @param bufferSize the maximum and ideal size of the buffers used for each iterator.
     * @return a stream delivering all elements in all provided {@code iterators} in {@code comparator} order.
     * @param <T> any class.
     * @see #mergeStreams(Collection, Comparator)
     */
    public static <T> CloseableIterator<T> mergeIteratorsBuffered(
            Collection<Iterator<T>> iterators, Comparator<T> comparator, Executor executor, Semaphore gatekeeper,
            int bufferSize) {
        AtomicBoolean continueProcessing = new AtomicBoolean(true);
        List<Iterator<T>> constrainedAndBuffered = iterators.stream()
                .map(i -> SharedConstraintIterator.of(i, gatekeeper))
                .map(i -> BufferingIterator.of(i, executor, bufferSize, continueProcessing))
                .collect(Collectors.toList());
        Iterator<T> merged = mergeIterators(constrainedAndBuffered, comparator);
        return CloseableIterator.of(merged, continueProcessing);
    }
}

