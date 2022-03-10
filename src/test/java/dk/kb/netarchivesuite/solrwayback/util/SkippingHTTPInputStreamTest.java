package dk.kb.netarchivesuite.solrwayback.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    /**
     * Skips one gigabyte forward over HTTP and expects it to be done "fast".
     * @throws IOException if skipping failed.
     */
    @Test
    public void testLongSkip() throws IOException {
        final long MAX_MS = 2000; // No read should take longer than 2 seconds
        final byte[] BUFFER = new byte[1024];

        try (InputStream is = new SkippingHTTPInputStream(getAdamsURL())) {
            assertTime("First read", MAX_MS, () -> readSafe(is, BUFFER));
            assertTime("First skip", MAX_MS, () -> skipSafe(is, 31*1024));
            assertTime("Second read", MAX_MS, () -> readSafe(is, BUFFER));
            assertTime("Long skip", MAX_MS, () -> skipSafe(is, 1024L*1024*1024)); // 1 GB
            assertTime("Third read", MAX_MS, () -> readSafe(is, BUFFER));
            assertEquals("After skipping > 1GB, the first entry in the buffer should be as expected",
                         72, 0xFF & BUFFER[0]);
        }
    }

    private void skipSafe(InputStream is, long distance) {
        try {
            InputStreamUtils.skipFully(is, distance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readSafe(InputStream is, byte[] buffer) {
        try {
            IOUtils.readFully(is, buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertTime(String designation, long maxMS, Runnable runnable) {
        long startTime = System.nanoTime();
        runnable.run();
        long spendMS = (System.nanoTime()-startTime)/1000000L;
        assertTrue("Performing '" + designation + "' should take at most '" + maxMS + "ms, but took " + spendMS + "ms",
                   spendMS <= maxMS);
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