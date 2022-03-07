package dk.kb.netarchivesuite.solrwayback.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

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
public class SkippingHTTPInputStreamTest {

    // Supports range requests and should be fairly permanent. 3.5 GigaBytes
    public static final String ADAMS =
            "https://labs.statsbiblioteket.dk/anskuelse/adams_history/18068_Adams_Illustrated_Panorama_of_History_1878_size_104345x11288.tif";
    public static final int BYTE_870 = 111;
    public static final int BYTE_33638 = 238;

    @Test
    public void testRangeSupport() throws IOException {
        System.out.println(SkippingHTTPInputStream.supportsRangeRequests(new URL("https://www.kb.dk/")));
    }

    @Test
    public void testBaseline() throws IOException {
        try (InputStream is = getAdamsURL().openStream()) {
            assertContent(is, "Skipping");
        }
    }

    @Test
    public void testSkipper() throws IOException {
        try (InputStream is = new SkippingHTTPInputStream(getAdamsURL())) {
            assertContent(is, "Skipping");
        }
    }

    public static void assertContent(InputStream is, String designation) throws IOException {
        byte[] BUFFER = new byte[1024];
        IOUtils.readFully(is, BUFFER);
//        assertEquals("Reading buffer of size " + BUFFER.length + " should fill the buffer",
//                     BUFFER.length, is.read(BUFFER));
        assertEquals("Reading 1KB directly with '" + designation + "' should yield the expected byte at pos 870",
                     BYTE_870, 0xFF & BUFFER[870]);

        InputStreamUtils.skipFully(is, 31*1024);
        IOUtils.readFully(is, BUFFER);
//        assertEquals("Reading buffer of size " + BUFFER.length + " should fill the buffer",
//                     BUFFER.length, is.read(BUFFER));
        assertEquals("Skipping 32KB more with ' " + designation + "' should yield the expected byte at pos 33638 (32KB + 870)",
                     BYTE_33638, 0xFF & BUFFER[870]);
    }

    private URL getAdamsURL() {
        try {
            return new URL(ADAMS);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL '" + ADAMS + "'", e);
        }
    }

}