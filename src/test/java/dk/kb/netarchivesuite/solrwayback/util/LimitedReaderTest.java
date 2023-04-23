package dk.kb.netarchivesuite.solrwayback.util;

import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.StringReader;

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
public class LimitedReaderTest extends TestCase {

    public void testExact() throws IOException {
        String TEXT = "foobar";
        assertEquals(TEXT, readHelper(TEXT, TEXT.length()));
    }

    public void testTruncated() throws IOException {
        String TEXT = "foobar";
        assertEquals("foo", readHelper(TEXT, 3));
    }

    public void testOvercommit() throws IOException {
        String TEXT = "foobar";
        assertEquals(TEXT, readHelper(TEXT, TEXT.length()*2));
    }

    /**
     * Creates a {@link LimitedReader} from the given {@code text} with the given {@code limit},
     * then reads the full content as a String and returns that.
     * @param text  source for the {@link LimitedReader}.
     * @param limit limit for the {@link LimitedReader}.
     * @return the full String content of the constructed {@link LimitedReader}.
     */
    private String readHelper(String text, int limit) throws IOException {
        LimitedReader reader = new LimitedReader(new StringReader(text), limit);
        return IOUtils.toString(reader);
    }
}