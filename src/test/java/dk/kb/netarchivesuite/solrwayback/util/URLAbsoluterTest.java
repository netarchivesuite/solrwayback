package dk.kb.netarchivesuite.solrwayback.util;


import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation.NormaliseType;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.junit.Before;
import org.junit.Test;

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
public class URLAbsoluterTest {

    @Before
    public void setUpProperties()  throws Exception{
        // Need this to ensure that the normaliser has a known setting
        PropertiesLoader.initProperties(UnitTestUtils.getFile("properties/solrwayback.properties").getPath());
        Normalisation.setTypeFromConfig();

        // We need this so that we know what the Solr server is set to
        PropertiesLoader.WAYBACK_BASEURL = "http://localhost:0000/solrwayback/";
    }

    @Test
    public void testTemp() {
        String url ="http://www.example.com";
        Normalisation.setType(NormaliseType.NORMAL);
        String urlNorm1 = Normalisation.canonicaliseURL(url);
        System.out.println(Normalisation.getType());
        System.out.println(urlNorm1);
        Normalisation.setType(NormaliseType.LEGACY);
        String urlNorm2 = Normalisation.canonicaliseURL(url);
        System.out.println(Normalisation.getType());
        System.out.println(urlNorm2);
        
    }

    
    
    @Test
    public void testSimple() {
        URLAbsoluter absoluter = new URLAbsoluter("http://example.com", true);
        assertEquals("http://example.com/foo", absoluter.apply("https://Example.com/FOO"));
    }

    @Test
    public void testBackslashUnicode() {
        URLAbsoluter absoluter = new URLAbsoluter("http://example.com/somefolder/", true);
        // Normalisarion legacy mode expected here
        assertEquals("Unicode escapes should be kept",
                     "http://example.com/j0%5cu00253d&_nc_ohc=pnymjb_o1",
                     absoluter.apply("https://example.com/j0\\u00253D&_nc_ohc=PNyMjb_o1"));
    }
}