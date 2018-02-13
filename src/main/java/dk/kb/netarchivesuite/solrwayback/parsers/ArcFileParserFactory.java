package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

public class ArcFileParserFactory {

  /*  
   * @param file_path is the file location, the file location must be resolved first. 
   * @param offset offset in the warc file
   * 
   */  
    public static ArcEntry getArcEntry(String file_path, long offset) throws Exception{

        if (file_path == null ){           
            throw new IllegalArgumentException("file_path is null");        
        }

        ArcEntry arcEntry = null; 

        if (file_path.toLowerCase().endsWith(".warc")){
            arcEntry = WarcParser.getWarcEntry(file_path, offset);     
        }
        else if (file_path.toLowerCase().endsWith(".warc.gz")){ //Same parser
          arcEntry = WarcParser.getWarcEntry(file_path, offset);     
        }
                
        else if (file_path.toLowerCase().endsWith(".arc")){
            arcEntry = ArcParser.getArcEntry(file_path, offset);
        }
        else if (file_path.toLowerCase().endsWith(".arc.gz")){ //Same parser
          arcEntry = ArcParser.getArcEntry(file_path, offset);
        }       
        
        else{
            throw new IllegalArgumentException("File not arc or warc:"+file_path);
        }

        return arcEntry;
    }

}
