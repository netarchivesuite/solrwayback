package dk.kb.netarchivesuite.solrwayback.util;


import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation.NormaliseType;
import dk.kb.netarchivesuite.solrwayback.normalise.NormalisationStandard;
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
        PropertiesLoader.initProperties(UnitTestUtils.getFile("properties/solrwayback_unittest.properties").getPath());
        // We need this so that we know what the Solr server is set to
        PropertiesLoader.WAYBACK_BASEURL = "http://localhost:0000/solrwayback/";
    }

    
    /*
     * Notice this test uses two different url normalizers
     * The only difference is the legacy normaliser will keep the www prefix before domain, but ONLY if the whole url is domain level. (no further path)
     * 
     */
    @Test
    public void testLegacyNormaliser() {
        
        //Test legacy
        Normalisation.setType(NormaliseType.LEGACY);        
        String urlDomainOnly ="http://www.example.com"; //www will be kept and a slash added to end
        String urlDomainOnlyNorm= Normalisation.canonicaliseURL(urlDomainOnly );
        assertEquals("http://www.example.com/", urlDomainOnlyNorm);        
        String urlDomainWithPath ="http://www.example.com/index.html"; //www must be removed
        String urlDomainWithPathNorm= Normalisation.canonicaliseURL(urlDomainWithPath );                
        assertEquals("http://example.com/index.html", urlDomainWithPathNorm);
        
        //This is a test of the bug that the legacy normaliser had.
        URLAbsoluter absoluter = new URLAbsoluter("http://example.com/somefolder/", true);
        // Normalisarion legacy mode expected here
        assertEquals("Unicode escapes should be kept",
                     "http://example.com/j0%5cu00253d&_nc_ohc=pnymjb_o1",
                     absoluter.apply("https://example.com/j0\\u00253D&_nc_ohc=PNyMjb_o1"));
        
        // Repeat the test with  normal normaliser
        Normalisation.setType(NormaliseType.NORMAL);
        urlDomainOnlyNorm= Normalisation.canonicaliseURL(urlDomainOnly );
        assertEquals("http://example.com/", urlDomainOnlyNorm); //www is removed
        urlDomainWithPathNorm= Normalisation.canonicaliseURL(urlDomainWithPath );
        assertEquals("http://example.com/index.html",urlDomainWithPathNorm); //www is removed        
    }

    
    
    
    
    //Notice this is probably not the beviour we want. Warc-indexer most remove port 80
    @Test
    public void testNormal() {
         Normalisation.setType(NormaliseType.NORMAL);
         //with port 80
         String url="http://test.dk:80/TEST.html";
         String canonicaliseURL = NormalisationStandard.canonicaliseURL(url);
         assertEquals(url.toLowerCase(),canonicaliseURL);
    
         //without port
         url="http://test.dk/TEST.html";
         canonicaliseURL = NormalisationStandard.canonicaliseURL(url);
         assertEquals(url.toLowerCase(),canonicaliseURL);                  
    }

    
    
    @Test
    public void testSimple() {
        Normalisation.setType(NormaliseType.NORMAL);
        URLAbsoluter absoluter = new URLAbsoluter("http://example.com", true);
        assertEquals("http://example.com/foo", absoluter.apply("https://Example.com/FOO"));
    }

    @Test
    public void testBackslashUnicode() {
        Normalisation.setType(NormaliseType.NORMAL);
        URLAbsoluter absoluter = new URLAbsoluter("http://example.com/somefolder/", true);
        assertEquals("Unicode escapes should be kept",
                     "http://example.com/j0%5cu00253d&_nc_ohc=pnymjb_o1",
                     absoluter.apply("https://example.com/j0\\u00253D&_nc_ohc=PNyMjb_o1"));
    }
}