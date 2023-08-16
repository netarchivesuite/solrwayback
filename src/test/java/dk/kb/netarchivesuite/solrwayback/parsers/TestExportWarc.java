package dk.kb.netarchivesuite.solrwayback.parsers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.interfaces.ArcSource;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.dto.SearchResult;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;

public class TestExportWarc {

  public static void main (String[] args) throws Exception{

    PropertiesLoader.initProperties(UnitTestUtils.getFile("properties/solrwayback_unittest.properties").getPath());
    SearchResult search = NetarchiveSolrClient.getInstance().search("hash:\"sha1:PROTE66RZ6GDXPZI3ZAHG6YPCXRKZMEN\"", 100000);
    // /netarkiv/0105/filedir/272829-30-20170318193124175-00168-sb-prod-har-001.statsbiblioteket.dk.warc.gz
    
    //SearchResult search = SolrClient.getInstance().search("source_file:\"273055-254-20170317180213693-00000-sb-prod-har-004.statsbiblioteket.dk.warc.gz\"", 100000);
    List<IndexDoc> docs = search.getResults();

    
    
    Path exportPath = Paths.get("billede_export.warc");
    
    try{
    Files.delete(exportPath);
    }
    catch(Exception e){
      
    }
    Files.createFile(exportPath);
    
    try {
    for (IndexDoc doc : docs){
      String source_file_path = doc.getSource_file_path();
      long offset = doc.getOffset();
      if (source_file_path.toLowerCase().endsWith(".arc")  || source_file_path.toLowerCase().endsWith(".arc.gz")){
        System.out.println("skipping arc record:"+source_file_path);
        continue;
      }
      System.out.println(source_file_path);
      System.out.println(offset);
      ArcEntry warcEntry = WarcParser.getWarcEntry(ArcSource.fromFile(source_file_path), offset);
      String warc2HeaderEncoding = warcEntry.getContentEncoding();
      Charset charset = Charset.forName(WarcParser.WARC_HEADER_ENCODING); //Default if none define or illegal charset
 
      if (warc2HeaderEncoding != null){
        try{
            charset = Charset.forName(warc2HeaderEncoding);
        }
        catch (Exception e){
          if (!"binary".equals(warc2HeaderEncoding)){ //This is not a real encoding
             System.out.println("unknown charset:"+warc2HeaderEncoding);
           }
          }
      }
      
      Files.write(exportPath, warcEntry.getHeader().getBytes(charset), StandardOpenOption.APPEND);           
      System.out.println(warcEntry.getBinaryDecodedBytes().length);
      Files.write(exportPath, warcEntry.getBinaryDecodedBytes(), StandardOpenOption.APPEND);
      Files.write(exportPath, "\r\n\r\n".getBytes(WarcParser.WARC_HEADER_ENCODING), StandardOpenOption.APPEND); // separator
      
    }
  
  
    
    //dd if=netarkiv/0105/filedir/272258-267-20170313221448280-00003-kb-prod-har-003.kb.dk.warc.gz bs=1 skip=60927160 | gunzip | less
    }catch (IOException e) {
      e.printStackTrace();
  }
    
  
  }
  
}
