package dk.kb.netarchivesuite.solrwayback.util;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class CollectionUtilsTest {

    @Test
    public void interleave() {
        List<String> interleaved = CollectionUtils.interleave(
                Stream.of("a", "b", "c", "d", "e"), 
                Stream.of("1", "2", "3")).collect(Collectors.toList());
        assertEquals("Interleaving with default ratios should yield the expected result",
                     "[a, 1, b, 2, c, 3, d, e]", interleaved.toString());
    }

    @Test
    public void interleaveRatio() {
        List<String> interleaved = CollectionUtils.interleave(
                Arrays.asList(
                        Stream.of("a", "b", "c", "d", "e"),
                        Stream.of("1", "2", "3")
                ),
                Arrays.asList(2, 1)
        ).collect(Collectors.toList());
        assertEquals("Interleaving with default ratios should yield the expected result",
                     "[a, b, 1, c, d, 2, e, 3]", interleaved.toString());
    }
}