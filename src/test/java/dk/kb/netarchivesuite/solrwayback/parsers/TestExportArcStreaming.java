package dk.kb.netarchivesuite.solrwayback.parsers;

import java.io.File;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

public class TestExportArcStreaming {

  

  public static void main(String[] args) throws Exception{
    PropertiesLoader.initProperties();
    
    
    
    /*
    FileUtils.writeByteArrayToFile(new File(fileFromBytes), bytes);
    
    
    FileOutputStream fos = new FileOutputStream(fileFromBytesStream);
    fos.write(bytes);
    fos.close();
    
    InputStream is = new ByteArrayInputStream(bytes);         
    System.out.println(is.read());
    System.out.println(is.read());
    System.out.println(is.read());
    System.out.println(is.read());
    System.out.println(is.read());
    System.out.println(is.read());
    
    
    FileUtils.copyInputStreamToFile( is, new File(fileFromBytesWarcInputStream)); 
    */
    
    InputStream is1 = Facade.exportArcStreaming("source_file:\"27081-33-20080331223131-00052-sb-prod-har-001.statsbiblioteket.dk.arc.gz\"", null);
    FileUtils.copyInputStreamToFile( is1, new File("export_final.arc"));
    
    
  }
}
