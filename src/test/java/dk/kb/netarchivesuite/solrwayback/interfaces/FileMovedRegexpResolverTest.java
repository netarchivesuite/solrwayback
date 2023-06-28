package dk.kb.netarchivesuite.solrwayback.interfaces;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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
public class FileMovedRegexpResolverTest {

    @Test
    public void basicRewrite() {
        ArcFileLocationResolverInterface resolver = createRegexpResolver(
                "/home/harvest/(.*)", "/netarchive/$1");

        assertEquals("/netarchive/warcs/myharvest123.warc.gz",
                     resolver.resolveArcFileLocation("/home/harvest/warcs/myharvest123.warc.gz").getSource());
    }

    @Test
    public void mergeCollections() {
        ArcFileLocationResolverInterface resolver = createRegexpResolver(
                "(?:/home/harvest/warcs/|/otherharvest/files/)(.*)", "/netarchive/warcs/$1");

        assertEquals("/netarchive/warcs/myharvest123.warc.gz",
                     resolver.resolveArcFileLocation("/home/harvest/warcs/myharvest123.warc.gz").getSource());
        assertEquals("/netarchive/warcs/myotherharvestABC.warc.gz",
                     resolver.resolveArcFileLocation("/otherharvest/files/myotherharvestABC.warc.gz").getSource());
    }

    private ArcFileLocationResolverInterface createRegexpResolver(String regexp, String replacement) {
        FileMovedRegexpResolver resolver = new FileMovedRegexpResolver();
        Map<String, String> conf = new HashMap<>();
        conf.put(FileMovedRegexpResolver.REGEXP_KEY, regexp);
        conf.put(FileMovedRegexpResolver.REPLACEMENT_KEY, replacement);
        resolver.setParameters(conf);
        resolver.initialize();
        return resolver;
    }

}