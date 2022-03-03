package dk.kb.netarchivesuite.solrwayback.parsers;

import dk.kb.netarchivesuite.solrwayback.interfaces.ArcSource;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

import java.util.Locale;

public class ArcFileParserFactory {

  /*  
   * Do not call this call. This class is called from ArcParseFileResolver and will use file-mapping first
   * 
   * @param file_path is the file location, the file location must be resolved first. 
   * @param offset offset in the warc file
   * @param loadBinary will load the byte[] with the content. Do mot use for video/audio etc. Use the InputStream method for this
   */  
    public static ArcEntry getArcEntry(ArcSource arcSource, long offset, boolean loadBinary) throws Exception{
        
        if (arcSource == null ){
            throw new IllegalArgumentException("No arcSupplier provided");
        }

        ArcEntry arcEntry = null; 
        String sourceLowercase = arcSource.getSource().toLowerCase(Locale.ROOT);


        if (sourceLowercase.endsWith(".warc")  || sourceLowercase.endsWith(".warc.gz") ) {
            arcEntry = WarcParser.getWarcEntry(arcSource, offset, loadBinary);
        }
                        
        else if (sourceLowercase.endsWith(".arc") || sourceLowercase.endsWith("arc.gz")){
            arcEntry = ArcParser.getArcEntry(arcSource, offset,loadBinary);
        }
        else{
            throw new IllegalArgumentException(
                    "Expected (W)ARC source not arc or warc: '"+ arcSource.getSource() + "'");
        }

        return arcEntry;
    }

}
