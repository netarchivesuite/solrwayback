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

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Acts as a uniqueness filter for a {@code Stream<SolrDocument>}, using the content from given fields as unique values.
 * This filter holds values for all unique encountered documents in memory and thus does not scale indefinitely.
 * When constructed, the maximum number of unique values to track is specified. If this limit is reached, an exception
 * is thrown.
 * <p>
 * This implementation is not thread safe.
 */
public class UniqueFilter implements Predicate<SolrDocument> {
    private static final Logger log = LoggerFactory.getLogger(UniqueFilter.class);

    private final List<String> fields;
    private final int maxUnique;
    private final Set<String> uniqueValues;
    private final IntSet uniqueHashes;

    public long tests = 0;
    public long duplicates = 0;

    /**
     * @param useHashing true, hashes of the values from the given fields are used for tracking instead of the
     *                   values themselves. Using hashing vastly (factor 10+) reduce the memory footprint, but
     *                   introduces the possibility of hash collisions and thereby does not guarantee uniqueness.
     *                   Hashing is recommended unless there is a strict need for uniqueness.
     * @param maxUnique the maximum number of unique elements before an exception is thrown.
     * @param fields the fields to use for uniqueness tracking.
     */
    public UniqueFilter(boolean useHashing, int maxUnique, String... fields) {
        this(useHashing, maxUnique, Arrays.asList(fields));
    }

    /**
     * @param useHashing true, hashes of the values from the given fields are used for tracking instead of the
     *                   values themselves. Using hashing vastly (factor 10+) reduce the memory footprint, but
     *                   introduces the possibility of hash collisions and thereby does not guarantee uniqueness.
     *                   Hashing is recommended unless there is a strict need for uniqueness.
     * @param maxElements the maximum number of unique elements before an exception is thrown.
     * @param fields the fields to use for uniqueness tracking.
     */
    public UniqueFilter(boolean useHashing, int maxElements, List<String> fields) {
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("No fields provided");
        }
        this.fields = fields;
        this.maxUnique = maxElements;
        if (useHashing) {
            uniqueHashes = new IntOpenHashSet();
            uniqueValues = null;
        } else {
            uniqueHashes = null;
            uniqueValues = new HashSet<>();
        }
    }

    @Override
    public boolean test(SolrDocument solrDoc) {
        return test(getUniqueValue(solrDoc));
    }
    public synchronized boolean test(String fieldValue) {
        tests++;
        boolean ok;
        if (uniqueValues != null) { // values
            ok = uniqueValues.add(fieldValue);
        } else {
            try {
                ok = uniqueHashes.add(fieldValue.hashCode());
            } catch (Exception e) {
                throw new RuntimeException("Exception adding hash " + fieldValue.hashCode() + " to set with " + uniqueHashes.size() + " elements for '" + fieldValue + "'", e);
            }
        }
        if (uniqueCount() > maxUnique) {
            log.warn("Throwing ArrayIndexOutOfBoundsException as the unique limit of {} has been reached", maxUnique);
            throw new ArrayIndexOutOfBoundsException(
                    "The number of elements in the unique tracker exceeded the limit " + maxUnique +
                    ". Processing has been stopped to avoid Out Of Memory errors");
        }
        return ok;
    }

    /**
     * Construct the value used to check for uniqueness by concatenating the content from Solr fields.
     * The fields used are defined as {@link #fields} when constructing the {@code Uniquefilter}.
     * @param solrDoc a solrDoc containing at least {@link #fields}.
     * @return concatenation of the field values.
     */
    private String getUniqueValue(SolrDocument solrDoc) {
        return fields.stream().
                map(field -> solrDoc.getFieldValue(field).toString()).
                collect(Collectors.joining("_/_"));
    }

    /**
     * @return the number of unique values encountered.
     */
    public int uniqueCount() {
        return uniqueValues != null ? uniqueValues.size() : uniqueHashes.size();
    }


    /**
     * @return the number of uniqueness tests performed.
     */
    public long testCount() {
        return tests;
    }

    /**
     * @return the number of duplicates encountered.
     */
    public long duplicateCount() {
        return duplicates;
    }
}
