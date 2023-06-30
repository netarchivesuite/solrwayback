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

import dk.kb.netarchivesuite.solrwayback.util.FileUtil;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoFileResolverTest {
    private static final String KNOWN_ARC_WITH_PATH = "example_arc/IAH-20080430204825-00000-blackbook.arc";

    private static final String ARC =     "IAH-20080430204825-00000-blackbook.arc";
    private static final String ARC_GZ =  "IAH-20080430204825-00000-blackbook.arc.gz";
    private static final String WARC =    "IAH-20080430204825-00000-blackbook.warc";
    private static final String WARC_GZ = "IAH-20080430204825-00000-blackbook.warc.gz";
    private static final List<String> ALL = Arrays.asList(ARC, ARC_GZ, WARC, WARC_GZ);

    @Test
    public void staticScan() throws IOException {
        // The test/resources folder in the solrwayback checkout
        Path root = FileUtil.resolve(KNOWN_ARC_WITH_PATH).getParent().getParent();
        Map<String, String> config = new HashMap<>();
        config.put(AutoFileResolver.ROOTS_KEY, root.toString());
        AutoFileResolver resolver = new AutoFileResolver();
        resolver.setParameters(config);
        resolver.initialize();

        for (String warc: ALL) {
            Path warcPath = Paths.get(resolver.resolveArcFileLocation(warc).getSource());
            assertNotNull("There should be a path for (W)ARC '" + warc + "'", warcPath);
            assertTrue("The path '" + warcPath + "' for (W)ARC '" + warc + "' should exist", Files.exists(warcPath));
        }
    }

    @Test
    public void testRescan() throws IOException, InterruptedException {
        File tmpdirF = Files.createTempDirectory("autoresolver_").toFile();
        tmpdirF.deleteOnExit();
        Path tmpdir = tmpdirF.toPath();

        // Create 4 (W)ARCs, one of them in a subfolder
        Files.write(tmpdir.resolve("foo.warc"), "moo".getBytes(StandardCharsets.UTF_8));
        Files.write(tmpdir.resolve("bar.warc.gz"), "moo".getBytes(StandardCharsets.UTF_8));
        Files.createDirectory(tmpdir.resolve("subfolder"));
        Files.write(tmpdir.resolve("subfolder").resolve("zoo.arc"), "moo".getBytes(StandardCharsets.UTF_8));
        Files.write(tmpdir.resolve("baz.arc.gz"), "moo".getBytes(StandardCharsets.UTF_8));
        List<String> warcs = Arrays.asList("foo.warc", "bar.warc.gz", "zoo.arc", "baz.arc.gz");

        // Create an auto resolver
        Map<String, String> config = new HashMap<>();
        config.put(AutoFileResolver.ROOTS_KEY, tmpdir.toString());
        config.put(AutoFileResolver.RESCAN_ENABLED_KEY, "true");
        config.put(AutoFileResolver.RESCAN_SECONDS_KEY, "1");
        AutoFileResolver resolver = new AutoFileResolver();
        resolver.setParameters(config);
        resolver.initialize();

        // Verify existence
        for (String warc: warcs) {
            assertNotEquals("The (W)ARC '" + warc + "' should be resolvable",
                            warc, resolver.resolveArcFileLocation(warc).getSource());
        }
        String newWarc = "new.warc";
        assertEquals("The (W)ARC '" + newWarc + "' should not be resolvable",
                        newWarc, resolver.resolveArcFileLocation(newWarc).getSource());

        // Create new WARC and wait a bit
        Files.write(tmpdir.resolve(newWarc), "moo".getBytes(StandardCharsets.UTF_8));
        Thread.sleep(2000); // 1 second for timeout, 1 for scan time (to be sure)

        // Check that the new WARC can be resolved
        assertNotEquals("The (W)ARC '" + newWarc + "' should be resolvable after creation",
                        newWarc, resolver.resolveArcFileLocation(newWarc).getSource());
    }
}