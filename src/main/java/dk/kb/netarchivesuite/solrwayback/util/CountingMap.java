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

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Map that keeps track of the number of successfull and unsuccessfull {@link #get(Object)} calls.
 */
public class CountingMap<K, V> extends HashMap<K, V> {
    private static Log log = LogFactory.getLog(CountingMap.class);

    private int found = 0;
    private int fail = 0;

    @Override
    public V get(Object o) {
        V value = super.get(o);
        if (value == null) {
            fail++;
        } else {
            found++;
        }
        return value;
    }

    public int getFoundCount() {
        return found;
    }

    public int getFailCount() {
        return fail;
    }
}
