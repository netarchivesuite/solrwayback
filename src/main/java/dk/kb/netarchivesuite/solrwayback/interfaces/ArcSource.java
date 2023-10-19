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
package dk.kb.netarchivesuite.solrwayback.interfaces;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.util.SkippingHTTPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Encapsulates a supplier of data from a WARC- or ARC, independent of storage system.
 *
 * When HTTP sources are used, the server SHOULD support HTTP range requests:
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
 * It is possible to use HTTP servers without range requests, although this is extremely inefficient
 * for larger WARCs. To enable non-range support, set
 * {@code warc.file.resolver.source.http.readfallback=true}
 */
public class ArcSource implements Supplier<InputStream> {
    private static final Logger log = LoggerFactory.getLogger(ArcSource.class);

    private static final Pattern HTTP = Pattern.compile("^https?://.*");
    private static final Pattern FILE = Pattern.compile("^file://.*");

    private final String source;
    private final Supplier<InputStream> supplier;

    /**
     * It is highly recommended to ensure that the {@link InputStream} delivered by the {@code supplier} handles
     * {@link InputStream#skip(long)} effectively as the primary use case of {@code ArcSource} is to extract a single
     * entry from a (W)ARC by skipping to entry start and reading forward from there.
     * @param source the source (URL, file path or similar) of the ArcData.
     */
    public ArcSource(String source, Supplier<InputStream> supplier) {
        this.source = source;
        this.supplier = supplier;
    }

    /**
     * @return the source of the data. Used for guessing the type of the source (warc/warc.gz/arc/arc.gz) by
     * looking at the last part of the String.
     */
    public String getSource() {
        return source;
    }

    /**
     * @return a stream with the full content of an ARC or a WARC file.
     */
    @Override
    public InputStream get() {
        return supplier.get();
    }

    /**
     * Construct an ArcSource from a file path.
     * <p>
     * Consider using the general {@link #create(String)} instead of this method.
     * @param file a file on the local file system.
     * @return an ArcSource for the given file.
     */
    public static ArcSource fromFile(String file) {
        return new ArcSource(file, () -> {
            try {
                // TODO: Verify that Files.newInputStream supports efficient skipping then switch to that
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                log.error("FileNotFoundException trying to access (W)ARC '{}'", file);
                throw new RuntimeException("FileNotFoundException trying to access (W)ARC '" + file + "'", e);
            } catch (Exception e) {
                log.error("Unable to create FileInputStream for (W)ARC '" + file + "'", e);
                throw new RuntimeException("Unable to create FileInputStream for (W)ARC '" + file + "'", e);
            }
        });
    }

    /**
     * Construct an ArcSource from a http(s) URL.
     * <p>
     * Consider using the general {@link #create(String)} instead of this method.
     * @param httpURL an URL for a WARC.
     * @return an ArcSource for the given file.
     */
    public static ArcSource fromHTTP(String httpURL) {
        final URL url;
        try {
            url = new URL(httpURL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Unable to construct URL from '" + httpURL + "'", e);
        }

        return new ArcSource(httpURL, () -> {
            try {
                return new SkippingHTTPInputStream(url, PropertiesLoader.WARC_SOURCE_HTTP_FALLBACK);
            } catch (IOException e) {
                // TODO: This could be extended with a check for 404 for better error message
                log.error("Unable to open stream for '" + httpURL + "'", e);
                throw new RuntimeException("Unable to open stream for '" + httpURL + "'", e);
            }
        });
    }
    /**
     * Construct an ArcSource from multiple possible source types.
     * Supported sources are
     * <ul>
     *     <li>File path ({@code /mounts/archive/ab/cd/harvest_abcdefgh.warc.gz}</li>
     *     <li>File URL  ({@code file:///mounts/archive/ab/cd/harvest_abcdefgh.warc.gz}</li>
     *     <li>HTTP URL  ({@code http://archive.example.com/warcs/ab/cd/harvest_abcdefgh.warc.gz}</li>
     *     <li>HTTPS URL ({@code https://archive.example.com/warcs/ab/cd/harvest_abcdefgh.warc.gz}</li>
     * </ul>
     * Note: In order for HTTP(S) to be efficient as a delivery mechanism for WARC content, the server must support
     * HTTP range requests: https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
     * @param source a path on the file system or an URL.
     * @return an ArcSource for the given WARC.
     */
    public static ArcSource create(String source) {
        if (HTTP.matcher(source).matches()) {
            return fromHTTP(source);
        }
        if (FILE.matcher(source).matches()) {
            return fromFile(source.substring("file://".length()));
        }
        // No protocol means file path
        return fromFile(source);
    }

    @Override
    public String toString() {
        return "ArcSource(" + "source='" + source + '\'' + ')';
    }
}
