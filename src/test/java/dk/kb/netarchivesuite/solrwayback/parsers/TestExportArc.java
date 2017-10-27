package dk.kb.netarchivesuite.solrwayback.parsers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.solr.SolrClient;

public class TestExportArc {

  public static void main (String[] args) throws Exception{

    PropertiesLoader.initProperties();
      
    
    String arcFile="/netarkiv/0205/filedir/27120-33-20080401191241-00000-sb-prod-har-001.statsbiblioteket.dk.arc.gz";
    long offset=18475474;
    

    
    ArcEntry arcEntry = ArcParser.getArcEntry(arcFile, offset);
    
    String warcHeader = ArcHeader2WarcHeader.arcHeader2WarcHeader(arcEntry);
    
    
    Path exportPath = Paths.get("arc2warc-error.warc");
    
    try{
    Files.delete(exportPath);
    }
    catch(Exception e){
      
    }
    Files.createFile(exportPath);
    System.out.println(arcEntry.getHeader()); // 37870 is corrent
    System.out.println("-----");
    System.out.println(warcHeader);
    
    Files.write(exportPath, warcHeader.getBytes(WarcParser.WARC_HEADER_ENCODING), StandardOpenOption.APPEND);           
    Files.write(exportPath, arcEntry.getBinary(), StandardOpenOption.APPEND);
    Files.write(exportPath, "\r\n\r\n".getBytes(WarcParser.WARC_HEADER_ENCODING), StandardOpenOption.APPEND); // separator
    
  }
  
}
