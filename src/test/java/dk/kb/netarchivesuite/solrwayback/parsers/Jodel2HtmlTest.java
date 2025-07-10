package dk.kb.netarchivesuite.solrwayback.parsers;

import java.io.File;

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
public class Jodel2HtmlTest {

    private final File testFolder = new File(System.getProperty("java.io.tmpdir"), "jodeltest");
/*
 * Uncommented until jodel parser rewritten using new util class.
 
    @Test
    public void testManualInspection() throws IOException {
    	
        String json = UnitTestUtils.loadUTF8("example_jodel/jodel.json");
        String html = Jodel2Html.render(json, "2018-03-16T11:04:00Z");

        // TODO: Make proper test for the returned HTML
        System.out.println(html);
        if (!testFolder.exists()) {
            assertTrue("The test folder " + testFolder + " should be available", testFolder.mkdirs());
        }
        UnitTestUtils.saveUTF8(html, new File(testFolder, "index.html"));

        File cssFolder = new File(testFolder, "css");
        if (!cssFolder.exists()) {
            assertTrue("The css folder should be available", cssFolder.mkdirs());
        }
        try {
            Files.copy(UnitTestUtils.getFile("css/jodel.css").toPath(),
                       new File(cssFolder, "jodel.css").toPath(),
                       REPLACE_EXISTING);
        } catch (IOException e) {
            // TODO: How do we get access to webapp/css/jodel.css from test?
        }
        System.out.println("Output available as file://" + testFolder + "/index.html");
    }
*/
}