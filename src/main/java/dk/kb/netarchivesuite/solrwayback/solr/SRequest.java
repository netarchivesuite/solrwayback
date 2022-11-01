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

import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Encapsulation of a request to SolrGenericStreaming. Care has been taken to ensure sane defaults;
 * the only required attributes are {@link #query(String)} and {@link #fields(String...)}.
 * <p>
 * Use as a builder: {@code SRequest.builder().query("*:*").fields("url", "url_norm")}
 * <p>
 * Note: If {@link SRequest#solrQuery(SolrQuery)} is specified it will be used as a base for the full request.
 */
// TODO: Introduce addFilterqueries
public class SRequest {
    private static final Logger log = LoggerFactory.getLogger(SRequest.class);

    /**
     * Default maximum number of elements when the request requires unique results.
     * If this limit is exceeded during processing, an exception is thrown.
     * The uniquifier uses a HashSet (which is a Map underneath the hood) for tracking
     * unique values. Each entry takes up about ~150 bytes plus the value itself, so
     * something like 250 bytes/entry as a rule of thumb. The default MAX_UNIQUE is thus
     * about 1.25GB of maximum heap.
     */
    public static final int DEFAULT_MAX_UNIQUE = 5_000_000;

    /**
     * Solr ISO timestamp parsing. Supports optional milliseconds.
     * Sample inputs: {@code 2022-09-26T12:05:00Z}, {@code 2022-09-26T12:05:00.123Z}.
     */
    private static final Pattern ISO_TIME = Pattern.compile(
            "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T[012][0-9]:[0-5][0-9]:[0-5][0-9][.]?[0-9]?[0-9]?[0-9]?Z");

    public SolrClient solrClient = SolrGenericStreaming.defaultSolrClient;
    public SolrQuery solrQuery = new SolrQuery();
    public boolean expandResources = false;
    public List<String> expandResourcesFilterQueries;
    public boolean ensureUnique = false;
    public Integer maxUnique = DEFAULT_MAX_UNIQUE;
    private String idealTime; // If defined, a sort will be created as String.format(Locale.ROOT, "%s asc, abs(sub(ms(%s), crawl_date)) asc", deduplicateField, idealTime);
    public String deduplicateField = null;
    List<String> fields;
    public long maxResults = Long.MAX_VALUE;
    /**
     * Default sort used when exporting. Ends with tie breaking on id.
     */
    public static final String DEFAULT_SORT = "score desc, id asc";
    public String sort = DEFAULT_SORT;
    public String query = null;
    public Stream<String> queries = null;
    public List<String> filterQueries;
    public int pageSize = SolrGenericStreaming.DEFAULT_PAGESIZE;
    public int queryBatchSize = SolrGenericStreaming.DEFAULT_QUERY_BATCHSIZE;

    /**
     * @return a fresh instance of SRequest intended for further adjustment.
     */
    public static SRequest builder() {
        return new SRequest();
    }

    /**
     * Creates a request with query and fields.
     * As the returned request is initialized with query and fields, it can be used without further adjustments.
     *
     * @return an instance of SRequest, initialized with the provided query and fields.
     */
    public static SRequest create(String query, String... fields) {
        return new SRequest().query(query).fields(fields);
    }

    /**
     * Creates a request with query and fields.
     * As the returned request is initialized with query and fields, it can be used without further adjustments.
     *
     * @return an instance of SRequest, initialized with the provided query and fields.
     */
    public static SRequest create(String query, List<String> fields) {
        return new SRequest().query(query).fields(fields);
    }

    /**
     * @param solrClient used for issuing Solr requests.
     *                   If not specified, {@link SolrGenericStreaming#defaultSolrClient} will be used.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest solrClient(SolrClient solrClient) {
        if (solrClient == null) {
            log.debug("solrClient(null) called. Leaving solrClient unchanged");
            return this;
        }
        this.solrClient = solrClient;
        return this;
    }

    /**
     * The parameters in the solrQuery has the lowest priority: All calls to modifier methods will override matching
     * values in the solrQuery.
     *
     * @param solrQuery the base for the request. If not provided it will be constructed from scratch.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest solrQuery(SolrQuery solrQuery) {
        this.solrQuery = solrQuery;
        return this;
    }

    /**
     * @param expandResources if true, embedded resources for HTML pages are extracted and added to the delivered
     *                        lists of Solr Documents. Default is false.
     *                        Note: Indirect references (through JavaScript & CSS) are not followed.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest expandResources(Boolean expandResources) {
        this.expandResources = Boolean.TRUE.equals(expandResources);
        return this;
    }

    /**
     * @param ensureUnique if true, unique documents are guaranteed. This is only sane if expandResources is true.
     *                     Default is false.
     *                     Note that a HashSet is created to keep track of encountered documents and will impose
     *                     a memory overhead linear to the number of results.
     * @return the SRequest adjusted with the provided value.
     * @see #maxUnique(Integer)
     */
    public SRequest ensureUnique(Boolean ensureUnique) {
        this.ensureUnique = Boolean.TRUE.equals(ensureUnique);
        return this;
    }

    /**
     * @param maxUnique the maximum number of uniques to track when ensureUnique is true.
     *                  If the number of uniques exceeds this limit, an exception will be thrown.
     *                  Default is {@link #DEFAULT_MAX_UNIQUE}.
     * @return the SRequest adjusted with the provided value.
     * @see #ensureUnique(Boolean)
     */
    public SRequest maxUnique(Integer maxUnique) {
        this.maxUnique = maxUnique;
        return this;
    }

    /**
     * Note: This overrides any existing {@link #sort(String)}.
     * @param deduplicateField The field to use for de-duplication. This is typically {@code url}.
     *                         Default is null (no deduplication).
     *                         Note: deduplicateField does not affect expandResources. Set ensureUnique to true if
     *                         if expandResources is true and uniqueness must also be guaranteed for resources.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest deduplicateField(String deduplicateField) {
        if (deduplicateField != null && deduplicateField.isEmpty()) {
            throw new IllegalArgumentException("deduplicateField cannot be the empty String");
        }
        this.deduplicateField = deduplicateField;
        return this;
    }

    /**
     * Deduplication combined with time proximity sorting. This is a shorthand for
     * {@code request.deduplicatefield(deduplicateField).sort("abs(sub(ms(idealTime), crawl_date)) asc");}
     * <p>
     * Use case: Extract unique URLs for matches that are closest to a given point in time:
     * {@code timeProximityDeduplication("2014-01-03T11:56:58Z", "url_norm"}.
     * <p>
     * Note: This overrides any existing {@link #sort(String)}.
     *
     * @param idealTime        The time that the resources should be closest to, stated as a Solr timestamp
     *                         {@code YYYY-MM-DDTHH:mm:SSZ}.
     *                         Also supports {@code oldest} and {@code newest} as values.
     * @param deduplicateField The field to use for de-duplication. This is typically {@code url}.
     * @return the SRequest adjusted with the provided values.
     */
    public SRequest timeProximityDeduplication(String idealTime, String deduplicateField) {
        if (deduplicateField != null && deduplicateField.isEmpty()) {
            throw new IllegalArgumentException("deduplicateField cannot be the empty String");
        }
        String origo = idealTime;
        if ("newest".equals(idealTime)) {
            origo = "9999-12-31T23:59:59Z";
        } else if ("oldest".equals(idealTime)) {
            origo = "0001-01-01T00:00:01Z";
        } else if (!ISO_TIME.matcher(idealTime).matches()) {
            throw new IllegalArgumentException(
                    "The idealTime '" + idealTime + "' does not match 'oldest', 'newest', 'YYYY-MM-DDTHH:mm:SSZ' or " +
                    "'YYYY-MM-DDTHH:mm:SS.sssZ");
        }
        this.idealTime = origo;

        if (deduplicateField == null) {
            throw new NullPointerException("deduplicateField == null which is not allowed for timeProximityDeduplication");
        }
        this.deduplicateField = deduplicateField;
        return this;
    }

    /**
     * @param fields fields to export (fl). deduplicateField will be added to this is not already present.
     *               This parameter has no default and must be defined.
     * @return the SRequest adjusted with the provided value.
     * @see #fields(String...)
     */
    public SRequest fields(List<String> fields) {
        this.fields = fields;
        return this;
    }

    /**
     * @param fields fields to export (fl). deduplicateField will be added to this is not already present.
     *               This parameter has no default and must be defined.
     *               If only 1 field is specified and fields contains at least 1 comma, it will treated
     *               as a comma separated list of fields: {@code "foo,bar,zoo" == "foo", "bar", "zoo"}.
     * @return the SRequest adjusted with the provided value.
     * @see #fields(List)
     */
    public SRequest fields(String... fields) {
        if (fields.length == 1 && fields[0].contains(",")) {
            this.fields = Arrays.asList(fields[0].split(", *"));
        } else {
            this.fields = Arrays.asList(fields);
        }
        return this;
    }

    /**
     * @param maxResults the maximum number of results to return. This includes expanded resources.
     *                   Default is {@link Long#MAX_VALUE} (effectively no limit).
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest maxResults(long maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    /**
     * Note: {@link #timeProximityDeduplication(String, String)} and {@link #deduplicateField(String)} takes
     * precedence over sort.
     *
     * @param sort standard Solr sort. Depending on deduplicateField and tie breaker it might be adjusted
     *             by {@link SolrGenericStreaming#adjustSolrQuery(SolrQuery, boolean, boolean, String)}.
     *             Default is {@link #DEFAULT_SORT}.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest sort(String sort) {
        this.sort = sort;
        return this;
    }

    /**
     * @param query standard Solr query.
     *              This parameter has no default and must be defined either directly or
     *              through {@link #solrQuery(SolrQuery)}.
     * @return the SRequest adjusted with the provided value.
     * @throws IllegalStateException if {@link #queries(Stream)} called before this.
     */
    public SRequest query(String query) {
        if (queries != null) {
            throw new IllegalStateException("queries(Stream<String>) has already been called");
        }
        this.query = query;
        return this;
    }

    /**
     * @param queries standard Solr queries, where all queries will conceptually be issued one after another.
     *                For performance reasons, queries will be batched with {@code OR} as modifier:
     *                If {@code queries = Arrays.asList("url:http://example.com/foo", "url:http://example.com/bar").stream()},
     *                the batch request will be {@code "url:http://example.com/foo OR url:http://example.com/bar"}.
     *                However, there is an upper limit to batching so multiple batches might be issued.
     *                This affects {@link #deduplicateField(String)} and {@link #timeProximityDeduplication(String, String)}
     *                as overall deduplication will not be guaranteed.
     *                Use {@link #ensureUnique(Boolean)} to force unique results, if needed.
     * @return the SRequest adjusted with the provided value.
     * @throws IllegalStateException if {@link #query(String)} called before this.
     */
    public SRequest queries(Stream<String> queries) {
        if (query != null) {
            throw new IllegalStateException("query(String) has already been called");
        }
        this.queries = queries;
        return this;
    }

    /**
     * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      Note: This overrides any existing filterQueries.
     * @return the SRequest adjusted with the provided value.
     * @see #filterQueries(String...)
     */
    public SRequest filterQueries(List<String> filterQueries) {
        this.filterQueries = filterQueries;
        return this;
    }

    /**
     * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      Note: This overrides any existing filterQueries.
     * @return the SRequest adjusted with the provided value.
     * @see #filterQueries(List)
     */
    public SRequest filterQueries(String... filterQueries) {
        this.filterQueries = Arrays.asList(filterQueries);
        return this;
    }

    /**
     * @param filterQueries optional Solr filter queries used when {@link #expandResources(Boolean)} is true.
     *                      Only resources that satisfies these filters are exported.
     *                      For performance, 0 or 1 filter query is recommended.
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      Note: This overrides any existing expandResourcesFilterQueries.
     * @return the SRequest adjusted with the provided value.
     * @see #filterQueries(String...)
     */
    public SRequest expandResourcesFilterQueries(List<String> filterQueries) {
        this.expandResourcesFilterQueries = filterQueries;
        return this;
    }

    /**
     * @param filterQueries optional Solr filter queries used when {@link #expandResources(Boolean)} is true.
     *                      Only resources that satisfies these filters are exported.
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      Note: This overrides any existing expandResourcesFilterQueries.
     * @return the SRequest adjusted with the provided value.
     * @see #filterQueries(List)
     */
    public SRequest expandResourcesFilterQueries(String... filterQueries) {
        this.expandResourcesFilterQueries = Arrays.asList(filterQueries);
        return this;
    }

    /**
     * @param pageSize paging size. Typically 500-100,000 depending on fields.
     *                 Default is {@link SolrGenericStreaming#DEFAULT_PAGESIZE}.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Newer Solrs (at least 9+) share a default upper limit of 1024 boolean clauses recursively in the user issued
     * query tree. As multi-query uses batching, this limit can quickly be reached. Keep well below 1024.
     * @param queryBatchSize batch size when using {@link #queries(Stream)}.
     *                       Default is {@link SolrGenericStreaming#DEFAULT_QUERY_BATCHSIZE}.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest queryBatchSize(int queryBatchSize) {
        this.queryBatchSize = queryBatchSize;
        return this;
    }

    /**
     * Copies {@link #solrQuery} and adjusts it with defined attributes from the SRequest, extending with needed
     * SolrRequest-attributes.
     *
     * Note: This does assign {@link #query}, but not {@link #queries}: They muct be handled explicitly.
     * @return a SolrQuery ready for processing.
     */
    public SolrQuery getMergedSolrQuery() {
        SolrQuery solrQuery = SolrUtils.deepCopy(this.solrQuery);
        if (query != null) {
            solrQuery.setQuery(query);
        }
        if (filterQueries != null) {
            solrQuery.setFilterQueries(filterQueries.toArray(new String[0]));
        }

        if (idealTime != null) {
            sort = String.format(Locale.ROOT, "%s asc, abs(sub(ms(%s), crawl_date)) asc", deduplicateField, idealTime);
        } else if (deduplicateField != null) {
            sort = String.format(Locale.ROOT, "%s asc", deduplicateField);
        }
        solrQuery.set(CommonParams.SORT, sort);
        if (fields != null) {
            solrQuery.set(CommonParams.FL, String.join(",", fields));
        }
        solrQuery.set(CommonParams.ROWS, (int) Math.min(maxResults, pageSize));
        return solrQuery;
    }

    /**
     * @return expandResourcesFilterQueries as an array. Empty if no filters has been assigned.
     */
    public String[] getExpandResourcesFilterQueries() {
        return expandResourcesFilterQueries == null ?
                new String[0] :
                expandResourcesFilterQueries.toArray(new String[0]);
    }

    /**
     * @return true if there are multiple queries, i.e. {@link #query(String)} has been called.
     */
    public boolean isMultiQuery() {
        return queries != null;
    }

    /**
     * Note: Due to the nature of Streams, {@link #queries} is not deep copied.
     * @return a copy of this SRequest, as independent as possible: {@link #solrQuery} and Lists are deep-copied.
     */
    public SRequest deepCopy() {
        SRequest copy = new SRequest().
                solrClient(solrClient).
                solrQuery(solrQuery == null ? null : SolrUtils.deepCopy(solrQuery)).
                expandResources(expandResources).
                ensureUnique(ensureUnique).
                maxUnique(maxUnique).
                deduplicateField(deduplicateField).
                fields(copy(fields)).
                maxResults(maxResults).
                sort(sort).
                query(query).
                queries(queries).
                filterQueries(copy(filterQueries)).
                expandResourcesFilterQueries(copy(expandResourcesFilterQueries)).
                pageSize(pageSize);
        copy.idealTime = idealTime;
        return copy;
    }

    private List<String> copy(List<String> fields) {
        if (fields == null) {
            return null;
        }
        return new ArrayList<>(fields);
    }

    /**
     * Stream the Solr responses one document at a time.
     * Shorthand for {@code SolrGenericStreaming.create(srequest).stream()}.
     * Use when the status methods from {@link SolrGenericStreaming} are not needed.
     * @return a stream of SolrDocuments.
     */
    public Stream<SolrDocument> stream() {
        return SolrGenericStreaming.create(this).stream();
    }
}
