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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Time oriented cache where oldest entry can be evicted either due to not being accessed in a given time or the cache
 * being full. The cache can share size with other TimeCaches when checking for fullness.
 */
public class TimeCache<O> implements Map<String, O> {
    private static final Logger log = LoggerFactory.getLogger(TimeCache.class);

    private final Map<String, TimeEntry<O>> inner;
    private final int maxCapacity;
    private final long maxAgeMS;
    private final AtomicLong calls = new AtomicLong(0);
    private final AtomicLong hits = new AtomicLong(0);

    private final Set<TimeCache<?>> linkedCaches = new HashSet<>();

    /**
     * @param maxCapacity the maximum numbers of entries to hold in the cache.
     * @param maxAgeMS    the maximum number og milliseconds that an object can exist in the cache.
     */
    public TimeCache(int maxCapacity, long maxAgeMS) {
        super();
        this.maxCapacity = maxCapacity;
        this.maxAgeMS = maxAgeMS;
        this.inner = Collections.synchronizedMap(new LinkedHashMap<String, TimeEntry<O>>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, TimeEntry<O>> eldest) {
                int totalSize = size() + linkedCaches.stream().mapToInt(TimeCache::size).sum();
                return totalSize > maxCapacity || eldest.getValue().isTooOld();
            }
        });
    }

    /**
     * Create a new cache, typically with another type, that is linked to this cache.
     * Linked cache has shared capacity.
     * @param <T> the type of the cache.
     * @return a new cache with limits (max count and age) shared with this cache.
     */
    public <T> TimeCache<T> createLinked() {
        TimeCache<T> other = new TimeCache<T>(maxCapacity, maxAgeMS);
        other.link(this);
        return other;
    }
    
    /**
     * Link this cache to the other cache. maxCapacity must be equal and current capacity will be the sum.
     * More than 2 caches can be linked.
     * @param other another cache to link to.
     */
    private void link(TimeCache<?> other) {
        if (linkedCaches.contains(other)) {
            return; // Already linked. Needed to avoid endless loop
        }
        linkedCaches.add(other);
        linkedCaches.forEach(c -> link(this));
    }

    /**
     * Get the object with the given key from the cache. If the object is not available, attempt to create a new one
     * using the supplier. If a new object is created, add it to the cache and return it.
     *
     * If the key is null, no caching is attempted and the supplier is called directly.
     * @param key      the key for the object to retrieve.
     * @param supplier used for creating the object if it is not available.
     * @return the object corresponding to the key.
     */
    public O get(String key, Supplier<O> supplier) {
        if (key == null) {
            log.debug("get(null, ...) called: No caching is performed");
            return supplier.get();
        }
        O o = get(key);
        if (o == null) {
            o = supplier.get();
            if (o != null) {
                put(key, o);
            }
        }
        return o;
    }

    @Override
    public O get(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        calls.incrementAndGet();
        TimeEntry<O> o = inner.get(key);
        if (o == null) {
            return null;
        }
        if (o.isTooOld()) {
            inner.remove(key);
            return null;
        }
        hits.incrementAndGet();
        inner.put((String)key, o);
        return o.getValue();
    }

    @Override
    public O getOrDefault(Object key, O defaultValue) {
        return Optional.ofNullable(get(key)).orElse(defaultValue);
    }

    /**
     * The number of times a value was requested from the cache.
     */
    public long getCalls() {
        return calls.get();
    }

    /**
     * The number of hits (the value was available) in the cache.
     */
    public long getHits() {
        return hits.get();
    }

    @Override
    public int size() {
        return inner.size();
    }

    public int capacity() {
        return maxCapacity;
    }

    public long getMaxAgeMS() {
        return maxAgeMS;
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return inner.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        synchronized (inner) {
            return inner.values().stream()
                    .map(TimeEntry::getValue)
                    .anyMatch(value::equals);
        }
    }

    @Override
    public O put(String key, O value) {
        TimeEntry<O> entry = new TimeEntry<>(value);
        return Optional.ofNullable(inner.put(key, entry))
                .map(TimeEntry::getValue)
                .orElse(null);
    }

    @Override
    public O remove(Object key) {
        return Optional.ofNullable(inner.remove(key))
                .map(TimeEntry::getValue)
                .orElse(null);
    }

    @Override
    public void putAll(Map<? extends String, ? extends O> m) {
        m.forEach((key, value) -> inner.put(key, new TimeEntry<>(value)));
    }

    @Override
    public void clear() {
        inner.clear();
    }

    /**
     * Calling keySet triggers a shallow copy of all keys. This is potentially a heavy operation!
     */
    @Override
    public Set<String> keySet() {
        synchronized (inner) {
            return new HashSet<>(inner.keySet());
        }
    }

    /**
     * Calling values triggers a shallow copy of all values. This is potentially a heavy operation!
     */
    @Override
    public Collection<O> values() {
        synchronized (inner) {
            return inner.values().stream()
                    .map(TimeEntry::getValue)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Calling entrySet triggers a full build of the response set. This is potentially a heavy operation!
     */
    @Override
    public Set<Entry<String, O>> entrySet() {
        synchronized (inner) {
            return inner.entrySet().stream()
                    .map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue().getValue()))
                    .collect(Collectors.toSet());
        }
    }

    /* Helper class */

    public class TimeEntry<O> {
        private final O value;
        private final Instant created = Instant.now();

        public TimeEntry(O o) {
            this.value = o;
        }

        public Instant getCreated() {
            return created;
        }

        public O getValue() {
            return value;
        }

        public boolean isTooOld() {
            return getCreated().plus(maxAgeMS, ChronoUnit.MILLIS).isBefore(Instant.now());
        }
    }
}
