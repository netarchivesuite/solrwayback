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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper methods for doing processing, i.e. threading of workloads.
 */
public class Processing {
    private static final Logger log = LoggerFactory.getLogger(Processing.class);
    // TODO: Make the number fo threads configurable
    public static final int THREADS = 20;

    // Shared between all callers, so this also acts as a limiter towards Solr/WARC-resolving
    private static ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

    /**
     * Threaded batch job execution.
     *
     * Packs the given jobs in batches of maximum {@link #THREADS} entries,
     * runs the batches using a threaded executor,
     * collects the results of the batch
     * passes the results as a Stream.
     *
     * Note that a shared {@link #executorService} is used with a maximum of {@link #THREADS} threads.
     * @param jobs the jobs to batch and execute in parallel.
     * @return the result of the jobs, in the same order as the jobs.
     */
    public static <T> Stream<T> batch(Stream<Callable<T>> jobs) {
        return batch(jobs, THREADS);
    }

    /**
     * Threaded batch job execution.
     *
     * Packs the given jobs in batches of maximum batchSize entries,
     * runs the batches using a threaded executor,
     * collects the results of the batch
     * passes the results as a Stream.
     *
     * Note that a shared {@link #executorService} is used with a maximum of {@link #THREADS} threads.
     * @param jobs the jobs to batch and execute in parallel.
     * @param batchSize if the batchSize exceeds {@link #THREADS}, the excess jobs will be queued by
     *                  {@link #executorService}. Having a large batchSize improves throughput at the cost of higher
     *                  latency, where latency in this case means the time before the returned Stream can deliver
     *                  job results. Conversely a low batchSize wil result in a poorer throughput and lower latency.
     * @return the result of the jobs, in the same order as the jobs.
     */
    public static <T> Stream<T> batch(Stream<Callable<T>> jobs, int batchSize) {
        return CollectionUtils.splitToLists(jobs, batchSize).
                flatMap(batch -> {
                    try {
                        return executorService.invokeAll(batch).stream();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Iterrupted while waiting for batch processing", e);
                    }
                }).
                map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        throw new RuntimeException("Exception while getting result from batch Future", e);
                    }
                });
    }

}
