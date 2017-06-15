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
import dk.kb.netarchivesuite.solrwayback.service.dto.WeightedArcEntryDescriptor;
import dk.kb.netarchivesuite.solrwayback.solr.SolrClient;

public class ImageFromArcFileExtractorExecutor {
    
    static ExecutorService executorService = Executors.newFixedThreadPool(20); // just hardcoded for now. increasing to 50 does not help
   
    private static final Logger log = LoggerFactory.getLogger(ImageFromArcFileExtractorExecutor.class);
    
    public static  ArrayList<? extends ArcEntryDescriptor> extractImages(List<IndexDoc> docs)  throws Exception{
   
       
       Set<Callable<ArrayList<WeightedArcEntryDescriptor>>> callables = new HashSet<>();
              
       for (final IndexDoc current : docs){
       
           callables.add(new Callable<ArrayList<WeightedArcEntryDescriptor>>() {
           @Override
           public ArrayList<WeightedArcEntryDescriptor> call() throws Exception {
                             
                   if ("html".equals(current.getContentTypeNorm())){                           
                       return Facade.getImagesFromHtmlPage(current);
                   }
                   else if ("image".equals(current.getContentTypeNorm())){ 
                       String source_file_s = current.getSource_file_s();//always only 1 due to group
                       String arcFull = current.getArc_full();
                       WeightedArcEntryDescriptor desc= new WeightedArcEntryDescriptor();
                       desc.setArcFull(arcFull);
                       desc.setSource_file_s(source_file_s);
                       desc.setHash(current.getHash());
                       desc.setOffset(SolrClient.getOffset(source_file_s));
                       desc.setWeight(current.getScore()); // We just use the score directly here
                       ArrayList<WeightedArcEntryDescriptor> single = new ArrayList<> ();
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
       List<Future<ArrayList<WeightedArcEntryDescriptor>>> futures = executorService.invokeAll(callables);

       // Extract all results and sort then by weight
       List<WeightedArcEntryDescriptor> allList = new ArrayList<>();
       for(Future<ArrayList<WeightedArcEntryDescriptor>> future : futures) {
        try{
           ArrayList<WeightedArcEntryDescriptor> hits = future.get();
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
       Collections.sort(allList);

       // Remove duplicates
       //Add all the ArcEntryDescriptor to a single list, and only once for each hash. So we add them to a hashset
       HashSet<? extends ArcEntryDescriptor> allAddedSet = new LinkedHashSet<>(allList);

       // Convert back to list for delivery
       return new ArrayList<>(allAddedSet);
       
       //executorService.shutdown(); // No need to shut down. It is emptied and reused
    }

}
