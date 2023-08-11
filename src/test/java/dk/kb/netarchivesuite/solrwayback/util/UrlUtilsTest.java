package dk.kb.netarchivesuite.solrwayback.util;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UrlUtilsTest {

  
  
  @Test
  public void testDomain() {
    String url="http://teg-desktop.sb.statsbiblioteket.dk:8080/test/images/horse.png"; //with port        
    assertEquals("teg-desktop.sb.statsbiblioteket.dk",UrlUtils.getDomain(url));      
    
    url="http://teg-desktop.sb.statsbiblioteket.dk/test/images/horse.png"; //no port        
    assertEquals("teg-desktop.sb.statsbiblioteket.dk",UrlUtils.getDomain(url));
    
    url="http://teg-desktop.sb.statsbiblioteket.dk/";         
    assertEquals("teg-desktop.sb.statsbiblioteket.dk",UrlUtils.getDomain(url));
  }
  
  @Test
  public void testGetDomainFromWebApiParameters() {
    String url="http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285";        
    assertEquals("kl.dk",UrlUtils.getDomainFromWebApiParameters(url));      
  }

  @Test
  public void testGetCrawlTimeFromWebApiParameters() {
    String url="http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285";        
    assertEquals("20071221033234",UrlUtils.getCrawltimeFromWebApiParameters(url));      
  }
  
  
  @Test
  public void testGetResourcename() {
    String url="http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285";        
    assertEquals("ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285",UrlUtils.getResourceNameFromWebApiParameters(url));      
  }
  
  
  
  @Test
  public void testgetCrawltimeAndUrlFromWebProxyLeak(){
      String url1 = "https://solrwb.test.dk:4000/solrwayback/services/web/20110225142047/http://www.bt.dk/debat";
      String[] result = UrlUtils.getCrawltimeAndUrlFromWebProxyLeak(url1);
      assertEquals("20110225142047", result[0]);
      assertEquals("http://www.bt.dk/debat", result[1]);
      
      //Test if pattern is also matched in url
      String url2 = "https://solrwb.test.dk:4000/solrwayback/services/web/20110225142047/http://www.bt.dk/debat/20110225142048/test";
      
      result = UrlUtils.getCrawltimeAndUrlFromWebProxyLeak(url2);
      assertEquals("20110225142047", result[0]);
      assertEquals("http://www.bt.dk/debat/20110225142048/test", result[1]);
      
      //Null test
      String url3 = "https://solrwb.test.dk:4000/solrwayback/services/horse.jpg";
      result = UrlUtils.getCrawltimeAndUrlFromWebProxyLeak(url3);
      assertNull(result);
  }
  
  @Test
  public void testIsUrlWithDomain() {
    String url="http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285";        
    assertTrue(UrlUtils.isUrlWithDomain(url));
    
    url="https://test.dk/"; //HTTPS
    assertTrue(UrlUtils.isUrlWithDomain(url));   
    
    url="http://test.dk"; // Domain urls in the solr index will always end with a / for domains.
    assertTrue(UrlUtils.isUrlWithDomain(url));
    
    
    url="http://test/"; // Allowed, just no .
    assertTrue(UrlUtils.isUrlWithDomain(url));
    
    url="http://test/images/horse.png"; // L
    assertTrue(UrlUtils.isUrlWithDomain(url));   
    
    url="test/images/horse.png"; // only a relative url
    assertFalse(UrlUtils.isUrlWithDomain(url));    
  }

  @Test
  public void testLenientURLQueryNoArgs() {
      assertEquals("url:\"http://example.com/\"^200 OR url_norm:\"http://example.com/\"^100",
                   UrlUtils.lenientURLQuery("http://example.com/"));
      assertEquals("url:\"https://EXAMPLE.com/\"^200 OR url_norm:\"http://example.com/\"^100",
                   UrlUtils.lenientURLQuery("https://EXAMPLE.com/"));
  }

  @Test
  public void testLenientURLQueryArgs() {
      assertEquals(("url:\"http://example.com/IMAGES/search?q=horse&fq=animals&_=67890\"^200 OR " +
                   "url_norm:\"http://example.com/images/search?q=horse&fq=animals&_=67890\"^100 OR (" +
                   "host:\"example.com\" AND " +
                   "url_search:\"images/search\" AND" +
                   " (host:\"example.com\" OR" +
                   " url_search:\"q=horse\"" +
                   " OR url_search:\"fq=animals\"" +
                   " OR url_search:\"_=67890\")" +
                   ")").replace(" ", "\n"),
                   UrlUtils.lenientURLQuery("http://example.com/IMAGES/search?q=horse&fq=animals&_=67890").replace(" ", "\n"));
  }

  @Test
  public void testLenientURLQueryArgsHost() {
      assertEquals(("url:\"http://hello.example.com/IMAGES/search?q=horse\"^200 OR " +
                   "url_norm:\"http://hello.example.com/images/search?q=horse\"^100 OR (" +
                   "host:\"hello.example.com\" AND " +
                   "url_search:\"images/search\" AND" +
                   " (host:\"hello.example.com\" OR" +
                   " url_search:\"q=horse\")" +
                   ")").replace(" ", "\n"),
                   UrlUtils.lenientURLQuery("http://hello.example.com/IMAGES/search?q=horse").replace(" ", "\n"));
  }

  @Test
  public void testLenientURLQueryArgsWWW() {
      assertEquals(("url:\"https://www.example.com/IMAGES/search?q=horse\"^200 OR " +
                   "url_norm:\"http://example.com/images/search?q=horse\"^100 OR (" +
                   "host:\"example.com\" AND " +
                   "url_search:\"images/search\" AND" +
                   " (host:\"example.com\" OR" +
                   " url_search:\"q=horse\")" +
                   ")").replace(" ", "\n"),
                   UrlUtils.lenientURLQuery("https://www.example.com/IMAGES/search?q=horse").replace(" ", "\n"));
  }
}
