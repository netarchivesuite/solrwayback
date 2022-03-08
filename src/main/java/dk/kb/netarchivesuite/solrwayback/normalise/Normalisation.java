package dk.kb.netarchivesuite.solrwayback.normalise;



import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * This class will delegate to the Normalisation class defined in solrWayback.properties
 * 
 * @author teg
 *
 */
public class Normalisation {
 
   private static final Logger log = LoggerFactory.getLogger(Normalisation.class);
   private enum NormaliseType {NORMAL,LEGACY,HERITRIX};
   
   static private NormaliseType type = NormaliseType.NORMAL;
   
    static {
       String normaliseProperty=PropertiesLoader.URL_NORMALISE;
       if ("legacy".equalsIgnoreCase(normaliseProperty)){
           type=NormaliseType.LEGACY;
       }
       else if("heritrix".equalsIgnoreCase(normaliseProperty)){
           type=NormaliseType.HERITRIX;
       } 
       else {
           type = NormaliseType.NORMAL;           
       }
       log.info("URL normalise will use type:"+type);
    }
    
    
    public static String canonicaliseURL(String url) {        

        switch (type) {
        case NORMAL:
            return NormalisationStandard.canonicaliseURL(url, true, true);
        
        case LEGACY:
          return NormalisationLegacy.canonicaliseURL(url, true, true);            
               
         case HERITRIX:
           return NormalisationLegacy.canonicaliseURL(url, true, true);                        
        }
        
        return NormalisationStandard.canonicaliseURL(url, true, true);
     
    }

    public static String canonicaliseURL(String url, boolean allowHighOrder, boolean createUnambiguous) {
        return NormalisationStandard.canonicaliseURL(url, allowHighOrder, createUnambiguous);               
    }

    public static String resolveRelative(String url, String relative, boolean normalise) throws IllegalArgumentException {        
        return NormalisationStandard.resolveRelative(url, relative, normalise);
    }
}
