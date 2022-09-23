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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CollectionUtils {
    private static final Logger log = LoggerFactory.getLogger(CollectionUtils.class);

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

