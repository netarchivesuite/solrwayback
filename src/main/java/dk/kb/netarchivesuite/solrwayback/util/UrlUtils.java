package dk.kb.netarchivesuite.solrwayback.util;

public class UrlUtils {

  
  public static void main(String[] args){
   
    //System.out.println(isUrlWithDomain("http://Portal_gfx/KL/farvepakker/topmenu/topmenu_markering_groen_mBo.gif"));
    System.out.println(getDomainFromWebApiParameters("http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285"));
  }
  /*
   * Must start with http:// and have a domain(must have .)
   * 
   */
  public static boolean isUrlWithDomain(String url){
    if (!url.startsWith("http://")){ 
     return false;
    }
    String[] tokens= url.split("/");
    if (tokens.length < 3){ 
      return false;
    }
    String domain = tokens[2];    
    if (domain.indexOf(".")<0){
      return false;
    }
    return true;        
  }
 
  public static String getDomain(String url){    
    String[] tokens= url.split("/");
    return tokens[2];
    
  }
  //http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285
   //return kl.dk
  public static String getDomainFromWebApiParameters(String fullUrl){          
    int dataStart=fullUrl.indexOf("/web/");    
    String waybackDataObject = fullUrl.substring(dataStart+5);
    int indexFirstSlash = waybackDataObject.indexOf("/");             
    String waybackDate = waybackDataObject.substring(0,indexFirstSlash);
    String url = waybackDataObject.substring(indexFirstSlash+1);
    
    return getDomain(url);
    
  }
  
  /*
   * last paths
   */
  public static String getResourcename(String url){    
    String[] tokens= url.split("/");
    return tokens[tokens.length-1];
    
  }
}

