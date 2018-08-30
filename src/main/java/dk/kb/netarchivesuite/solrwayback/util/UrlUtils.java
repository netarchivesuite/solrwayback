package dk.kb.netarchivesuite.solrwayback.util;

public class UrlUtils {

  
  public static void main(String[] args){
   
    //System.out.println(isUrlWithDomain("http://Portal_gfx/KL/farvepakker/topmenu/topmenu_markering_groen_mBo.gif"));
    System.out.println(getDomainFromWebApiParameters("http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285"));
  }
  /*
   * Must start with http:// and have a domain(must have . as one of the characters)
   * 
   */
  public static boolean isUrlWithDomain(String url){
    if (!(url.startsWith("http://") || url.startsWith("https://")) ){ 
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
 
  /*
   * Url must be legal and have a domain, also remove port.
   */
  public static String getDomain(String url){    
    String[] tokens= url.split("/");
    String domainWithPort =  tokens[2];
    int portIndex = domainWithPort.indexOf(":");
    if (portIndex == -1){
      return domainWithPort;
    }
    else{
      return domainWithPort.substring(0, portIndex);      
    }    
  }
  
  //http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285
   //return kl.dk
  public static String getDomainFromWebApiParameters(String fullUrl){          
    int dataStart=fullUrl.indexOf("/web/");    
    String waybackDataObject = fullUrl.substring(dataStart+5);
    int indexFirstSlash = waybackDataObject.indexOf("/");             
    String url = waybackDataObject.substring(indexFirstSlash+1);    
    return getDomain(url);    
  }
  
  
  //http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285
  //return 20071221033234
 public static String getCrawltimeFromWebApiParameters(String fullUrl){          
   int dataStart=fullUrl.indexOf("/web/");    
   String waybackDataObject = fullUrl.substring(dataStart+5);
   int indexFirstSlash = waybackDataObject.indexOf("/");             
   String crawlTime = waybackDataObject.substring(0,indexFirstSlash);    
   return crawlTime;
   
 }
  
  /*
   * last path element wit query params as well.
   */
  public static String getResourceNameFromWebApiParameters(String url){    
    String[] tokens= url.split("/");
    return tokens[tokens.length-1];
    
  }
}

