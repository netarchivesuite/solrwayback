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

import dk.kb.netarchivesuite.solrwayback.util.SkippingHTTPInputStreamTest;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Piggy backs on {@link dk.kb.netarchivesuite.solrwayback.util.SkippingHTTPInputStreamTest}.
 */
public class ArcHTTPResolverTest {
    public static final String ADAMS_FILE = "foo/18068_Adams_Illustrated_Panorama_of_History_1878_size_104345x11288.tif";

    @Test
    public void testSkip() throws IOException {
        Map<String, String> skipParams = new HashMap<>();
        skipParams.put(ArcHTTPResolver.REGEXP_KEY, ".*/([^/]*)");
        skipParams.put(ArcHTTPResolver.REPLACEMENT_KEY, "https://labs.statsbiblioteket.dk/anskuelse/adams_history/$1");
        skipParams.put(ArcHTTPResolver.READ_FALLBACK_KEY, "false");
        ArcHTTPResolver resolver = new ArcHTTPResolver();
        resolver.setParameters(skipParams);
        resolver.initialize();

        ArcSource source = resolver.resolveArcFileLocation(ADAMS_FILE);

        // Base verify of skip
        try (InputStream is = source.get()) {
            SkippingHTTPInputStreamTest.assertContent(is, "Adams_take1");
        }

        // Verify that multiple calls to get works as intended
        try (InputStream is = source.get()) {
            SkippingHTTPInputStreamTest.assertContent(is, "Adams_take2");
        }

    }


}