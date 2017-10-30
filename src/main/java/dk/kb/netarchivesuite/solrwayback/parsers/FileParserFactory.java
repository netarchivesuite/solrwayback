package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

public class FileParserFactory {

    public static ArcEntry getArcEntry(String source_file_path, long offset) throws Exception{

        if (source_file_path == null ){           
            throw new IllegalArgumentException("source_file_path is null");        
        }

        ArcEntry arcEntry = null; 

        if (source_file_path.toLowerCase().endsWith(".warc")){
            arcEntry = WarcParser.getWarcEntry(source_file_path, offset);     
        }
        else if (source_file_path.toLowerCase().endsWith(".warc.gz")){ //Same parser
          arcEntry = WarcParser.getWarcEntry(source_file_path, offset);     
        }
                
        else if (source_file_path.toLowerCase().endsWith(".arc")){
            arcEntry = ArcParser.getArcEntry(source_file_path, offset);
        }
        else if (source_file_path.toLowerCase().endsWith(".arc.gz")){ //Same parser
          arcEntry = ArcParser.getArcEntry(source_file_path, offset);
        }       
        
        else{
            throw new IllegalArgumentException("File not arc or warc:"+source_file_path);
        }

        return arcEntry;
    }

}
