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
package dk.kb.labsapi;

import dk.kb.webservice.exception.InternalServiceException;
import org.apache.solr.client.solrj.*;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Caching wrapper for SolrClient. Only caches queries. puts, pings etc. are passed directly.
 */
public class CachingSolrClient extends SolrClient {
    private static final Logger log = LoggerFactory.getLogger(SolrBase.class);

    private final SolrClient inner;
    private final int maxConnections;
    protected final Semaphore connection;

    // The two caches share the size when checking is cached entries should be evicted.
    final TimeCache<QueryResponse> queryCache;
    final TimeCache<NamedList<Object>> namedCache;

    /**
     * Wrap a cache around the given inner SolrClient
     * @param inner the SolrClient to handle the calls that are not already cached.
     * @param maxCachedEntries the maximum number of entries in the cache.
     *                         Setting this to -1 disables this limit.
     * @param maxCacheTimeSeconds the maximum age of entries in the cache.
     *                            Setting this to -1 disables this limit.
     * @param maxConcurrentConnections the maximum number of concurrent connections against the inner SolrClient.
     *                                 Setting this to -1 disable this limit.
     */
    public CachingSolrClient(SolrClient inner,
                             int maxCachedEntries, int maxCacheTimeSeconds, int maxConcurrentConnections) {
        this.inner = inner;
        queryCache = new TimeCache<>(maxCachedEntries == -1 ? Integer.MAX_VALUE : maxCachedEntries,
                                    maxCacheTimeSeconds == -1 ? Integer.MAX_VALUE/4 : maxCacheTimeSeconds*1000);
        namedCache = queryCache.createLinked();
        this.maxConnections = maxConcurrentConnections;
        connection = new Semaphore(maxConcurrentConnections == -1 ? Integer.MAX_VALUE : maxConcurrentConnections,
                                   true);
    }

    /**
     * Clear all cached entries. This does not clear the calls/hits-statistics.
     */
    public void clearCache() {
        queryCache.clear();
        namedCache.clear();
    }

    /**
     * @return the number of cached elements.
     */
    public int size() {
        return queryCache.size() + namedCache.size();
    }

    /**
     * @return the number of request calls issued to this SolrClient.
     */
    public long getCalls() {
        return queryCache.getCalls() + namedCache.getCalls();
    }

    /**
     * @return the number of hits when issuing request calls to this Solrclient;
     */
    public long getHits() {
        return queryCache.getHits() + namedCache.getHits();
    }

    /**
     * Return the result of the call immediately if it is cached, else evaluate the solrCall, store it in the cache
     * and return it.
     * @param key      cache entry key.
     * @param solrCall call to perform to populate the cache.
     * @return
     */
    protected QueryResponse cachedSolrCall(String key, Supplier<QueryResponse> solrCall) {
        return queryCache.get(key, () -> {
            QueryResponse response;
            try {
                connection.acquire();
                response = solrCall.get();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to acquire a connection", e);
            } finally {
                connection.release();
            }
            return response;
        });
    }

    /**
     * Return the result of the call immediately if it is cached, else evaluate the request using the wrapped
     * SolrClient, store the result in the cache and return it.
     * Performs a Solr call for the given request, using the wrapped SolrClient.
     * @param request the request to Solr.
     * @return the response from Solr.
     * @throws RuntimeException if the Solr call could not be completed.
     */
    public QueryResponse callSolr(JsonQueryRequest request) {
        return cachedSolrCall(getKey(request), () -> {
            try {
                return request.process(inner);
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException("Exception while executing Solr request " + request, e);
            }
        });
    }

    /**
     * Calculate a key for the given query.
     * @param query a Solr query.
     * @return a key for the query, intended for the caching map.
     */
    static String getKey(JsonQueryRequest query) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            query.getContentWriter(null).write(out);
        } catch (IOException e) {
            throw new InternalServiceException("Unable to create key for query", e);
        }
        return out.toString(StandardCharsets.UTF_8) + query.getParams() + query.getQueryParams();
    }

    /**
     * Calculate a key for the given request.
     * @param query a Solr query.
     * @return a key for the query, intended for the caching map.
     */
    static String getKey(SolrRequest<?> query) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            query.getContentWriter(null).write(out);
        } catch (IOException e) {
            throw new InternalServiceException("Unable to create key for query", e);
        }
        return out.toString(StandardCharsets.UTF_8) + query.getParams() + query.getQueryParams();
    }

    @Override
    public String toString() {
        return "CachingSolrClient{" +
               "maxConnections=" + maxConnections +
               ", size/capacity=" + size() + "/" + queryCache.capacity() +
               ", maxAgeSeconds=" + queryCache.getMaxAge()/1000 +
               ", hits/calls=" + getHits() + "/" + getCalls() +
               '}';
    }

    /* ************************************************************************************************************** */
    /* Cached delegations                                                                                             */
    /* ************************************************************************************************************** */

    /**
     * If the cache contains the result of the query, it is returned immediately. Else a query for the given params
     * is performed, the result stored in the cache and returned to the caller.
     * @param params  an object holding all key/value parameters to send along the request
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} containing the response
     *         from the server
     * @throws RuntimeException {@link org.apache.solr.common.SolrException}s and {@link IOException}s are wrapped.
     */
    @Override
    public QueryResponse query(SolrParams params) throws RuntimeException {
        return cachedSolrCall(params.toString(), () -> { // request.toString represents the full request per the JavaDoc
            try {
                return inner.query(params);
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException("Exception while executing SolrClient query " + params, e);
            }
        });
    }

    /**
     * If the cache contains the result of the query, it is returned immediately. Else a query for the given params
     * is performed, the result stored in the cache and returned to the caller.
     * @param collection the Solr collection to query
     * @param params  an object holding all key/value parameters to send along the request
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} containing the response
     *         from the server
     * @throws RuntimeException {@link org.apache.solr.common.SolrException}s and {@link IOException}s are wrapped.
     */
    @Override
    public QueryResponse query(String collection, SolrParams params) {
        return cachedSolrCall("collection=" + collection + "_" + params.toString(), () -> {
            try {
                return inner.query(collection, params);
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException(
                        "Exception while executing SolrClient collection='" + collection + "', query=" + params, e);
            }
        });
    }

    /**
     * If the cache contains the result of the query, it is returned immediately. Else a query for the given params
     * is performed, the result stored in the cache and returned to the caller.
     * @param collection the Solr collection to query
     * @param params  an object holding all key/value parameters to send along the request
     * @param method  specifies the HTTP method to use for the request, such as GET or POST
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} containing the response
     *         from the server
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    @Override
    public QueryResponse query(String collection, SolrParams params, SolrRequest.METHOD method) throws SolrServerException, IOException {
        if (method == SolrRequest.METHOD.GET || method == SolrRequest.METHOD.POST) {
            // GET & POST should yield the same result so we ignore it for keys
            return cachedSolrCall("collection=" + collection + "_" + params.toString(), () -> {
                try {
                    return inner.query(collection, params, method);
                } catch (SolrServerException | IOException e) {
                    throw new RuntimeException("Exception while executing SolrClient method=" + method +
                                               ", collection='" + collection + "', query=" + params, e);
                }
            });
        }
        return inner.query(collection, params, method);
    }

    /**
     * If the cache contains the result of the query, it is returned immediately. Else a query for the given params
     * is performed, the result stored in the cache and returned to the caller.
     * @param params  an object holding all key/value parameters to send along the request
     * @param method  specifies the HTTP method to use for the request, such as GET or POST
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} containing the response
     *         from the server
     * @throws IOException If there is a low-level I/O error.
     * @throws SolrServerException if there is an error on the server
     */
    @Override
    public QueryResponse query(SolrParams params, SolrRequest.METHOD method) throws SolrServerException, IOException {
        if (method == SolrRequest.METHOD.GET || method == SolrRequest.METHOD.POST) {
            // GET & POST should yield the same result so we ignore it for keys
            return cachedSolrCall(params.toString(), () -> {
                try {
                    return inner.query(params);
                } catch (SolrServerException | IOException e) {
                    throw new RuntimeException("Exception while executing SolrClient query " + params, e);
                }
            });
        }
        return inner.query(params, method);
    }

    /**
     * If the cache contains the result of the request, it is returned immediately. Else a request for the given params
     * is performed, the result stored in the cache and returned to the caller.
     * @param request the request to execute
     * @param collection the collection to execute the request against
     * @return a {@link NamedList} containing the response from the server
     * @throws RuntimeException {@link org.apache.solr.common.SolrException}s and {@link IOException}s are wrapped.
     */
    @Override
    public NamedList<Object> request(SolrRequest request, String collection) {
        final String key = "collection=" + collection + "_" + getKey(request);
        return namedCache.get(key, () -> {
            try {
                connection.acquire();
                return inner.request(request, collection);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to acquire a connection", e);
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException(
                        "Exception while executing SolrClient collection='" + collection + "', request=" + request, e);
            } finally {
                connection.release();
            }
        });
    }

    /* ************************************************************************************************************** */
    /* Straight delegations                                                                                           */
    /* ************************************************************************************************************** */

    @Override
    public UpdateResponse add(String collection, Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
        return inner.add(collection, docs);
    }

    @Override
    public UpdateResponse add(Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
        return inner.add(docs);
    }

    @Override
    public UpdateResponse add(String collection, Collection<SolrInputDocument> docs, int commitWithinMs) throws SolrServerException, IOException {
        return inner.add(collection, docs, commitWithinMs);
    }

    @Override
    public UpdateResponse add(Collection<SolrInputDocument> docs, int commitWithinMs) throws SolrServerException, IOException {
        return inner.add(docs, commitWithinMs);
    }

    @Override
    public UpdateResponse add(String collection, SolrInputDocument doc) throws SolrServerException, IOException {
        return inner.add(collection, doc);
    }

    @Override
    public UpdateResponse add(SolrInputDocument doc) throws SolrServerException, IOException {
        return inner.add(doc);
    }

    @Override
    public UpdateResponse add(String collection, SolrInputDocument doc, int commitWithinMs) throws SolrServerException, IOException {
        return inner.add(collection, doc, commitWithinMs);
    }

    @Override
    public UpdateResponse add(SolrInputDocument doc, int commitWithinMs) throws SolrServerException, IOException {
        return inner.add(doc, commitWithinMs);
    }

    @Override
    public UpdateResponse add(String collection, Iterator<SolrInputDocument> docIterator) throws SolrServerException, IOException {
        return inner.add(collection, docIterator);
    }

    @Override
    public UpdateResponse add(Iterator<SolrInputDocument> docIterator) throws SolrServerException, IOException {
        return inner.add(docIterator);
    }

    @Override
    public UpdateResponse addBean(String collection, Object obj) throws IOException, SolrServerException {
        return inner.addBean(collection, obj);
    }

    @Override
    public UpdateResponse addBean(Object obj) throws IOException, SolrServerException {
        return inner.addBean(obj);
    }

    @Override
    public UpdateResponse addBean(String collection, Object obj, int commitWithinMs) throws IOException, SolrServerException {
        return inner.addBean(collection, obj, commitWithinMs);
    }

    @Override
    public UpdateResponse addBean(Object obj, int commitWithinMs) throws IOException, SolrServerException {
        return inner.addBean(obj, commitWithinMs);
    }

    @Override
    public UpdateResponse addBeans(String collection, Collection<?> beans) throws SolrServerException, IOException {
        return inner.addBeans(collection, beans);
    }

    @Override
    public UpdateResponse addBeans(Collection<?> beans) throws SolrServerException, IOException {
        return inner.addBeans(beans);
    }

    @Override
    public UpdateResponse addBeans(String collection, Collection<?> beans, int commitWithinMs) throws SolrServerException, IOException {
        return inner.addBeans(collection, beans, commitWithinMs);
    }

    @Override
    public UpdateResponse addBeans(Collection<?> beans, int commitWithinMs) throws SolrServerException, IOException {
        return inner.addBeans(beans, commitWithinMs);
    }

    @Override
    public UpdateResponse addBeans(String collection, Iterator<?> beanIterator) throws SolrServerException, IOException {
        return inner.addBeans(collection, beanIterator);
    }

    @Override
    public UpdateResponse addBeans(Iterator<?> beanIterator) throws SolrServerException, IOException {
        return inner.addBeans(beanIterator);
    }

    @Override
    public UpdateResponse commit(String collection) throws SolrServerException, IOException {
        return inner.commit(collection);
    }

    @Override
    public UpdateResponse commit() throws SolrServerException, IOException {
        return inner.commit();
    }

    @Override
    public UpdateResponse commit(String collection, boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return inner.commit(collection, waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse commit(boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return inner.commit(waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse commit(String collection, boolean waitFlush, boolean waitSearcher, boolean softCommit) throws SolrServerException, IOException {
        return inner.commit(collection, waitFlush, waitSearcher, softCommit);
    }

    @Override
    public UpdateResponse commit(boolean waitFlush, boolean waitSearcher, boolean softCommit) throws SolrServerException, IOException {
        return inner.commit(waitFlush, waitSearcher, softCommit);
    }

    @Override
    public UpdateResponse optimize(String collection) throws SolrServerException, IOException {
        return inner.optimize(collection);
    }

    @Override
    public UpdateResponse optimize() throws SolrServerException, IOException {
        return inner.optimize();
    }

    @Override
    public UpdateResponse optimize(String collection, boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return inner.optimize(collection, waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher) throws SolrServerException, IOException {
        return inner.optimize(waitFlush, waitSearcher);
    }

    @Override
    public UpdateResponse optimize(String collection, boolean waitFlush, boolean waitSearcher, int maxSegments) throws SolrServerException, IOException {
        return inner.optimize(collection, waitFlush, waitSearcher, maxSegments);
    }

    @Override
    public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher, int maxSegments) throws SolrServerException, IOException {
        return inner.optimize(waitFlush, waitSearcher, maxSegments);
    }

    @Override
    public UpdateResponse rollback(String collection) throws SolrServerException, IOException {
        return inner.rollback(collection);
    }

    @Override
    public UpdateResponse rollback() throws SolrServerException, IOException {
        return inner.rollback();
    }

    @Override
    public UpdateResponse deleteById(String collection, String id) throws SolrServerException, IOException {
        return inner.deleteById(collection, id);
    }

    @Override
    public UpdateResponse deleteById(String id) throws SolrServerException, IOException {
        return inner.deleteById(id);
    }

    @Override
    public UpdateResponse deleteById(String collection, String id, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteById(collection, id, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteById(String id, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteById(id, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteById(String collection, List<String> ids) throws SolrServerException, IOException {
        return inner.deleteById(collection, ids);
    }

    @Override
    public UpdateResponse deleteById(List<String> ids) throws SolrServerException, IOException {
        return inner.deleteById(ids);
    }

    @Override
    public UpdateResponse deleteById(String collection, List<String> ids, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteById(collection, ids, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteById(List<String> ids, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteById(ids, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteByQuery(String collection, String query) throws SolrServerException, IOException {
        return inner.deleteByQuery(collection, query);
    }

    @Override
    public UpdateResponse deleteByQuery(String query) throws SolrServerException, IOException {
        return inner.deleteByQuery(query);
    }

    @Override
    public UpdateResponse deleteByQuery(String collection, String query, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteByQuery(collection, query, commitWithinMs);
    }

    @Override
    public UpdateResponse deleteByQuery(String query, int commitWithinMs) throws SolrServerException, IOException {
        return inner.deleteByQuery(query, commitWithinMs);
    }

    @Override
    public SolrPingResponse ping(String collection) throws SolrServerException, IOException {
        return inner.ping(collection);
    }

    @Override
    public SolrPingResponse ping() throws SolrServerException, IOException {
        return inner.ping();
    }

    @Override
    public QueryResponse queryAndStreamResponse(String collection, SolrParams params, StreamingResponseCallback callback) throws SolrServerException, IOException {
        return inner.queryAndStreamResponse(collection, params, callback);
    }

    @Override
    public QueryResponse queryAndStreamResponse(String collection, SolrParams params, FastStreamingDocsCallback callback) throws SolrServerException, IOException {
        return inner.queryAndStreamResponse(collection, params, callback);
    }

    @Override
    public QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback) throws SolrServerException, IOException {
        return inner.queryAndStreamResponse(params, callback);
    }

    @Override
    public SolrDocument getById(String collection, String id) throws SolrServerException, IOException {
        return inner.getById(collection, id);
    }

    @Override
    public SolrDocument getById(String id) throws SolrServerException, IOException {
        return inner.getById(id);
    }

    @Override
    public SolrDocument getById(String collection, String id, SolrParams params) throws SolrServerException, IOException {
        return inner.getById(collection, id, params);
    }

    @Override
    public SolrDocument getById(String id, SolrParams params) throws SolrServerException, IOException {
        return inner.getById(id, params);
    }

    @Override
    public SolrDocumentList getById(String collection, Collection<String> ids) throws SolrServerException, IOException {
        return inner.getById(collection, ids);
    }

    @Override
    public SolrDocumentList getById(Collection<String> ids) throws SolrServerException, IOException {
        return inner.getById(ids);
    }

    @Override
    public SolrDocumentList getById(String collection, Collection<String> ids, SolrParams params) throws SolrServerException, IOException {
        return inner.getById(collection, ids, params);
    }

    @Override
    public SolrDocumentList getById(Collection<String> ids, SolrParams params) throws SolrServerException, IOException {
        return inner.getById(ids, params);
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
