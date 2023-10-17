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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SolrClient wrapper that ensures that {@link PropertiesLoader#SOLR_PARAMS_MAP}
 * are applied on each call. Also allows for overriding the default collection.
 * <p>
 * All {@link SolrClient}s used in SolrWayback should be wrapped in this!
 * <p>
 * Important: As the {@link #defaultCollection} is explicit, the given {@code inner} {@code SolrClient}s
 * must not specify the collection name in its URL. A valid URL would be {@code http://localhost:8983/solr}.
 * <p>
 * Recommended: Use the convenience method {@link #createSolrClient()} for all Solr client creation.
 */
public class RestrictedSolrClient extends SolrClient {
    private static final Logger log = LoggerFactory.getLogger(RestrictedSolrClient.class);

    /**
     * The collection to use for calls where no explicit collection is given.
     */
    private final String defaultCollection;
    /**
     * All calls are delegated to this {@code SolrClient} after request parameters has been restricted.
     */
    private final SolrClient inner;
    /**
     * Applied to all calls to the {@code RestrictedSolrClient}.
     */
    private final Map<String, String> fixedParams;

    /**
     * Create a {@link HttpSolrClient} wrapped as a {@code RestrictedSolrClient} using the property
     * {@link PropertiesLoader#SOLR_PARAMS_MAP} for restrictions and the given {@code collection} as
     * {@link #defaultCollection}.
     * <p>
     * The {@code solrBaseURL} and {@code collection} are parsed from the combined version
     * {@link PropertiesLoader#SOLR_SERVER}.
     * @return a {@code SolrClient} where all calls are restricted aka "safe".
     */
    public static RestrictedSolrClient createSolrClient() {
        Matcher m = SOLR_COLLECTION_PATTERN.matcher(PropertiesLoader.SOLR_SERVER);
        if (!m.matches()) {
            throw new IllegalStateException(String.format(
                    Locale.ROOT, "Unable to match Solr and collection from '%s' using pattern '%s'",
                    PropertiesLoader.SOLR_SERVER, SOLR_COLLECTION_PATTERN.pattern()));
        }
        return createSolrClient(m.group(1), m.group(2));
    }
    private static final Pattern SOLR_COLLECTION_PATTERN = Pattern.compile("(http.*)/([^/]+)/?$");

    /**
     * Create a {@link HttpSolrClient} wrapped as a {@code RestrictedSolrClient} using the property
     * {@link PropertiesLoader#SOLR_PARAMS_MAP} for restrictions and the given {@code collection} as
     * {@link #defaultCollection}.
     * @param solrBaseURL an URL to a Solr server, sans collection. Example: {@code http://localhost:8983/solr}.
     * @param collection the collection to use for {@link #defaultCollection}. Example: {@code netarchivebuilder}.
     * @return a {@code SolrClient} where all calls are restricted aka "safe".
     */
    public static RestrictedSolrClient createSolrClient(String solrBaseURL, String collection) {
        log.info("Creating RestrictedSolrClient(solrBaseURL='{}', collection='{}')", solrBaseURL, collection);
        return new RestrictedSolrClient(new HttpSolrClient.Builder(solrBaseURL).build(), collection);
    }

    /**
     * Construct a restricting {@code SolrClient} where the {@link #fixedParams} are taken from
     * {@link PropertiesLoader#SOLR_PARAMS_MAP}.
     * <p>
     * Important: As the {@code defaultCollection} is explicit, the given {@code inner} {@code SolrClient}
     * must not specify the collection name in its URL. A valid URL would be {@code http://localhost:8983/solr}.
     * @param inner a SolrClient set to a Solr instance without a collection name specified.
     * @param defaultCollection the collection to query is no explicit collection is given in the
     * {@code SolrClient} calls.
     */
    public RestrictedSolrClient(SolrClient inner, String defaultCollection) {
        this(inner, defaultCollection, PropertiesLoader.SOLR_PARAMS_MAP);
    }

    /**
     * Construct a restricting {@code SolrClient}.
     * <p>
     * Important: As the {@code defaultCollection} is explicit, the given {@code inner} {@code SolrClient}
     * must not specify the collection name in its URL. A valid URL would be {@code http://localhost:8983/solr}.
     * @param inner a SolrClient set to a Solr instance without a collection name specified.
     * @param defaultCollection the collection to query is no explicit collection is given in the
     * {@code SolrClient} calls.
     * @param fixedParams the fixed parameters to apply to all calls to the {@code SolrClient}.
     */
    public RestrictedSolrClient(SolrClient inner, String defaultCollection, Map<String, String> fixedParams) {
        this.inner = inner;
        this.defaultCollection = defaultCollection;
        this.fixedParams = fixedParams;
    }

    /**
     * Applies the {@link #fixedParams} on the given {@code params}, with the adjustment that existing
     * filter queries are extended instead of being replaced.
     * @param params parameters intended for searching.
     * @return restricted parameters, ready for search.
     */
    private SolrParams restrict(SolrParams params) {
        if (fixedParams == null || fixedParams.isEmpty()) {
            return params;
        }
        ModifiableSolrParams restricted = new ModifiableSolrParams(params);
        fixedParams.forEach((key, value) -> {
            if (CommonParams.FL.equals(key)) {
                restricted.add(key, value);
            } else {
                restricted.set(key, value);
            }
        });
        return restricted;
    }

    /**
     * Non-implemented {@link SolrRequest} version of {@link #restrict(SolrParams)}.
     */
    @SuppressWarnings("rawtypes")
    private SolrRequest restrict(SolrRequest request) {
        log.error("restrict(SolrRequest) called, but is not implemented yet, " +
                  "as it was alledgedly not used in SolrWayback");
        throw new UnsupportedOperationException("Restriction of SolrRequests not supported yet");
    }

    /* Delegates below where restrict(...) and defaultCollection are applied when possible */

    @Override
    public UpdateResponse add(String collection, Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
        return inner.add(collection, docs);
    }

    @Override
    public UpdateResponse add(Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
        return inner.add(defaultCollection, docs);
    }

    @Override
    public UpdateResponse add(String collection, Collection<SolrInputDocument> docs, int commitWithinMs) throws SolrServerException, IOException {
        return inner.add(collection, docs, commitWithinMs);
    }

    @Override
    public UpdateResponse add(Collection<SolrInputDocument> docs, int commitWithinMs) throws SolrServerException, IOException {
        return inner.add(defaultCollection, docs, commitWithinMs);
    }

    @Override
    public UpdateResponse add(String collection, SolrInputDocument doc) throws SolrServerException, IOException {
        return inner.add(collection, doc);
    }

    @Override
    public UpdateResponse add(SolrInputDocument doc) throws SolrServerException, IOException {
        return inner.add(defaultCollection, doc);
    }

    @Override
    public UpdateResponse add(String collection, SolrInputDocument doc, int commitWithinMs) throws SolrServerException, IOException {
        return inner.add(collection, doc, commitWithinMs);
    }

    @Override
    public UpdateResponse add(SolrInputDocument doc, int commitWithinMs) throws SolrServerException, IOException {
        return inner.add(defaultCollection, doc, commitWithinMs);
    }

    @Override
    public UpdateResponse add(String collection, Iterator<SolrInputDocument> docIterator) throws SolrServerException, IOException {
        return inner.add(collection, docIterator);
    }

    @Override
    public UpdateResponse add(Iterator<SolrInputDocument> docIterator) throws SolrServerException, IOException {
        return inner.add(defaultCollection, docIterator);
    }

    @Override
    public UpdateResponse addBean(String collection, Object obj) throws IOException, SolrServerException {
        return inner.addBean(collection, obj);
    }

    @Override
    public UpdateResponse addBean(Object obj) throws IOException, SolrServerException {
        return inner.addBean(defaultCollection, obj);
    }

    @Override
    public UpdateResponse addBean(String collection, Object obj, int commitWithinMs) throws IOException, SolrServerException {
        return inner.addBean(collection, obj, commitWithinMs);
    }

    @Override
    public UpdateResponse addBean(Object obj, int commitWithinMs) throws IOException, SolrServerException {
        return inner.addBean(defaultCollection, obj, commitWithinMs);
    }

    @Override
    public UpdateResponse addBeans(String collection, Collection<?> beans) throws SolrServerException, IOException {
        return inner.addBeans(collection, beans);
    }

    @Override
    public UpdateResponse addBeans(Collection<?> beans) throws SolrServerException, IOException {
        return inner.addBeans(defaultCollection, beans);
    }

    @Override
    public UpdateResponse addBeans(String collection, Collection<?> beans, int commitWithinMs) throws SolrServerException, IOException {
        return inner.addBeans(collection, beans, commitWithinMs);
    }

    @Override
    public UpdateResponse addBeans(Collection<?> beans, int commitWithinMs) throws SolrServerException, IOException {
        return inner.addBeans(defaultCollection, beans, commitWithinMs);
    }

    @Override
    public UpdateResponse addBeans(String collection, Iterator<?> beanIterator) throws SolrServerException, IOException {
        return inner.addBeans(collection, beanIterator);
    }

    @Override
    public UpdateResponse addBeans(Iterator<?> beanIterator) throws SolrServerException, IOException {
        return inner.addBeans(defaultCollection, beanIterator);
    }

    @Override
    public UpdateResponse commit(String collection) throws SolrServerException, IOException {
        return inner.commit(collection);
    }

    @Override
    public UpdateResponse commit() throws SolrServerException, IOException {
        return inner.commit(defaultCollection);
    }

    @Override
    public UpdateResponse commit(String collection, boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return inner.commit(collection, waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse commit(boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return inner.commit(defaultCollection, waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse commit(String collection, boolean waitFlush, boolean waitSearcher, boolean softCommit) throws SolrServerException, IOException {
        return inner.commit(collection, waitFlush, waitSearcher, softCommit);
    }

    @Override
    public UpdateResponse commit(boolean waitFlush, boolean waitSearcher, boolean softCommit) throws SolrServerException, IOException {
        return inner.commit(defaultCollection, waitFlush, waitSearcher, softCommit);
    }

    @Override
    public UpdateResponse optimize(String collection) throws SolrServerException, IOException {
        return inner.optimize(collection);
    }

    @Override
    public UpdateResponse optimize() throws SolrServerException, IOException {
        return inner.optimize(defaultCollection);
    }

    @Override
    public UpdateResponse optimize(String collection, boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return inner.optimize(collection, waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return inner.optimize(defaultCollection, waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse optimize(String collection, boolean waitFlush, boolean waitSearcher, int maxSegments) throws SolrServerException, IOException {
        return inner.optimize(collection, waitFlush, waitSearcher, maxSegments);
    }

    @Override
    public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher, int maxSegments) throws SolrServerException, IOException {
        return inner.optimize(defaultCollection, waitFlush, waitSearcher, maxSegments);
    }

    @Override
    public UpdateResponse rollback(String collection) throws SolrServerException, IOException {
        return inner.rollback(collection);
    }

    @Override
    public UpdateResponse rollback() throws SolrServerException, IOException {
        return inner.rollback(defaultCollection);
    }

    @Override
    public UpdateResponse deleteById(String collection, String id) throws SolrServerException, IOException {
        return inner.deleteById(collection, id);
    }

    @Override
    public UpdateResponse deleteById(String id) throws SolrServerException, IOException {
        return inner.deleteById(defaultCollection, id);
    }

    @Override
    public UpdateResponse deleteById(String collection, String id, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteById(collection, id, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteById(String id, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteById(defaultCollection, id, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteById(String collection, List<String> ids) throws SolrServerException, IOException {
        return inner.deleteById(collection, ids);
    }

    @Override
    public UpdateResponse deleteById(List<String> ids) throws SolrServerException, IOException {
        return inner.deleteById(defaultCollection, ids);
    }

    @Override
    public UpdateResponse deleteById(String collection, List<String> ids, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteById(collection, ids, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteById(List<String> ids, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteById(defaultCollection, ids, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteByQuery(String collection, String query) throws SolrServerException, IOException {
        return inner.deleteByQuery(collection, query);
    }

    @Override
    public UpdateResponse deleteByQuery(String query) throws SolrServerException, IOException {
        return inner.deleteByQuery(defaultCollection, query);
    }

    @Override
    public UpdateResponse deleteByQuery(String collection, String query, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteByQuery(collection, query, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteByQuery(String query, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteByQuery(defaultCollection, query, commitWithinMs);
    }

    @Override
    public SolrPingResponse ping() throws SolrServerException, IOException {
        return inner.ping();
    }

    @Override
    public QueryResponse query(String collection, SolrParams params) throws SolrServerException, IOException {
        return inner.query(collection, restrict(params));
    }

    @Override
    public QueryResponse query(SolrParams params) throws SolrServerException, IOException {
        return inner.query(defaultCollection, restrict(params));
    }

    @Override
    public QueryResponse query(String collection, SolrParams params, SolrRequest.METHOD method) throws SolrServerException, IOException {
        return inner.query(collection, params, method);
    }

    @Override
    public QueryResponse query(SolrParams params, SolrRequest.METHOD method) throws SolrServerException, IOException {
        return inner.query(defaultCollection, restrict(params), method);
    }

    @Override
    public QueryResponse queryAndStreamResponse(String collection, SolrParams params, StreamingResponseCallback callback) throws SolrServerException, IOException {
        return inner.queryAndStreamResponse(collection, restrict(params), callback);
    }

    @Override
    public QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback) throws SolrServerException, IOException {
        return inner.queryAndStreamResponse(defaultCollection, restrict(params), callback);
    }

    @Override
    public SolrDocument getById(String collection, String id) throws SolrServerException, IOException {
        return inner.getById(collection, id);
    }

    @Override
    public SolrDocument getById(String id) throws SolrServerException, IOException {
        return inner.getById(defaultCollection, id);
    }

    @Override
    public SolrDocument getById(String collection, String id, SolrParams params) throws SolrServerException, IOException {
        return inner.getById(collection, id, restrict(params));
    }

    @Override
    public SolrDocument getById(String id, SolrParams params) throws SolrServerException, IOException {
        return inner.getById(defaultCollection, id, restrict(params));
    }

    @Override
    public SolrDocumentList getById(String collection, Collection<String> ids) throws SolrServerException, IOException {
        // TODO: Should the default parameters be used with ID lookups?
        return inner.getById(collection, ids);
    }

    @Override
    public SolrDocumentList getById(Collection<String> ids) throws SolrServerException, IOException {
        return inner.getById(defaultCollection, ids);
    }

    @Override
    public SolrDocumentList getById(String collection, Collection<String> ids, SolrParams params) throws SolrServerException, IOException {
        return inner.getById(collection, ids, restrict(params));
    }

    @Override
    public SolrDocumentList getById(Collection<String> ids, SolrParams params) throws SolrServerException, IOException {
        return inner.getById(defaultCollection, ids, restrict(params));
    }

    @Override
    public NamedList<Object> request(SolrRequest request, String collection) throws SolrServerException, IOException {
        return inner.request(restrict(request), collection);
    }

    @Override
    public DocumentObjectBinder getBinder() {
        return inner.getBinder();
    }

    @Override
    public void close() throws IOException {
        inner.close();
    }
}
