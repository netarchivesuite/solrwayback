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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Helper methods for doing processing, i.e. threading of workloads.
 */
public class Processing {
    private static final Logger log = LoggerFactory.getLogger(Processing.class);
    // TODO: Make the number fo threads configurable
    public static final int THREADS = 20;

    // Shared between all callers, so this also acts as a limiter towards Solr/WARC-resolving
    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREADS, new ThreadFactory() {
        final AtomicInteger counter = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable runnable) {
            Thread t = new Thread(runnable, "processing_" +counter.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    });

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
        return CollectionUtils.splitToLists(jobs, batchSize). // Make the batches
                flatMap(Processing::processBatch). // Execute a batch
                map(Processing::safeGet); //
    }

    /**
     * Processes the full batch at once using the {@link #executorService}.
     * Callers are advised to keep batches at a manageable size.
     * <p>
     * The returned {@link Future}s are guaranteed to be {@link Future#isDone()}.
     * @param batch jobs.
     * @return the {@link Future}s with the results from the batch jobs, in the same order as the batch jobs.
     */
    private static <T> Stream<Future<T>> processBatch(List<Callable<T>> batch) {
        try {
            long startTime = System.currentTimeMillis();
            List<Future<T>> results = executorService.invokeAll(batch);
            //The following log is too spammy even for debug
            // log.debug("Batch processed {} jobs in {} ms", batch.size(), System.currentTimeMillis() - startTime);
            return results.stream();
        } catch (InterruptedException e) {
            throw new RuntimeException("Iterrupted while waiting for batch processing", e);
        }
    }

    /**
     * Wraps {@code future.get()} so that Exceptions are rethrown as {@link RuntimeException}s.
     */
    private static <T> T safeGet(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException("Exception while getting result from batch Future", e);
        }
    }

}
