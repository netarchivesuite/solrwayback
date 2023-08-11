package dk.kb.netarchivesuite.solrwayback.interfaces;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

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
public class ArcSourceTest {

    
    /*
     * These tests depend on external resource and one of them is not on net anymore.
     * The two test has been changed into a manual integration test.
     * 
     */
    public static void main(String[] args) {
      try {
        testHTTPSURL();
        testHTTSURL();
        
      }
      catch(Exception e) {
          System.out.println("Integration test error for testHTTPSURL() or testHTTSURL()");
          e.printStackTrace();
          
      }
      
        
    }
    
    
    
    
    @Test
    public void testPath() throws IOException {
        String known = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml")).getPath();

        ArcSource source = ArcSource.create(known);
        String content = IOUtils.toString(source.get(), StandardCharsets.UTF_8);

        assertTrue("Getting file system content from '" + known + " should work", content.startsWith("<?xml"));
    }

    @Test
    public void testFileURL() throws IOException {
        String known = "file://" +
                Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml")).getPath();

        ArcSource source = ArcSource.create(known);
        String content = IOUtils.toString(source.get(), StandardCharsets.UTF_8);

        assertTrue("Getting file system content from '" + known + " should work", content.startsWith("<?xml"));
    }


    public static void testHTTSURL() throws IOException {
        String known = "http://bash.org/";

        ArcSource source = ArcSource.create(known);
        String content = IOUtils.toString(source.get(), StandardCharsets.UTF_8);

        assertTrue("Getting file system content from '" + known + " should work", content.contains("<html"));
    }


    public static void testHTTPSURL() throws IOException {
        String known = "https://www.kb.dk/";

        ArcSource source = ArcSource.create(known);
        String content = IOUtils.toString(source.get(), StandardCharsets.UTF_8);

        assertTrue("Getting file system content from '" + known + " should work", content.contains("<html"));
    }

}