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

import java.io.Closeable;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Keeps track of objects that passes through the stream and logs performance statistics at given intervals.
 * <p>
 * Technically it works as a filter, but it always passes the test. Add to a stream with
 * {@code myStream.filter(new ThroughputTracker("ImageSearch:", "solrDocs", log, 100))}
 * which will log statistics about SolrDocuments processes every 100 documents, as part of an image search.
 * <p>
 * Note: There is no automatic "final" logging when the stream is depleted. Call {@link #performLog()} for that.
 */
public class ThroughputTracker implements Predicate<Object>, Closeable {
    private static final Logger defaultLog = LoggerFactory.getLogger("kb.dk.performance");

    private String prefix = "Throughput:";
    private String designation = "objects";
    private Logger log = defaultLog;
    private long logInterval = -1;
    private long logIntervalMS = 10*1000;

    private final long startTime = System.currentTimeMillis();

    private long callCounter = 0;

    private long lastLog = 0;
    private long nextLog = -1;

    private long lastLogMS = startTime;
    private long nextLogMS = startTime + logIntervalMS;

    /**
     * Create a tracker intended as a "side effect only" filter for a Stream.
     */
    public ThroughputTracker() {
    }

    /**
     * Create a tracker intended as a "side effect only" filter for a Stream.
     * Every {@code logInterval} objects a message is logged to the given {@code log} at {@code debug} level.
     * @param prefix      each log message starts with this.
     * @param designation the object that is logged, e.g. {@code images} or {@code solrDocs}.
     */
    public ThroughputTracker(String prefix, String designation) {
        this.prefix = prefix == null ? "" : prefix;
        this.designation = designation == null ? "objects" : designation;
    }

    @Override
    public synchronized boolean test(Object o) {
        if (++callCounter >= nextLog || System.currentTimeMillis() >= nextLogMS) {
            performLog();
        }
        return true;
    }

    /**
     * Trigger statistics logging.
     */
    public void performLog() {
        if (lastLog == callCounter) {
            return; // Already logged
        }

        final long now = System.currentTimeMillis();
        long totalDelta = callCounter;
        long totalDeltaMS = now-startTime;

        long lastDelta = callCounter-lastLog;
        long lastDeltaMS = now-lastLogMS;

        log.debug(String.format(
                // ImageSearch: 87 docs in 174 ms: 500.0 docs/s (last 50: 563.4 dosc/s)
                Locale.ROOT, "%s %d %s in %d ms: %.1f %s/s (last %d: %.1f %s/s)",
                prefix, totalDelta, designation, totalDeltaMS,
                totalDeltaMS == 0 ? 0 : totalDelta*1000.0/totalDeltaMS, designation,
                lastDelta, lastDeltaMS == 0 ? 0 : lastDelta * 1000.0 / lastDeltaMS, designation));

        lastLog = callCounter;
        nextLog = logInterval == -1 ? Long.MAX_VALUE : lastLog + logInterval;

        lastLogMS = now;
        nextLogMS = logIntervalMS == -1 ? Long.MAX_VALUE : now + logIntervalMS;
    }

    /**
     * The prefix is the first part of any log message.
     * Typically, this will contain a hint to what is being logged, e.g. {@code Export-WARC (query=...)}.
     * It is recommended to keep this reasonably short to avoid log spamming. At most 40 characters.
     * <p>
     * Default is the empty string.
     * @param prefix first part of logges messages.
     * @return the tracker adjusted with the given value.
     */
    public ThroughputTracker prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * The log used for logging throughput statistics.
     * <p>
     * Default is {@link #defaultLog}, initialized with the name {@code dk.kb.performance}.
     * @param log any valid logger.
     * @return the tracker adjusted with the given value.
     */
    public ThroughputTracker logger(Logger log) {
        this.log = log;
        return this;
    }

    /**
     * Default is {@code -1} (disabled).
     * @param logInterval logging is performed every {@code logInterval} calls til {@link #test}.
     *                    Disable by specifying {@code -1}.
     * @return the tracker adjusted with the given value.
     * @see #logIntervalMS(long)
     */
    public ThroughputTracker logInterval(long logInterval) {
        this.logInterval = logInterval;
        return this;
    }

    /**
     * The {@code logIntervalMS} is the recommended way to control how often logging is performed.
     * Logging is performed on calls to {@link #test} if the last logging were at least the stated
     * amount of milliseconds ago.
     * <p>
     * Specifying log interval in milliseconds makes the {@code ThroughputTracker} flexible with
     * regard to the concrete throughput.
     * <p>
     * Default is {@code 10000} (every 10 seconds).
     * @param logIntervalMS logging is performed at most every {@code logIntervalMS} millisecond upon
     *                      calls til {@link #test}. Disable by specifying {@code -1}.
     * @return the tracker adjusted with the given value.
     * @see #logIntervalMS(long)
     */
    public ThroughputTracker logIntervalMS(long logIntervalMS) {
        this.logIntervalMS = logIntervalMS;
        return this;
    }

    /**
     * Default is {@code objects}.
     * @param designation noun for the thing being logged, e.g. {@code calls} or {@code images}.
     * @return the tracker adjusted with the given value.
     */
    public ThroughputTracker designation(String designation) {
        this.designation = designation;
        return this;
    }

    @Override
    public void close() {
        performLog();
    }
}
