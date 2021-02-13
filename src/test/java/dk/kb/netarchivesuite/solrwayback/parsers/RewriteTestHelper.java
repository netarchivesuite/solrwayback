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

import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Common methods for making it easier to test URL-rewriting.
 */
public class RewriteTestHelper {
    private static Log log = LogFactory.getLog(RewriteTestHelper.class);

    /**
     * Creates a mock resolver that resolves all input that contains {@code _oXXX}, where XXX is a number.
     * @return a mock resolver.
     * @param acceptNotFound
     */
    static HtmlParserUrlRewriter.NearestResolver createOXResolver(boolean acceptNotFound) {
        return (urls, timeStamp)-> urls.stream().
                map(url -> makeIndexDoc(url, acceptNotFound)).
                filter(Objects::nonNull).
                collect(Collectors.toList());
    }

    /**
     * @return a resolver that returns the URLs unchanged.
     */
    static HtmlParserUrlRewriter.NearestResolver createIdentityResolver() {
        return (urls, timeStamp)-> urls.stream().
                map(url -> {
                    IndexDocShort doc = new IndexDocShort();
                    doc.setUrl(url);
                    doc.setUrl_norm(Normalisation.canonicaliseURL(url));
                    doc.setSource_file_path("somesourcefile");
                    doc.setOffset(0);
                    return doc;
                }).
                collect(Collectors.toList());
    }

    // Fake url_norm, url, source_file, source_file_offset
    private static IndexDocShort makeIndexDoc(String url, boolean acceptNotFound) {
        if (!url.startsWith("http")) {
            log.warn("mockResolver is skipping '" + url + "' as it does not start with 'http'");
            return null;
        }
        IndexDocShort doc = new IndexDocShort();
        doc.setUrl(url);
        doc.setUrl_norm(Normalisation.canonicaliseURL(url));
        doc.setSource_file_path("somesourcefile");
        // Offset is taken from the URL string in testing
        Matcher offsetMatcher = OFFSET_PATTERN.matcher(url);
        String match = null;
        while (offsetMatcher.find()) { // We want the LAST match (so we can do substring tricks)
            match = offsetMatcher.group(1);
        }
        if (match == null) {
            if (acceptNotFound) {
                return null;
            }
            throw new IllegalArgumentException(
                    "This mock requires all URLs to contain a substring matching '" + OFFSET_PATTERN.pattern() + "'. " +
                    "The URL with match was '" + url + "'. Please adjust unit test accordingly");

        }
        doc.setOffset(Long.parseLong(match));
        return doc;
    }
    private static Pattern OFFSET_PATTERN = Pattern.compile(".*_o([0-9]+).*");

    /**
     * Retrieve the given resource as an UTF-8 String.
     * @param resource a class path entry or a file path.
     * @return the content of the resource as a String.
     * @throws IOException if the resource could not be fetch or UTF-8 parsed.
     */
    public static String fetchUTF8(String resource) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url == null) {
            Path path = Paths.get(resource);
            if (!Files.exists(path)) {
                throw new FileNotFoundException("Unable to locate '" + resource + "'");
            }
            url = path.toUri().toURL();
        }

        try (InputStream in = url.openStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream(1024);) {
            byte[] buffer = new byte[1024];
            int len = in.read(buffer);
            while (len != -1) {
                out.write(buffer, 0, len);
                len = in.read(buffer);
            }
            return out.toString("utf-8");
        }
    }
}
