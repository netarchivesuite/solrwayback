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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP InputStream with the special feature that {@link InputStream#skip(long)} uses HTTP Range Request to perform
 * efficient skipping.
 */
public class SkippingHTTPInputStream extends InputStream {
    private static final Logger log = LoggerFactory.getLogger(SkippingHTTPInputStream.class);

    // Skips smaller than this are handled by reads instead of creating a new connection
    private static final long TRIVIAL_SKIP = 8192;

    private final boolean fallbackToRead;
    private final URL url;

    private final boolean supportsRangeRequests;
    private long position = 0;
    private InputStream inner;

    /**
     * Construct an InputStream for the given url, with the special feature that calling {@link InputStream#skip(long)}
     * will use HTTP Range Request to perform efficient skipping. If the server does not support Range Requests, calls
     * to skip will fail.
     * @param url a HTTP or HTTPS URL.
     * @return an InputStream for HTTP-URLs with efficient skipping.
     * @throws java.io.IOException if the stream could not be constructed.
     * @see "https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests"
     */
    public SkippingHTTPInputStream(URL url) throws IOException {
        this(url, false);
    }
    /**
     * Construct an InputStream for the given url, with the special feature that calling {@link InputStream#skip(long)}
     * will use HTTP Range Request to perform efficient skipping.
     * @param url a HTTP or HTTPS URL.
     * @param fallbackToRead if true, the implementation will use {@link InputStream#read} if the server does not
     *                       support Range Requests. If false, calls to {@link InputStream#skip(long)} will fail.
     * @return an InputStream for HTTP-URLs with efficient skipping.
     * @throws java.io.IOException if the stream could not be constructed.
     * @see "https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests"
     */
    public SkippingHTTPInputStream(URL url, boolean fallbackToRead) throws IOException {
        this.url = url;
        this.fallbackToRead = fallbackToRead;
        supportsRangeRequests = supportsRangeRequests(url);
        log.debug("Created " + this);
    }

    /**
     * @param url a HTTP or HTTPS URL.
     * @return true if the server supports HTTP Range Requests.
     */
    public static boolean supportsRangeRequests(URL url) throws IOException {
        URLConnection urlCon = url.openConnection();
        // There is no explicit close() for URLConnection. Hopefully getHeaderFields closes after itself
        Map<String, List<String>> map = urlCon.getHeaderFields();
        List<String> acceptRanges = map.get("Accept-Ranges");
        return acceptRanges != null && acceptRanges.contains("bytes");
    }

    /**
     * Connect to the url given in the constructor, using a Range Request if {@code startOffset != 0}.
     * @param startOffset start position in the data.
     * @return startOffset.
     * @throws IOException if the connection at the given offset could not be established.
     */
    long connect(long startOffset) throws IOException {
        if (inner != null) {
            try {
                inner.close();
            } catch (IOException e) {
                log.warn("IOException closing existing inner as preparation for Range Request to pos " + startOffset);
            }
        }

        URLConnection urlCon = createURLConnection(startOffset);
        inner = urlCon.getInputStream();
        position = startOffset;
        return startOffset;
    }

    /**
     * Create a URLConnection where the available inputStream starts at startOffset.
     * @param startOffset start position in the data.
     * @return a URLConnection where the available inputStream starts at startOffset.
     * @throws IOException if the connection at the given offset could not be established.
     */
    URLConnection createURLConnection(long startOffset) throws IOException {
        URLConnection urlCon = url.openConnection();
        urlCon.setRequestProperty("User-Agent", "Java Client; SolrWayback");
        urlCon.setRequestProperty("Accept", "*/*");
        urlCon.setRequestProperty("Connection", "close");
        if (startOffset > 0) {
            if (!supportsRangeRequests) {
                throw new IOException("connect(startOffset=" + startOffset + ") called for server that does not " +
                                      "support HTTP Range Requests: " + url);
            }
            // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Range
            urlCon.setRequestProperty("Range", "bytes=" + startOffset + "-");
        }
        return urlCon;
    }

    /**
     * @return the position in the resource, measured in bytes.
     */
    public long getPosition() {
        return position;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n < 0) {
            throw new IllegalArgumentException("Cannot skip a negative amount: " + n);
        }
        if (n == 0) {
            return 0;
        }

        if (n < TRIVIAL_SKIP || (!supportsRangeRequests && fallbackToRead)) {
            // Skip by delegating to the standard HTTP/HTTPS-InputStream which uses read for skips
            return skipByDelegation(n);
        };

        if (!supportsRangeRequests) {
            throw new IOException("Skip of " + n + " bytes requested but the server does not support " +
                                  "HTTP Range Requests and fallbackToRead == false: " + url);
        }

        // Skip by creating a new connection
        long oldPosition = position;
        connect(position + n);
        if (position != oldPosition + n) {
            throw new IOException(String.format(
                    Locale.ROOT,
                    "Unable to (re)connect and skip %d bytes from %d to %d. Only skipped %d bytes to %d: %s",
                    n, oldPosition, oldPosition + n, position - oldPosition, position, url));
        }
        return n;
    }

    /**
     * Delegates skipping to inner, which typically means reading instead of skipping.
     */
    long skipByDelegation(long n) throws IOException {
        if (inner == null) {
            connect(0);
        }

        long skipped = inner.skip(n); // Note that skip does not guarantee skipping n, only <= n
        if (skipped != n) {
            log.debug(String.format(
                    Locale.ROOT, "Unable to delegate-skip %d bytes from %d to %d. Only skipped %d bytes to %d: %s",
                    n, position, position + n, skipped, position + skipped, url));
        }
        position += skipped;
        return n;
    }

    @Override
    public int read() throws IOException {
        if (inner == null) {
            connect(0);
        }
        int b = inner.read();
        if (b != -1) {
            ++position;
        }
        return b;
    }

    // No need to override
    // public int read(byte[] b) throws IOException
    // as it calls read(byte[] b, int off, int len)


    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (inner == null) {
            connect(0);
        }
        int read = inner.read(b, off, len);
        position += read;
        return read;
    }

    @Override
    public int available() throws IOException {
        if (inner == null) {
            connect(0);
        }
        return inner.available();
    }

    @Override
    public void close() throws IOException {
        if (inner != null) {
            inner.close();
        }
    }

    @Override
    public String toString() {
        return "SkippingHTTPInputStream(" +
               "url=" + url +
               ", fallbackToRead=" + fallbackToRead +
               ", supportsRangeRequests=" + supportsRangeRequests +
               ", position=" + position +
               ')';
    }
}
