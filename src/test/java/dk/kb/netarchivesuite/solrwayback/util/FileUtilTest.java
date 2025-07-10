package dk.kb.netarchivesuite.solrwayback.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
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
public class FileUtilTest {
    private static final Logger log = LoggerFactory.getLogger(FileUtilTest.class);

    @Test
    public void testGetContainerCandidates() {
        List<Path> candidates = FileUtil.getContainerCandidates("foo").collect(Collectors.toList());
        log.debug("Got candidates {}", candidates);

        assertFalse("There should be some candidates", candidates.isEmpty());

        Path expected = Paths.get(System.getProperty("user.home"), "foo");
        assertTrue("The known path '" + expected + "' should be present in candidate list " + candidates,
                candidates.contains(expected));
    }

    @Test
    public void testResolveContainerResource() throws FileNotFoundException {
        // We expect the current folder to be the project root and we know that the file pom.xml is there
        Path pom = FileUtil.resolveContainerResource("pom.xml"); // Throws exception if not found
        log.debug("Found pom: '" + pom + "'");
    }
}