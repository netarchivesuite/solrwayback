package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

public class ArcFileParserFactory {

  /*  
   * Do not call this call. This class is called from ArcParseFileResolver and will use file-mapping first
   * 
   * @param file_path is the file location, the file location must be resolved first. 
   * @param offset offset in the warc file
   * @param loadBinary will load the byte[] with the content. Do mot use for video/audio etc. Use the InputStream method for this
   */  
    public static ArcEntry getArcEntry(String file_path, long offset, boolean loadBinary) throws Exception{
        
        if (file_path == null ){           
            throw new IllegalArgumentException("file_path is null");        
        }

        ArcEntry arcEntry = null; 
       String fileLowerCase=file_path.toLowerCase(); 
        
        
        if (fileLowerCase.endsWith(".warc")  || fileLowerCase.endsWith(".warc.gz") ) {
            arcEntry = WarcParser.getWarcEntry(file_path, offset, loadBinary);     
        }
                        
        else if (fileLowerCase.endsWith(".arc") || fileLowerCase.endsWith("arc.gz")){
            arcEntry = ArcParser.getArcEntry(file_path, offset,loadBinary);                     
        }
        else{
            throw new IllegalArgumentException("File not arc or warc:"+file_path);
        }

        return arcEntry;
    }

}
