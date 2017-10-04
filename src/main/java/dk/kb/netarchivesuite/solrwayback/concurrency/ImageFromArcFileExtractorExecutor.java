package dk.kb.netarchivesuite.solrwayback.concurrency;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;

public class ImageFromArcFileExtractorExecutor {
    
    static ExecutorService executorService = Executors.newFixedThreadPool(20); // 20 solr calls at a time 
   
    private static final Logger log = LoggerFactory.getLogger(ImageFromArcFileExtractorExecutor.class);
    
    public static  ArrayList<ArcEntryDescriptor> extractImages(List<IndexDoc> docs)  throws Exception{
   
       
       Set<Callable<ArrayList<ArcEntryDescriptor>>> callables = new HashSet<>();
              
       for (final IndexDoc current : docs){
       
           callables.add(new Callable<ArrayList<ArcEntryDescriptor>>() {
           @Override
           public ArrayList<ArcEntryDescriptor> call() throws Exception {
                             
                   if ("html".equals(current.getContentTypeNorm())){                           
                   log.info("getting images from:"+current.getUrl_norm());
                     ArrayList<ArcEntryDescriptor> images = Facade.getImagesForHtmlPageNewThreaded(current.getArc_full(),current.getOffset());
                       return images;                       
                   }
                   else if ("image".equals(current.getContentTypeNorm())){                        
                       String arcFull = current.getArc_full();
                       ArcEntryDescriptor desc= new ArcEntryDescriptor();
                       desc.setArcFull(arcFull);
                       desc.setHash(current.getHash());
                       desc.setOffset(current.getOffset());                       
                       ArrayList<ArcEntryDescriptor> single = new ArrayList<> ();
                       single.add(desc);
                       return single;
                   }
                   else{
                       return new ArrayList<>(); //Empty, but the callable must return it
                   }                                                                   
           }
       });      
            
       }
       
       //start all the executes.
       List<Future<ArrayList<ArcEntryDescriptor>>> futures = executorService.invokeAll(callables);

       // Extract all results and sort then by weight
       List<ArcEntryDescriptor> allList = new ArrayList<>();
       for(Future<ArrayList<ArcEntryDescriptor>> future : futures) {
        try{
           ArrayList<ArcEntryDescriptor> hits = future.get();
           if (hits != null){
             allList.addAll(hits);
           }
           else{
               log.error("Error invoking async future Solr call... Check Solr log");
           }
        }
        catch(Exception e){
            log.error("error getting future...");
        }
           
       }

       // Remove duplicates (there will be many)
       //Add all the ArcEntryDescriptor to a single list, and only once for each hash. So we add them to a hashset
       HashSet<ArcEntryDescriptor> allAddedSet = new LinkedHashSet<>(allList);

       // Convert back to list for delivery
       return new ArrayList<>(allAddedSet);
       
       //executorService.shutdown(); // No need to shut down. It is emptied and reused
    }

}
