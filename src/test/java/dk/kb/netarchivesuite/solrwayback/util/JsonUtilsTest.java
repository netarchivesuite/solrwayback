package dk.kb.netarchivesuite.solrwayback.util;

import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import dk.kb.netarchivesuite.solrwayback.util.JsonUtils;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
public class JsonUtilsTest {

    /** Serialize a SolrDocument to a JSON string. */
    @Test
    public void toJSON() {
        SolrDocument solrDoc = new SolrDocument();
        solrDoc.setField("text", "Foo");
        solrDoc.setField("int", 87);
        solrDoc.setField("textlist", Arrays.asList("foo", "bar\nzoo"));
        solrDoc.setField("longlist", Arrays.asList(1L, 2L));
        assertEquals("JSON serialization should qield the expected result",
                     "{\"text\":\"Foo\",\"int\":87,\"textlist\":[\"foo\",\"bar\\nzoo\"],\"longlist\":[1,2]}",
                     JsonUtils.toJSON(solrDoc));
    }

    /** Return nested value when present. */
    @Test
    public void testGetValueFound() {
        JSONObject root = new JSONObject();
        JSONObject user = new JSONObject();
        user.put("name", "Victor");
        root.put("user", user);

        String value = JsonUtils.getValue(root, "user.name");
        assertEquals("Victor", value);
    }

    /** Return null when an intermediate object is missing. */
    @Test
    public void testGetValueMissingIntermediateReturnsNull() {
        JSONObject root = new JSONObject();
        // no "account" object present
        root.put("user", new JSONObject().put("name", "Victor"));

        String value = JsonUtils.getValue(root, "account.name");
        assertNull(value);
    }

    /** Add single-token string value to collection. */
    @Test
    public void testSingleTokenAddsStringValue() {
        JSONObject root = new JSONObject();
        root.put("val", "hello");

        ArrayList<String> values = new ArrayList<>();
        JsonUtils.addAllValues(root, values, "val");

        assertEquals(1, values.size());
        assertEquals("hello", values.get(0));
    }

    /** Convert numeric value to string and add it. */
    @Test
    public void testSingleTokenAddsNumericValueAsString() {
        JSONObject root = new JSONObject();
        root.put("num", 42);

        ArrayList<String> values = new ArrayList<>();
        JsonUtils.addAllValues(root, values, "num");

        assertEquals(1, values.size());
        assertEquals("42", values.get(0));
    }

    /** Do not add when a required intermediate object is missing. */
    @Test
    public void testMissingIntermediateDoesNotAdd() {
        JSONObject root = new JSONObject();
        root.put("user", new JSONObject().put("name", "Victor"));

        ArrayList<String> values = new ArrayList<>();
        JsonUtils.addAllValues(root, values, "account.name"); // account missing

        assertEquals(0, values.size());
    }

    /** Collect values from all elements of an array path. */
    @Test
    public void testArrayPathAddsAllElements() {
        JSONObject root = new JSONObject();
        JSONArray users = new JSONArray();
        users.put(new JSONObject().put("name", "Alice"));
        users.put(new JSONObject().put("name", "Bob"));
        root.put("users", users);

        ArrayList<String> values = new ArrayList<>();
        JsonUtils.addAllValues(root, values, "users[].name");

        assertEquals(Arrays.asList("Alice", "Bob"), values);
    }

    /** Do nothing when the array path does not exist. */
    @Test
    public void testArrayPathNotPresentDoesNotAdd() {
        JSONObject root = new JSONObject();
        // no "users" array
        root.put("other", new JSONArray().put(new JSONObject().put("name", "X")));

        ArrayList<String> values = new ArrayList<>();
        JsonUtils.addAllValues(root, values, "users[].name");

        assertEquals(0, values.size());
    }

    /** Collect values from a deep nested array path. */
    @Test
    public void testDeepNestedArrayPathAddsAll() {
        // { "group": { "users": [ { "profile": { "name": "X" } }, { "profile": { "name": "Y" } } ] } }
        JSONObject profile1 = new JSONObject().put("name", "X");
        JSONObject profile2 = new JSONObject().put("name", "Y");
        JSONArray users = new JSONArray()
                .put(new JSONObject().put("profile", profile1))
                .put(new JSONObject().put("profile", profile2));
        JSONObject group = new JSONObject().put("users", users);
        JSONObject root = new JSONObject().put("group", group);

        ArrayList<String> values = new ArrayList<>();
        JsonUtils.addAllValues(root, values, "group.users[].profile.name");

        assertEquals(Arrays.asList("X", "Y"), values);
    }



}