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

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.util.CollectionUtils;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.StatsParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

    public enum CHOICE {always, never, auto}

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

    public SolrClient solrClient = NetarchiveSolrClient.noCacheSolrServer;
    public SolrQuery solrQuery = new SolrQuery();
    public boolean expandResources = false;
    public List<String> expandResourcesFilterQueries;

    public boolean ensureUnique = false;
    public Integer maxUnique = DEFAULT_MAX_UNIQUE;
    public List<String> uniqueFields = Collections.singletonList("id");
    public boolean useHashingForUnique = true;

    private String idealTime; // If defined, a sort will be created as String.format(Locale.ROOT, "%s asc, abs(sub(ms(%s), crawl_date)) asc", deduplicateField, idealTime);
    // TODO: Expand this to support multiple deduplicateFields
    public List<String> deduplicateFields;
    public List<String> fields;
    public long maxResults = Long.MAX_VALUE;
    /**
     * Default sort used when exporting. Ends with tie breaking on id.
     */
    public static final String DEFAULT_SORT = "score desc, id asc";
    /**
     * The sort param as given by the caller. It is recommended to call {@link #getFullSort()} instead of using this
     * value directly as {@code getFullSort()} extends the sort with relevant clauses depending on other attributes
     * in the current {@code SRequest}.
     */
    public String sort = DEFAULT_SORT;
    public String query = null;
    public Stream<String> queries = null;
    public List<String> filterQueries;
    public int pageSize = SolrStreamDirect.DEFAULT_PAGESIZE;
    public boolean usePaging = true;
    public int queryBatchSize = SolrStreamDirect.DEFAULT_QUERY_BATCHSIZE;
    public List<String> shards = null;
    public String collection = null;
    public CHOICE shardDivide = CHOICE.valueOf(PropertiesLoader.SOLR_STREAM_SHARD_DIVIDE);
    /**
     * If {@link #shardDivide} is {@link CHOICE#auto}, a hit-counting search is performed before the
     * full request is initiated. If the number of hits is above {@link #shardDivideAutoMinHits}, shard
     * division is activated.
     *
     * The default value is specified in properties, falling back to 5000.
     */
    public long shardDivideAutoMinHits = PropertiesLoader.SOLR_STREAM_SHARD_AUTO_MIN_HITS;

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
     *                   If not specified, {@link NetarchiveSolrClient#noCacheSolrServer} will be used.
     * @return the SRequest adjusted with the provided value.
     * @see #useCachingClient(boolean)
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
     * Whether or not a caching {@link SolrClient} is used. The client is shared with {@link NetarchiveSolrClient}.
     * <p>
     * The default is false (no caching). Set to true when used for limited result sets.
     * @param useCaching if true, a locally caching Solrclient is used.
     * @return the SRequest adjusted with the provided value.
     * @see #solrClient(SolrClient)
     */
    public SRequest useCachingClient(boolean useCaching) {
        solrClient = useCaching ?
                NetarchiveSolrClient.solrServer :
                NetarchiveSolrClient.noCacheSolrServer;
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
     * @see #ensureUnique(Boolean)
     * @see #uniqueHashing(boolean)
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
     * @see #uniqueFields(String...)
     * @see #maxUnique(Integer)
     */
    public SRequest maxUnique(Integer maxUnique) {
        this.maxUnique = maxUnique;
        return this;
    }

    /**
     * Used for determining uniqueness when {@link #ensureUnique(Boolean)} is true.
     * <p>
     * Setting this value automatically sets {@link #ensureUnique(Boolean)} to true.
     * @param fields the fields to use for uniqueness. Default is {@code id}.
     * @return the SRequest adjusted with the provided value.
     * @see #ensureUnique(Boolean)
     * @see #uniqueHashing(boolean)
     * @see #maxUnique(Integer)
     */
    public SRequest uniqueFields(String... fields) {
        if (fields.length == 0) {
            throw new IllegalArgumentException("No fields provided");
        }
        uniqueFields = Arrays.asList(fields);
        ensureUnique = true;
        return this;
    }

    /**
     * Using the hash of {@link #uniqueFields(String...)} instead of the field content for tracking uniqueness has
     * far lower memory impact (factor 10+), but with the possibility of hash collisions.
     * @param useHashing if true, hashing is used for determining uniqueness. Default is false.
     * @return the SRequest adjusted with the provided value.
     * @see #ensureUnique(Boolean)
     * @see #uniqueFields(String...)
     * @see #maxUnique(Integer)
     */
    public SRequest uniqueHashing(boolean useHashing) {
        useHashingForUnique = useHashing;
        return this;
    }

    /**
     * Deduplicating on one or more fields: Only the first document for any given combination of the fields is
     * delivered. Note that this modifies the sort order by prefixing the sort chain with the deduplicate fields.
     * @param deduplicateFields The fields to use for de-duplication. This is typically {@code url}.
     *                          Default is null (no deduplication).
     *                          Note: deduplicateField does not affect expandResources. Set ensureUnique to true if
     *                          expandResources is true and uniqueness must also be guaranteed for resources.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest deduplicateFields(String... deduplicateFields) {
        if (deduplicateFields != null && deduplicateFields.length == 1 && deduplicateFields[0] == null) {
            this.deduplicateFields = null;
            return this;
        }

        if (deduplicateFields != null && Arrays.stream(deduplicateFields).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Got null as deduplicateField in array " + Arrays.toString(deduplicateFields));
        }
        this.deduplicateFields = deduplicateFields == null || deduplicateFields.length == 0 ?
                null :
                Arrays.asList(deduplicateFields);
        return this;
    }

    /**
     * Deduplicating on one or more fields: Only the first document for any given combination of the fields is
     * delivered. Note that this modifies the sort order by prefixing the sort chain with the deduplicate fields.
     * @param deduplicateFields The fields to use for de-duplication. This is typically {@code url}.
     *                          Default is null (no deduplication).
     *                          Note: deduplicateField does not affect expandResources. Set ensureUnique to true if
     *                          expandResources is true and uniqueness must also be guaranteed for resources.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest deduplicateFields(List<String> deduplicateFields) {
        if (deduplicateFields != null && deduplicateFields.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Got null as deduplicateField in list " + deduplicateFields);
        }
        this.deduplicateFields = deduplicateFields == null || deduplicateFields.isEmpty() ?
                null :
                deduplicateFields;
        return this;
    }

    /**
     * Time proximity sorting. This is a shorthand for {@code sort("abs(sub(ms(idealTime), crawl_date)) asc");}
     * <p>
     * Use case: Extract documents for matches that are closest to a given point in time:
     * {@code timeProximity("2014-01-03T11:56:58Z"}.
     * @param idealTime         The time that the resources should be closest to, stated as a Solr timestamp
     *                          {@code YYYY-MM-DDTHH:mm:SSZ}.
     *                          <p>
     *                          Also supports {@code oldest} and {@code newest} as values.
     * @return the SRequest adjusted with the provided values.
     */
    public SRequest timeProximity(String idealTime) {
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

        return this;
    }

    /**
     * Deduplication combined with time proximity sorting. This is a shorthand for
     * {@code request.deduplicateFields(deduplicateFields).sort("abs(sub(ms(idealTime), crawl_date)) asc");}
     * <p>
     * Use case: Extract unique URLs for matches that are closest to a given point in time:
     * {@code timeProximityDeduplication("2014-01-03T11:56:58Z", "url_norm"}.
     * <p>
     * Note: The {@link #sort(String)} is prefixed with {@code deduplicateFields}.
     *
     * @param idealTime         The time that the resources should be closest to, stated as a Solr timestamp
     *                          {@code YYYY-MM-DDTHH:mm:SSZ}.
     *                          <p>
     *                          Also supports {@code oldest} and {@code newest} as values.
     * @param deduplicateFields Zero or more field to deduplicate on. This is typically {@code url_norm}.
     * @return the SRequest adjusted with the provided values.
     */
    public SRequest timeProximityDeduplication(String idealTime, String... deduplicateFields) {
        timeProximity(idealTime);
        deduplicateFields(deduplicateFields);
        return this;
    }

    /**
     * @param fields fields to export (fl). deduplicateField will be added to this is not already present.
     *               This parameter has no default and must be defined.
     * @return the SRequest adjusted with the provided value.
     * @throws IllegalStateException if fields has already been assigned.
     * @see #fields(String...)
     * @see #forceFields(List)
     */
    public SRequest fields(List<String> fields) {
        if (this.fields != null) {
            throw new IllegalStateException("fields already assigned. If overriding is intentional, use forceFields");
        }
        return forceFields(fields);
    }

    /**
     * This method overrides existing fields without warning or error.
     * @param fields fields to export (fl). deduplicateField will be added to this is not already present.
     *               This parameter has no default and must be defined.
     * @return the SRequest adjusted with the provided value.
     * @see #fields(String...)
     * @see #fields(List)
     */
    public SRequest forceFields(List<String> fields) {
        this.fields = fields;
        return this;
    }

    /**
     * @param fields fields to export (fl). deduplicateField will be added to this is not already present.
     *               This parameter has no default and must be defined.
     *               If only 1 field is specified and fields contains at least 1 comma, it will treated
     *               as a comma separated list of fields: {@code "foo,bar,zoo" == "foo", "bar", "zoo"}.
     * @return the SRequest adjusted with the provided value.
     * @throws IllegalStateException if fields has already been assigned.
     * @see #fields(List)
     * @see #forceFields(String...)
     */
    public SRequest fields(String... fields) {
        if (this.fields != null) {
            throw new IllegalStateException("fields already assigned. If overriding is intentional, use forceFields");
        }
        return forceFields(fields);
    }

    /**
     * This method overrides existing fields without warning or error.
     * @param fields fields to export (fl). deduplicateField will be added to this is not already present.
     *               This parameter has no default and must be defined.
     *               <p>
     *               If only 1 field is specified and fields contains at least 1 comma, it will treated
     *               as a comma separated list of fields: {@code "foo,bar,zoo" == "foo", "bar", "zoo"}.
     * @return the SRequest adjusted with the provided value.
     * @see #fields(List)
     * @see #fields(String...)
     */
    public SRequest forceFields(String... fields) {
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
     * Note: {@link #timeProximityDeduplication(String, String...)} and {@link #deduplicateFields(String...)} takes
     * precedence over sort by prefixing with {@link #deduplicateFields}.
     *
     * @param sort standard Solr sort. Depending on deduplicateFields and tie breaker it might be adjusted.
     *             Default is {@link #DEFAULT_SORT}.
     * @return the SRequest adjusted with the provided value.
     * @throws IllegalStateException if sort has already been assigned.
     * @see #forceSort(String)
     * @see #deduplicateFields(String...)
     */
    public SRequest sort(String sort) {
        if (!DEFAULT_SORT.equals(this.sort)) {
            throw new IllegalStateException("sort already assigned. If overriding is intentional, use forceSort");
        }
        return forceSort(sort);
    }

    /**
     * Note: {@link #timeProximityDeduplication(String, String...)} and {@link #deduplicateFields(String...)} takes
     * precedence over sort by prefixing with {@link #deduplicateFields}.
     *
     * This method overrides existing sort without warning or error.
     * @param sort standard Solr sort. Depending on deduplicateField and tie breaker it might be adjusted.
     *             Default is {@link #DEFAULT_SORT}.
     * @return the SRequest adjusted with the provided value.
     * @see #sort(String)
     */
    public SRequest forceSort(String sort) {
        this.sort = sort;
        return this;
    }

    /**
     * @param query standard Solr query.
     *              This parameter has no default and must be defined either directly or
     *              through {@link #solrQuery(SolrQuery)}.
     * @return the SRequest adjusted with the provided value.
     * @throws IllegalStateException if {@link #queries(Stream)} was called before this or the query was already set.
     */
    public SRequest query(String query) {
        if (this.query != null) {
            throw new IllegalStateException("query already assigned. If overriding is intentional, use forceQuery");
        }
        if (queries != null) {
            throw new IllegalStateException("queries(Stream<String>) has already been called");
        }
        return forceQuery(query);
    }

    /**
     * This method overrides existing query and removes existing queries without warning or error.
     * @param query standard Solr query.
     *              <p>
     *              This parameter has no default and must be defined either directly or
     *              through {@link #solrQuery(SolrQuery)}.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest forceQuery(String query) {
        this.query = query;
        this.queries = null;
        return this;
    }

    /**
     * @param queries standard Solr queries, where all queries will conceptually be issued one after another.
     *                For performance reasons, queries will be batched with {@code OR} as modifier:
     *                If {@code queries = Arrays.asList("url:http://example.com/foo", "url:http://example.com/bar").stream()},
     *                the batch request will be {@code "url:http://example.com/foo OR url:http://example.com/bar"}.
     *                However, there is an upper limit to batching so multiple batches might be issued.
     *                This affects {@link #deduplicateFields(String...)} and
     *                {@link #timeProximityDeduplication(String, String...)}
     *                as overall deduplication will not be guaranteed.
     *                Use {@link #ensureUnique(Boolean)} to force unique results, if needed.
     * @return the SRequest adjusted with the provided value.
     * @see #forceQueries(Stream)
     * @throws IllegalStateException if {@link #query(String)} called before this or queries were already set.
     */
    public SRequest queries(Stream<String> queries) {
        if (this.queries == null && queries == null) {
            return this;
        }
        if (this.queries != null) {
            throw new IllegalStateException(
                    "queries(Stream<String>) has already been called. If overriding is intentional, use forceQueries");
        }
        if (query != null) {
            throw new IllegalStateException("query(String) has already been called");
        }
        return forceQueries(queries);
    }

    /**
     * This method overrides existing queries and removes existing query without warning or error.
     * @param queries standard Solr queries, where all queries will conceptually be issued one after another.
     *                <p>
     *                For performance reasons, queries will be batched with {@code OR} as modifier:
     *                If {@code queries = Arrays.asList("url:http://example.com/foo", "url:http://example.com/bar").stream()},
     *                the batch request will be {@code "url:http://example.com/foo OR url:http://example.com/bar"}.
     *                <p>
     *                However, there is an upper limit to batching so multiple batches might be issued.
     *                This affects {@link #deduplicateFields(String...)} and
     *                {@link #timeProximityDeduplication(String, String...)}
     *                as overall deduplication will not be guaranteed.
     *                <p>
     *                Use {@link #ensureUnique(Boolean)} to force unique results, if needed.
     * @return the SRequest adjusted with the provided value.
     * @see #queries(Stream)
     */
    public SRequest forceQueries(Stream<String> queries) {
        this.queries = queries;
        this.query = null;
        return this;
    }

    /**
     * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      Note: This overrides any existing filterQueries.
     * @return the SRequest adjusted with the provided value.
     * @throws IllegalStateException if filterQueries were already set.
     * @see #filterQueries(String...)
     * @see #forceFilterQueries(List)
     */
    public SRequest filterQueries(List<String> filterQueries) {
        if (this.filterQueries != null) {
            throw new IllegalStateException(
                    "filterQueries(...) has already been called. If overriding is intentional, use forceFilterQueries");
        }
        return forceFilterQueries(filterQueries);
    }

    /**
     * This method overrides existing filterQueries without warning or error.
     * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
     *                      <p>
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      <p>
     *                      Note: This overrides any existing filterQueries.
     * @return the SRequest adjusted with the provided value.
     * @see #filterQueries(String...)
     * @see #filterQueries(List)
     */
    public SRequest forceFilterQueries(List<String> filterQueries) {
        this.filterQueries = filterQueries;
        return this;
    }

    /**
     * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
     *                      <p>
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      <p>
     *                      Note: This overrides any existing filterQueries.
     * @return the SRequest adjusted with the provided value.
     * @throws IllegalStateException if filterQueries were already set.
     * @see #filterQueries(List)
     * @see #forceFilterQueries(String...)
     */
    public SRequest filterQueries(String... filterQueries) {
        if (this.filterQueries != null) {
            throw new IllegalStateException(
                    "filterQueries(...) has already been called. If overriding is intentional, use forceFilterQueries");
        }
        return forceFilterQueries(filterQueries);
    }

    /**
     * This method overrides existing filterQueries without warning or error.
     * @param filterQueries optional Solr filter queries. For performance, 0 or 1 filter query is recommended.
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      <p>
     *                      Note: This overrides any existing filterQueries.
     * @return the SRequest adjusted with the provided value.
     * @see #filterQueries(String...)
     */
    public SRequest forceFilterQueries(String... filterQueries) {
        this.filterQueries = Arrays.asList(filterQueries);
        return this;
    }

    /**
     * @param filterQueries optional Solr filter queries used when {@link #expandResources(Boolean)} is true.
     *                      Only resources that satisfies these filters are exported.
     *                      <p>
     *                      For performance, 0 or 1 filter query is recommended.
     *                      <p>
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      <p>
     *                      Note: This overrides any existing expandResourcesFilterQueries.
     * @return the SRequest adjusted with the provided value.
     * @throws IllegalStateException if filterQueries were already set.
     * @see #expandResourcesFilterQueries(String...)
     * @see #forceExpandResourcesFilterQueries(List)
     */
    public SRequest expandResourcesFilterQueries(List<String> filterQueries) {
        if (this.expandResourcesFilterQueries != null) {
            throw new IllegalStateException(
                    "expandResourceFilterQueries(...) has already been called. If overriding is intentional, " +
                    "use forceExpandResourcesFilterQueries");
        }
        return forceExpandResourcesFilterQueries(filterQueries);
    }

    /**
     * This method overrides existing filterQueries without warning or error.
     * @param filterQueries optional Solr filter queries used when {@link #expandResources(Boolean)} is true.
     *                      <p>
     *                      Only resources that satisfies these filters are exported.
     *                      <p>
     *                      For performance, 0 or 1 filter query is recommended.
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      <p>
     *                      Note: This overrides any existing expandResourcesFilterQueries.
     * @return the SRequest adjusted with the provided value.
     * @see #expandResourcesFilterQueries(List)
     */
    public SRequest forceExpandResourcesFilterQueries(List<String> filterQueries) {
        this.expandResourcesFilterQueries = filterQueries;
        return this;
    }

    /**
     * @param filterQueries optional Solr filter queries used when {@link #expandResources(Boolean)} is true.
     *                      Only resources that satisfies these filters are exported.
     *                      <p>
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      <p>
     *                      Note: This overrides any existing expandResourcesFilterQueries.
     * @return the SRequest adjusted with the provided value.
     * @throws IllegalStateException if filterQueries were already set.
     * @see #expandResourcesFilterQueries(List)
     * @see #forceExpandResourcesFilterQueries(String...)
     */
    public SRequest expandResourcesFilterQueries(String... filterQueries) {
        if (this.expandResourcesFilterQueries != null) {
            throw new IllegalStateException(
                    "expandResourceFilterQueries(...) has already been called. If overriding is intentional, " +
                    "use forceExpandResourcesFilterQueries");
        }
        return forceExpandResourcesFilterQueries(filterQueries);
    }

    /**
     * This method overrides existing filterQueries without warning or error.
     * @param filterQueries optional Solr filter queries used when {@link #expandResources(Boolean)} is true.
     *                      Only resources that satisfies these filters are exported.
     *                      <p>
     *                      If multiple filters are to be used, consider collapsing them into one:
     *                      {@code ["foo", "bar"]} → {@code ["(foo) AND (bar)"]}.
     *                      <p>
     *                      Note: This overrides any existing expandResourcesFilterQueries.
     * @return the SRequest adjusted with the provided value.
     * @see #expandResourcesFilterQueries(String...)
     */
    public SRequest forceExpandResourcesFilterQueries(String... filterQueries) {
        this.expandResourcesFilterQueries = Arrays.asList(filterQueries);
        return this;
    }

    /**
     * @param pageSize paging size. Typically 500-100,000 depending on fields.
     *                 Default is {@link SolrStreamDirect#DEFAULT_PAGESIZE}.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Disables paging through result sets, so that only {@link #pageSize(int)} results are processed for each query.
     * <p>
     * Use case: Getting a limited number of results from each query when using {@link #queries}.
     *           For that scenario, remember setting {@link #queryBatchSize(int)} to 1.
     * <p>
     * @param usePaging if true (the default), paging is used. If false, paging is not used and only the first
     *                  {@link #pageSize(int)} documents are processed for each query.
     * @return the SRequest adjusted with the provided value.
     * @see #pageSize(int)
     * @see #timeProximityDeduplication(String, String...)
     * @see #deduplicateFields(String...)
     */
    public SRequest usePaging(boolean usePaging) {
        this.usePaging = usePaging;
        return this;
    }

    /**
     * Set the collection to query. If not changed, the collection will be the default collection from the overall
     * SolrWayback setup ({@code solrwayback.properties}).
     * <p>
     * Note: The collection will only be used for the base requests to Solr. {@link SRequest#expandResources} will
     * still use the default collection.
     * @param collection the collection to query.
     * @return the SRequest adjusted with the provided value.
     */
    // TODO: Figure out how to handle the expandResources situation when doing collection vs. shard divide calls
    // Maybe introduce a specific collection for that and let this method take two collections?
    public SRequest collection(String collection) {
        this.collection = collection;
        return this;
    }

    /**
     * Returns, in order of priority & existence<br>
     * - collection from {@link #shards}, under the contract that the request {@link #isSingleCollection()}<br>
     * - {@link #collection}<br>
     * - collection derived from {@link PropertiesLoader#WAYBACK_BASEURL}
     * @return the Solr collection to query. This might be a collection alias.
     */
    public String getCollectionGuaranteed() {
        if (shards != null) {
            Set<String> collections = shards.stream()
                    .filter(shard -> shard.contains(":"))
                    .map(shard -> shard.split(":")[0])
                    .collect(Collectors.toSet());
            if (collections.size() == 1) {
                return collections.iterator().next();
            } else if (collections.size() > 1) {
                throw new IllegalStateException("More than 1 collection derived from shards: " + collections);
            }
            // Fall through to other alternatives
        }

        if (collection != null) {
            return collection;
        }

        return SolrUtils.getBaseCollection();
    }

    /**
     * Limit the request to the given shards. If this value is not specified, all shards in the collection is queried.
     * <p>
     * Shards can either be simple shard names {@code myshard} or collection qualified {@code mycollection:myshard}.
     * If not qualified, {@link #collection(String)} is use. If {@code collection} is not defined, the default
     * collection will be used.
     * <p>
     * Using qualified shards in combination with {@link #shardDivide} set to {@link CHOICE#never} is not recommended.
     * In that case an exception will be thrown, unless all shards resolve to the same collection.
     * @param shards 1 or more shard names.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest shards(String... shards) {
        this.shards = Arrays.asList(shards);
        return this;
    }

    /**
     * Limit the request to the given shards. If this value is not specified, all shards in the collection is queried.
     * <p>
     * Shards can either be simple shard names {@code myshard} or collection qualified {@code mycollection:myshard}.
     * If not qualified, {@link #collection(String)} is use. If {@code collection} is not defined, the default
     * collection will be used.
     * <p>
     * Using qualified shards in combination with {@link #shardDivide} set to {@link CHOICE#never} is not recommended.
     * In that case an exception will be thrown, unless all shards resolve to the same collection.
     * @param shards 1 or more shard names.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest shards(List<String> shards) {
        this.shards = shards;
        return this;
    }

    /**
     * Extracting large search results from a Solr Cloud can be done efficiently with streaming exports.
     * Unfortunately streaming exports does not support stored fields. The alternative is to use paging,
     * either using {@code cursorMark} or sorting & grouping on a field. Both of these methods has the
     * downside of initiating cross-shard searches for each page, thus processing {@code #shards * pageSize}
     * results from {@code #shards} Solr Cloud-internal calls per overall page request.
     * <p>
     * If {@code shardDivide} it activated, it replaces the cross-shard per-page searches with {@code #shards}
     * single-shard searches, keeping the returned pages from each shard in memory while merging the
     * shard-results to a single output stream of documents. This comes at the cost of memory overhead, but
     * the average number of results processed for each overall page request is only {@code pageSize} and the
     * average number of shard calls is {@code 1}.
     * <p>
     * If {@link #shardDivide} is {@link CHOICE#never}, a standard request is created.
     * <p>
     * If {@link #shardDivide} is {@link CHOICE#always}, all shards are queried in parallel, subject to
     * connection limit rules, and the resulting streams are merged.
     * <p>
     * If {@link #shardDivide} is {@link CHOICE#auto}, a hit-counting search is performed before the
     * full request is initiated. If the number of hits is above {@link #shardDivideAutoMinHits}, shard
     * division is activated.
     * @param choice the shardDivide strategy. The default is {@link CHOICE#auto} if not specified in properties.
     * @see #shardDivideAutoMinHits(long)
     */
    public SRequest shardDivide(CHOICE choice) {
        shardDivide = choice;
        return this;
    }
    /**
     * Extracting large search results from a Solr Cloud can be done efficiently with streaming exports.
     * Unfortunately streaming exports does not support stored fields. The alternative is to use paging,
     * either using {@code cursorMark} or sorting & grouping on a field. Both of these methods has the
     * downside of initiating cross-shard searches for each page, thus processing {@code #shards * pageSize}
     * results from {@code #shards} Solr Cloud-internal calls per overall page request.
     * <p>
     * If {@code shardDivide} it activated, it replaces the cross-shard per-page searches with {@code #shards}
     * single-shard searches, keeping the returned pages from each shard in memory while merging the
     * shard-results to a single output stream of documents. This comes at the cost of memory overhead, but
     * the average number of results processed for each overall page request is only {@code pageSize} and the
     * average number of shard calls is {@code 1}.
     * <p>
     * If {@link #shardDivide} is {@link CHOICE#never}, a standard request is created.
     * <p>
     * If {@link #shardDivide} is {@link CHOICE#always}, all shards are queried in parallel, subject to
     * connection limit rules, and the resulting streams are merged.
     * <p>
     * If {@link #shardDivide} is {@link CHOICE#auto}, a hit-counting search is performed before the
     * full request is initiated. If the number of hits is {@code >=} {@link #shardDivideAutoMinHits}, shard
     * division is activated.
     * @param choice the shardDivide strategy. The default is {@link CHOICE#auto} if not specified in properties.
     * @see #shardDivideAutoMinHits(long)
     */
    public SRequest shardDivide(String choice) {
        shardDivide = CHOICE.valueOf(choice);
        return this;
    }

    /**
     * If {@link #shardDivide} is {@link CHOICE#auto}, a hit-counting search is performed before the
     * full request is initiated. The number of hits must be {@code >=} {@link #shardDivideAutoMinHits} for
     * shard division to be activated.
     * @param minHits if the shard divide strategy is auto, the search result size must be at least this amount before
     *                shard division is activated.
     *                The default is 5000 if not specified otherwise in properties.
     * @see #shardDivide(CHOICE)
     */
    public SRequest shardDivideAutoMinHits(long minHits) {
        this.shardDivideAutoMinHits = minHits;
        return this;
    }

    /**
     * Newer Solrs (at least 9+) share a default upper limit of 1024 boolean clauses recursively in the user issued
     * query tree. As multi-query uses batching, this limit can quickly be reached. Keep well below 1024.
     * @param queryBatchSize batch size when using {@link #queries(Stream)}.
     *                       Default is {@link SolrStreamDirect#DEFAULT_QUERY_BATCHSIZE}.
     * @return the SRequest adjusted with the provided value.
     */
    public SRequest queryBatchSize(int queryBatchSize) {
        this.queryBatchSize = queryBatchSize;
        return this;
    }

    /**
     * Copies {@link #solrQuery} and adjusts it with defined attributes from the SRequest, extending with needed
     * SolrRequest-attributes.
     * <p>
     * Note: This does assign {@link #query}, but not {@link #queries}: They must be handled explicitly.
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

        solrQuery.set(CommonParams.SORT, getFullSort());
        solrQuery.set(CommonParams.FL, String.join(",", getExpandedFieldList()));

        if (shards != null && !shards.isEmpty()) {
            // Under the assumption that isSingleCollection() is true
            String shardIDs = shards.stream()
                    .map(shard -> shard.contains(":") ? shard.split(":")[1] : shard)
                    .collect(Collectors.joining(","));
            solrQuery.set("shards",  shardIDs);
        }

        solrQuery.set(CommonParams.ROWS, (int) Math.min(maxResults, pageSize));

        // Disable irrelevant processing
        solrQuery.set(FacetParams.FACET, false);
        solrQuery.set(StatsParams.STATS, false);
        solrQuery.set(HighlightParams.HIGHLIGHT, false);

        return solrQuery;
    }

    /**
     * Resolve the sort param from {@link #DEFAULT_SORT}, {@link #solrQuery}-sort, {@link #sort}, {@link #idealTime},
     * {@link #deduplicateFields} and tie-breaking on {@code id}.
     * @return fully resolved {@code sort} for use with Solr requests.
     */
    public String getFullSort() {
        String sort = solrQuery == null ?
                SRequest.DEFAULT_SORT :
                solrQuery.get(CommonParams.SORT, SRequest.DEFAULT_SORT);
        if (this.sort != null) {
            sort = this.sort;
        }
        if (!(sort.endsWith("id asc") || sort.endsWith("id desc"))) {
            sort = sort + ", id asc"; // A tie breaker is needed for cursorMark and ensures deterministic order
        }
        if (idealTime != null) {
            sort = String.format(Locale.ROOT, "abs(sub(ms(%s), crawl_date)) asc, %s",
                                 idealTime, sort);
        }
        if (deduplicateFields != null) {
            for (int i = deduplicateFields.size()-1 ; i >= 0 ; i--) {
                sort = String.format(Locale.ROOT, "%s asc, %s",
                                     deduplicateFields.get(i), sort);
            }
        }
        return sort;
    }

    /**
     * @return the explicitly defined {@code solrQuery.fl} as well as fields needed by {@link #expandResources} etc.
     */
    public Set<String> getExpandedFieldList() {
        Set<String> fl = new HashSet<>();
        if (solrQuery != null && solrQuery.getFields() != null) {
            fl.addAll(Arrays.asList(solrQuery.get(CommonParams.FL).split(", *")));
        } else {
            fl.add("source_file_path");
            fl.add("source_file_offset");
        }
        if (expandResources) {
            fl.add("content_type_norm");  // Needed to determine if a resource is a webpage
            fl.add("source_file_path");   // Needed to fetch the webpage for link extraction
            fl.add("source_file_offset"); // Needed to fetch the webpage for link extraction
        }
        if (expandResources && ensureUnique) {
            fl.add("id"); // id is shorter than sourcefile@offset in webarchive-discovery compatible indexes
        }
        if (deduplicateFields != null) {
            fl.addAll(deduplicateFields);
        }
        if (uniqueFields != null) {
            fl.addAll(uniqueFields);
        }
        if (fields != null) {
            fl.addAll(fields);
        }
        return fl;
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
     * Iterates {@link #shards} and checks if all shards resolve to the same collection.
     * <p>
     * If there are null or 1 {@code shards} the quest is single collection.
     * @return true if the request is to be evaluated against a single collection.
     */
    public boolean isSingleCollection() {
        if (shards == null) {
            return true;
        }
        String defCol = collection != null ? collection : SolrUtils.getBaseCollection();
        return shards.stream()
                .map(shard -> shard.contains(":") ? shard.split(":")[0] : defCol)
                .distinct()
                .count() == 1;
    }

    /**
     * Note: Due to the nature of Streams, {@link #queries} is not deep copied.
     * @return a copy of this SRequest, as independent as possible: {@link #solrQuery} and Lists are deep-copied.
     */
    public SRequest deepCopy() {
        SRequest copy = new SRequest().
                solrClient(solrClient). // Implicitly handles useCachingClient
                solrQuery(solrQuery == null ? null : SolrUtils.deepCopy(solrQuery)).
                expandResources(expandResources).
                maxUnique(maxUnique).
                uniqueFields(uniqueFields.toArray(new String[0])).
                ensureUnique(ensureUnique). // Must be after the uniqueFields
                uniqueHashing(useHashingForUnique).
                deduplicateFields(deduplicateFields).
                fields(copy(fields)).
                maxResults(maxResults).
                sort(sort).
                query(query).
                queries(queries). // TODO: Does not really make sense. What do we do here?
                filterQueries(copy(filterQueries)).
                expandResourcesFilterQueries(copy(expandResourcesFilterQueries)).
                pageSize(pageSize).
                collection(collection).
                shards(copy(shards)).
                shardDivide(shardDivide);
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
     * <p>
     * Depending on the backing Solr Cloud topology, the collection and the {@link SRequest#shardDivide} and
     * {@link SRequest#shardDivideAutoMinHits}, either standard collection based document search & delivery or
     * shard dividing search & delivery is used to provide an Stream of {@link SolrDocument}s.
     * <p>
     * Important: This method returns a {@link dk.kb.netarchivesuite.solrwayback.util.CollectionUtils.CloseableStream}
     * and the caller <strong>must</strong> ensure that it is either depleted or closed after use, to avoid resource
     * leaking in case of shard division being used.
     * It is highly recommended to use {@code try-with-resources} directly on the returned stream:
     * <pre>
     * try (CollectionUtils.CloseableStream<SolrDocument> docs = myRequest.stream()) {
     *     long hugeIDs = docs.map(doc -> doc.get("id)).filter(id -> id.length() > 200).count();
     * }
     * </pre>
     * @return a stream of SolrDocuments.
     */
    public CollectionUtils.CloseableStream<SolrDocument> stream() {
        return SolrStreamFactory.stream(this);
    }

    /**
     * Create an iterator for the Solr documents specified for this request.
     * <p>
     * Depending on the backing Solr Cloud topology, the collection and the {@link SRequest#shardDivide} and
     * {@link SRequest#shardDivideAutoMinHits}, either standard collection based document search & delivery or
     * shard dividing search & delivery is used to provide an iterator of {@link SolrDocument}s.
     * <p>
     * Important: This method returns a {@link dk.kb.netarchivesuite.solrwayback.util.CollectionUtils.CloseableIterator}
     * and the caller <strong>must</strong> ensure that it is either depleted or closed after use, to avoid resource
     * leaking.
     * @return an iterator of SolrDocuments for this request.
     */
    public CollectionUtils.CloseableIterator<SolrDocument> iterate() {
        return SolrStreamFactory.iterate(this);
    }

    @Override
    public String toString() {
        return "SRequest(..., " +
               "query='" + limit(query, 40) + "', " +
               "queries=" + (queries == null ? "not present" : "present") + ", " +
               "filterQueries=" + limit(filterQueries, 20) + ", " +
               "fields=" + fields + ", " +
               "sort=" + sort + ", " +
               "pageSize=" + pageSize + ", " +
               "maxResults=" + maxResults + ", " +
               "deduplicateFields=" + deduplicateFields + ", " +
               "uniqueFields=" + uniqueFields + ", " +
               "ensureUnique=" + ensureUnique + ", " +
               "maxUnique=" + maxUnique + ", " +
               "uniqueHashing=" + useHashingForUnique + ", " +
               "expandResources=" + expandResources + ", " +
               "expandResourcesFilterQueries=" + limit(expandResourcesFilterQueries, 20) + ", " +
               "collection='" + collection + "'" + ", " +
               "shards=" + shards + ", " +
               "shardDivide=" + shardDivide;
    }

    /**
     * Simple String length limiter.
     * @param s any String.
     * @param maxLength {@code s} is truncated to this.
     * @return truncated String.
     */
    public static String limit(String s, int maxLength) {
        return s == null || s.length() <= maxLength ? s : s.substring(0, maxLength-3) + "...";
    }
    public static String limit(List<String> list, int maxLength) {
        return list == null ?
                "null" :
                list.stream()
                        .map(fq -> limit(fq, maxLength))
                        .collect(Collectors.joining(", ", "[", "]"));
    }

}
