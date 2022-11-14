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

import java.util.function.Predicate;

/**
 * Keeps track of objects that passes through the stream and logs performance statistics at given intervals.
 * <p>
 * Technically it works as a filter, but it always passes the test. Add to a stream with
 * {@code myStream.filter(new ThroughputTracker("ImageSearch:", "solrDocs", log, 100))}
 * which will log statistics about Solrdocuments processes every 100 documents, as part of an image search.
 * <p>
 * Note: There is no automatic "final" logging when the stream is depleted. Call {@link #performLog()} for that.
 */
public class ThroughputTracker implements Predicate<Object> {
    private final String prefix;
    private final String designation;
    private final Logger log;
    private final long logInterval;

    private long callCounter = 0;
    private long nextLog;
    private final long startTime = System.currentTimeMillis();

    /**
     * Create a tracker intended as a "side effect only" filter for a Stream.
     * Every {@code logInterval} objects a message is logged to the given {@code log} at {@code debug} level.
     * @param prefix      each log message starts with this.
     * @param designation the object that is lokked, e.g. {@code images} or {@code solrDocs}.
     * @param log         the Logger to use.
     * @param logInterval how often statistics is logged.
     */
    public ThroughputTracker(String prefix, String designation, Logger log, long logInterval) {
        this.prefix = prefix == null ? "" : prefix;
        this.designation = designation == null ? "objects" : designation;
        this.log = log;
        this.logInterval = logInterval;
        nextLog = logInterval;
    }

    @Override
    public synchronized boolean test(Object o) {
        if (++callCounter == nextLog) {
            performLog();
            nextLog += logInterval;
        }
        return true;
    }

    /**
     * Trigger statistics logging.
     */
    public void performLog() {
        long ms = System.currentTimeMillis()-startTime;
        log.debug("{} {} {} in {} ms: {} {}/s",
                  prefix, callCounter, designation, ms, callCounter*1000/ms, designation);
    }
}
