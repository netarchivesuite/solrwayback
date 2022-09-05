package dk.kb.netarchivesuite.solrwayback.normalise;


import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * This class will delegate to the Normalisation class defined in solrWayback.properties 
 * Always use NORMAL unless you using very old versions of WARC-Indexer
 * 
 * This class needs some refactoring into an abstract class instead of the switches!
 * 
 * @author teg
 *
 */
public class Normalisation {
 
   private static final Logger log = LoggerFactory.getLogger(Normalisation.class);
   private enum NormaliseType {NORMAL,LEGACY,MINIMAL};
   
   static private NormaliseType type = NormaliseType.NORMAL;
   
    static {
        setTypeFromConfig();
    }
    
    public static void setTypeFromConfig() {
        String normaliseProperty=PropertiesLoader.URL_NORMALISER;

        if ("legacy".equalsIgnoreCase(normaliseProperty)){
            type=NormaliseType.LEGACY;
        }
        else if("minimal".equalsIgnoreCase(normaliseProperty)){
            type=NormaliseType.MINIMAL;
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
               
         case MINIMAL:
           return NormalisationMinimal.canonicaliseURL(url, true, true);                        
        }
        
        return NormalisationStandard.canonicaliseURL(url, true, true);
     
    }

    public static String canonicaliseURL(String url, boolean allowHighOrder, boolean createUnambiguous) {
        switch (type) {
        case NORMAL:
            return NormalisationStandard.canonicaliseURL(url, true, true);
        
        case LEGACY:
          return NormalisationLegacy.canonicaliseURL(url, true, true);            
               
         case MINIMAL:
           return NormalisationMinimal.canonicaliseURL(url, allowHighOrder, createUnambiguous);                         
        }
        
        return NormalisationStandard.canonicaliseURL(url, allowHighOrder, createUnambiguous); 
     }
                    
    

    public static String resolveRelative(String url, String relative, boolean normalise) throws IllegalArgumentException {        
        switch (type) {
        case NORMAL:
            return NormalisationStandard.resolveRelative(url, relative, normalise);
        
        case LEGACY:
          return NormalisationLegacy.resolveRelative(url, relative, normalise);            
               
         case MINIMAL:
           return NormalisationMinimal.resolveRelative(url, relative, normalise);                         
        }
        return NormalisationStandard.resolveRelative(url, relative, normalise);
    }

    /**
     * @return the type of normalisation that is used (stated in the properties).
     */
    public static NormaliseType getType() {
        return type;
    }
}
