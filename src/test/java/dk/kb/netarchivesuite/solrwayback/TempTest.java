package dk.kb.netarchivesuite.solrwayback;

public class TempTest {

  public static void main(String[] args){
      
   String url="https://solrwb-stage.statsbiblioteket.dk:4000/solrwayback/services/pwid/web/urn:pwid:netarkivet.dk:2018-12-10T06:27:01Z:part:https://www.petdreams.dk/katteracer-siameser";
 
   //For some reason the var regexp does not work with comma (;) and other characters. So I have to grab the full url from uriInfo
   
   int pwidStart=url.indexOf("/pwid/web/"); //urn:pwid:netarkivet.dk:2018-12-10T06:27:01Z:part:https://www.petdreams.dk/katteracer-siameser
      
   String pwid = url.substring(pwidStart+10);
   System.out.println("Pwid object:"+pwid);
   if (!(pwid.startsWith("urn:pwid:"))){
     //syntax not correct
     System.out.println("not correct");
     System.exit(1);
     }
      String collectionStart = pwid.substring(9);
      System.out.println(collectionStart);
      int collectionEnd = collectionStart.indexOf(":");  
      String thisCollectionName = "netarkivet.dk";
    
      String urlCollectionName = collectionStart.substring(0,collectionEnd);
    
    System.out.println(urlCollectionName);
      //int indexFirstSlash = waybackDataObject.indexOf("/");  
    if (!(urlCollectionName.equals(thisCollectionName))){
      System.out.println("wrong collection");
      System.exit(1);
    }
    String utcStart =  collectionStart.substring(thisCollectionName.length()); // This now equals:  :part:https://www.petdreams.dk/katteracer-siameser
    System.out.println("utcStart:"+utcStart);
    //validate first char is :
    if (!(utcStart.startsWith(":"))){
      System.out.println("Syntax not correct");
      System.exit(1);
    }
    
    utcStart = utcStart.substring(1); // now : part:https://www.petdreams.dk/katteracer-siameser
    
    
   

    int utcEnd = utcStart.indexOf(":part:");
    String onlyUTC= utcStart.substring(0,utcEnd);
    System.out.println(onlyUTC);
    
    
    String lastPartStart = utcStart.substring(utcEnd);    
        if (!(lastPartStart.startsWith(":part:"))){
      System.out.println("Syntax not correct:"+lastPartStart );
      System.exit(1);
    }
    String pwidUrl= lastPartStart.substring(6);// only the url
     System.out.println(pwidUrl);
        
        
    
    
    
  }
  
}
