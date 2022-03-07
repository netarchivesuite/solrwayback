package dk.kb.netarchivesuite.solrwayback.interfaces;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

/*
 * Optional FileLocationResolver interface implementation.
 *  
 * This class takes an a text file as input. 

 * To activate it, set this in solrwayback.properties:
 *   warc.file.resolver.class= FileMovedMappingResolver
 *   warc.file.resolver.filemovemappingresolver=<mapping file location>
 *   
 * The filename must be defined in the warc.file.resolver.class property in solrwayback
 * Each line is the full location of the warc-file that has a new location after indexing
 * 
 * This resolver class will be activated by the InitialContextLoader
 * 
 * The Solr index has the filename stored already, but the full path in solr can be changed since indexing.
 * If the filename is found the this list, the resolver will use this full path instead of the one stored in solr.
 * 
 */
public class FileMovedMappingResolver implements ArcFileLocationResolverInterface {
  
  private static final Logger log = LoggerFactory.getLogger(FileMovedMappingResolver.class);
  
    //For the file location: /a/b/c/test.warc
    //key: test.warc
    //value:/a/b/c  (notice the missing / in the end). This is to save memory instead of having full path as value
    private static HashMap<String,String> FILE_MAP = new HashMap<String,String>();
    String mappingFile=null;
    
    public FileMovedMappingResolver() {
            
    }    
    
    @Override
    public void setParameters(Map<String, String> parameters) {
        setMappingFile(parameters.get(PropertiesLoader.WARC_FILE_RESOLVER_UNQUALIFIED));
        log.info("Initializing file mapping parameter: " + mappingFile);
    }

    public void setMappingFile(String mappingFile) {
        this.mappingFile = mappingFile;
    }

    @Override
    public void initialize() {            
        log.info("Initialising FileMovedMappingResolver from file:"+mappingFile);
                

        //read file and parse each line 
        try (Stream<String> stream = Files.lines(Paths.get(mappingFile))) {
           stream.forEach((k) -> {
           File file = new File(k);          
           FILE_MAP.put(file.getName(), file.getParent());                              
          });
            
        } catch (IOException e) {
           log.error("Error parsing file:"+mappingFile, e);
           log.error("Failed initializing FileMovedMappingResolver");
        }         

       log.info("Initialized success. Number of moved files in map:"+FILE_MAP.size());
    }
  
      //Return the filelocation if filename is found in the mapping file. 
      //If the filename is not found in the mapping, return the input back.
    
    @Override
    public ArcSource resolveArcFileLocation(String source_file_path){
        String fileName = new File(source_file_path).getName();
        String value = FILE_MAP.get(fileName);

        String finalPath = value == null ? source_file_path : value + "/" + fileName;

        return ArcSource.fromFile(finalPath);
      }
}
