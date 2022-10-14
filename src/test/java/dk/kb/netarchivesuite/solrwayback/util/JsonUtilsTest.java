package dk.kb.netarchivesuite.solrwayback.util;

import junit.framework.TestCase;
import org.apache.solr.common.SolrDocument;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

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
}