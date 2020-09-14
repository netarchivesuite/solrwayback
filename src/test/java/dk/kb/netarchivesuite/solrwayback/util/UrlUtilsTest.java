package dk.kb.netarchivesuite.solrwayback.util;

import static org.junit.Assert.*;

import org.junit.Test;

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
    
}
