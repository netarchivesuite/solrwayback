package dk.kb.netarchivesuite.solrwayback.smurf;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.solr.SolrClient;

public class NetarchiveYearCountCache {
  
  private static long lastReloadTime=0;
  private static long reloadInterval=4*60*60*1000L; //reload cache every 4 hours
  private static HashMap<Integer, Long> yearFacetsAll = null;
  private static final Logger log = LoggerFactory.getLogger(NetarchiveYearCountCache.class);
    
  private static void reload() throws Exception{    
    log.info("Reloading netarchive year count cache");
    lastReloadTime=System.currentTimeMillis();           
    HashMap<Integer, Long> yearFacetsAllTemp = SolrClient.getInstance().getYearFacetsHtmlAll();   
    yearFacetsAll=yearFacetsAllTemp;        
  }
  
  public static HashMap<Integer, Long> getYearFacetsAllQuery() throws Exception{
    if (  (System.currentTimeMillis() - reloadInterval) > lastReloadTime){
      reload();
    }       
    return yearFacetsAll;
  }
      
}
