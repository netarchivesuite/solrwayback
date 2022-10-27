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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CollectionUtils {

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

}

