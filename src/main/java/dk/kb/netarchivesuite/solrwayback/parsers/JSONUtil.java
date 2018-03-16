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
package dk.kb.netarchivesuite.solrwayback.parsers;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Generic extraction of values from JSON.
 */
public class JSONUtil {
    public static JSONSingleValueRule getSingleMatcher(String... paths) {
        return new JSONSingleValueRule(paths);
    }

    public static JSONMultiValueRule getAllMatcher(String... paths) {
        return new JSONMultiValueRule(false, false, paths);
    }

    public static JSONRule getMatcher(boolean onlyFirstMatchInPath, boolean onlyFirstMatchingPath, String... paths) {
        return new JSONRule(onlyFirstMatchInPath, onlyFirstMatchingPath, paths);
    }

    public static class JSONSingleValueRule extends JSONRule {
        public JSONSingleValueRule(String... paths) {
            super(true, true, paths);
        }
        public String match(JSONObject json) {
            return super.getSingleMatch(json);
        }
        /**
         * Used if no values are found in {@link #getSingleMatch(JSONObject)}.
         */
        public JSONSingleValueRule setDefault(String value) {
            defaultValue = value;
            return this;
        }
    }

    public static class JSONMultiValueRule extends JSONRule {
        public JSONMultiValueRule(boolean onlyFirstMatchInPath, boolean onlyFirstMatchingPath, String... paths) {
            super(onlyFirstMatchInPath, onlyFirstMatchingPath, paths);
        }

        public List<String> match(JSONObject json) {
            return super.getMatches(json);
        }
    }

    public static class JSONRule {
        private final List<String> paths;
        private final List<List<String>> pathElements;
        private final boolean onlyFirstMatchInPath;
        private final boolean onlyFirstMatchingPath;
        protected String defaultValue = null;

        public JSONRule(boolean onlyFirstMatchInPath, boolean onlyFirstMatchingPath, String... paths) {
            this.paths = Arrays.asList(paths);
            this.pathElements = new ArrayList<>(this.paths.size());
            for (String path: paths) {
                pathElements.add(splitPath(path));
            }
            this.onlyFirstMatchInPath = onlyFirstMatchInPath;
            this.onlyFirstMatchingPath = onlyFirstMatchingPath;
        }

        private List<String> splitPath(String path) {
            String[] tokens = path.split("[.]");
            if (tokens.length < 2) {
                throw new RuntimeException("Invalid JSON path '" + path + "'");
            }
            String[] pruned = new String[tokens.length-1];
            System.arraycopy(tokens, 1, pruned, 0, tokens.length-1);
            return Arrays.asList(pruned);
        }

        public String getSingleMatch(JSONObject json) {
            return getSingleMatch(json, defaultValue);
        }
        public String getSingleMatch(JSONObject json, String defaultValue) {
            List<String> matches = getMatches(json);
            return matches.isEmpty() ? defaultValue : matches.get(0);
        }

        public List<String> getMatches(JSONObject json) {
            List<String> matches = new ArrayList<>();

            for (int i = 0; i < paths.size(); i++) {
                final List<String> elements = pathElements.get(i);
                List<String> contents = getMatches(json, elements, 0);
                if (contents == null || contents.isEmpty()) {
                    continue;
                }
                if (onlyFirstMatchInPath) {
                    matches.add(contents.get(0));
                } else {
                    matches.addAll(contents);
                }
                if (onlyFirstMatchingPath) {
                    break;
                }
            }
            return matches;
        }

        private List<String> getMatches(JSONObject json, List<String> elements, int elementIndex) {
            final String element = elements.get(elementIndex);

            if (!json.has(elementName(element))) { // No match
                return Collections.emptyList();
            }

            // Are we at the end?
            if (elementIndex == elements.size()-1) {
                if (!isArrayPath(element)) { // Everything is a String in this simplified helper
                    return Collections.singletonList(json.get(elementName(element)).toString());
                }

                // Multi-value
                JSONArray array = json.getJSONArray(elementName(element));
                List<String> matches = new ArrayList<>();
                for (int i = 0 ; i < array.length() ; i++) {
                    matches.add(array.getString(i));
                }
                return matches;
            }

            // Recursive descend
            if (!isArrayPath(element)) {
                return getMatches(json.getJSONObject(elementName(element)), elements, elementIndex+1);
            }

            // Multi-value
            JSONArray array = json.getJSONArray(elementName(element));
            List<String> aggregated = new ArrayList<>();
            // TODO: Can this handle arrays of arrays?
            for (int i = 0 ; i < array.length() ; i++) {
                List<String> matches = getMatches(array.getJSONObject(i), elements, elementIndex+1);
                if (matches != null) {
                    aggregated.addAll(matches);
                }
            }
            return aggregated;
        }

        private boolean isArrayPath(String element) {
            return element.endsWith("[]");
        }

        // foo | foo[] -> foo
        private String elementName(String element) {
            return element.endsWith("[]") ? element.substring(0, element.length()-2) : element;
        }
    }

}
