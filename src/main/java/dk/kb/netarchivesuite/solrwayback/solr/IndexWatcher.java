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
package dk.kb.netarchivesuite.solrwayback.solr;

import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Periodically checks if the Solr index has changed and fires a notification if that happens.
 * Requires the backing Solr index to provide the field {@code index_time} of type {@code date}.
 */
public class IndexWatcher {
    private static final Logger log = LoggerFactory.getLogger(IndexWatcher.class);

    /**
     * Status changes for Solr and its index.
     */
    public enum STATUS {
        /** The backing Solr has become unresponsive after a period of responsiveness */
        unavailable,
        /** The backing Solr has become responsive after a period of unresponsiveness */
        available,
        /** The index of the backing solr has changed since last check */
        changed,
        /** The backing Solr has not been queried */
        undetermined
    }

    public static final String TIME_FIELD = "index_time";

    private final SolrClient solrClient;
    private final long intervalMS;
    private final Consumer<STATUS> callback;
    private final Timer timer;

    private STATUS status = STATUS.undetermined;
    private String lastMaxIndexTime = null;

    /**
     *
     * @param solrClient standard SolrClient.
     * @param intervalMS how long to wait between check for index changes.
     * @param callback   called if the index changes status.
     */
    public IndexWatcher(SolrClient solrClient, long intervalMS, Consumer<STATUS> callback) {
        this.solrClient = solrClient;
        this.intervalMS = intervalMS;
        this.callback = callback;
        log.debug("Initializing IndexWatcher");

        if (intervalMS <= 1000) {
            log.warn("Watch interval is {} milliseconds. This is very low and might harm general Solr performance",
                     intervalMS);
        }

        timer = new Timer("IndexWatcher", true);
        timer.schedule(createWatchTask(), intervalMS, intervalMS);
        log.info("Created index watcher with interval {}ms", intervalMS);
    }

    /**
     * @return the number of milliseconds between each check.
     */
    public long getIntervalMS() {
        return intervalMS;
    }

    /**
     * @return current status for the Solr installation.
     */
    public STATUS getStatus() {
        return status;
    }

    /**
     * Close down the watcher. If a check is running during close and the index has changed, {@code callback} will
     * be triggered.
     */
    public void close() {
        log.info("Shutting down index watcher");
        timer.cancel();
    }

    /**
     * @return a task that periodically checks for Solr availability and index changes.
     */
    private TimerTask createWatchTask() {
        return new TimerTask() {
            @Override
            public void run() {
                try {
                    boolean hasChanged = hasIndexChanged();
                    setStatus(STATUS.available);
                    if (hasChanged) {
                        callback.accept(STATUS.changed);
                    }
                } catch (Exception e) {
                    setStatus(STATUS.unavailable);
                }
            }
        };
    }

    /**
     * Sets the status and notifies {@link #callback} if the previous status was not null and not the same as the
     * given status.
     * @param status new status. No notification if is is the same as the old status.
     */
    private void setStatus(STATUS status) {
        if (this.status == null) {
            this.status = status;
        } else if (this.status != status) {
            this.status = status;
            log.info("The Solr server changed status to {}", status);
            callback.accept(status);
        }
    }

    /**
     * Extracts latest {@code index_time} from the Solr index.
     */
    private void initWatch() {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        solrQuery.setRows(0);
        solrQuery.set("facet", "false");
        solrQuery.set("stats", "true");
        solrQuery.set("stats.field", "{!max=true}index_time");

        long queryTime = -System.currentTimeMillis();
        QueryResponse rsp;
        try {
            rsp = solrClient.query(solrQuery, SolrRequest.METHOD.POST);
            queryTime += System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("Unable to determine initial watch condition for Solr index", e);
            setStatus(STATUS.unavailable);
            return;
        }

        try {
            Date maxDate = (Date)rsp.getFieldStatsInfo().get(TIME_FIELD).getMax();
            lastMaxIndexTime = DateUtils.getSolrDateFull(maxDate);
            log.debug("Initial max {} was '{}', retrieved in {}ms", TIME_FIELD, lastMaxIndexTime, queryTime);
        } catch (Exception e) {
            log.warn("Got result for initial max {} with stats request but was unable to retrieve the value",
                     TIME_FIELD);
        }
    }

    /**
     * Performs a range query for documents newer than last recorded max time stamp.
     * The query is (hopefully) cached by Solr so checks against a non-changed index are fast.
     * When the index has changed, the request takes longer, scaling up with index size.
     * @return true if the index has changed since last call to the method.
     */
    private boolean hasIndexChanged() throws SolrServerException, IOException {
        // Handle the situation where Solr was down initially
        if (lastMaxIndexTime == null) {
            initWatch();
            if (lastMaxIndexTime == null) {
                return false;
            }
        }

        String changedQuery = String.format(Locale.ROOT, "%s:{%s TO *]", TIME_FIELD, lastMaxIndexTime);
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(changedQuery);
        solrQuery.setSort(TIME_FIELD, SolrQuery.ORDER.desc);
        solrQuery.setRows(1);
        solrQuery.setFields(TIME_FIELD);

        // Only check for changes means passing exceptions to the caller
        long queryTime = -System.currentTimeMillis();
        QueryResponse rsp = solrClient.query(solrQuery, SolrRequest.METHOD.POST);
        queryTime += System.currentTimeMillis();

        if (rsp.getResults().getNumFound() == 0) {
            return false;
        }
        try {
            Date maxDate = (Date)rsp.getResults().get(0).getFieldValue(TIME_FIELD);
            lastMaxIndexTime = DateUtils.getSolrDateFull(maxDate);
        } catch (Exception e) {
            log.warn("Got result for changes documents with query '{}' but was unable to retrieve new max index " +
                     "time from field {}", changedQuery, TIME_FIELD);
            return false;
        }
        log.debug("Received new max {} '{}' from query '{}' in {}ms",
                  TIME_FIELD, lastMaxIndexTime, changedQuery, queryTime);
        return true;
    }

}
