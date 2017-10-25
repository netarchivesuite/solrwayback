package dk.kb.netarchivesuite.solrwayback.parsers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FileUtils;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;

public class TestExportWarcStreaming {

  

  public static void main(String[] args) throws Exception{
    PropertiesLoader.initProperties();
    String source_file_path="/home/teg/workspace/solrwayback/storedanske_export-00000.warc";
    int offset = 515818793;
    ArcEntry warcEntry = WarcParser.getWarcEntry(source_file_path,offset);
    
    byte[] bytes = warcEntry.getBinary(); // <--------- The binary
    String fileFromBytes = "image1.jpg";
    String fileFromBytesStream = "image2.jpg";
    String fileFromBytesWarcInputStream = "image3.jpg";
    String fileFromBytesWarcInputStream2 = "image4.jpg";
    
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
    
    InputStream is1 = Facade.exportWarcStreaming("hash:\"sha1:PROTE66RZ6GDXPZI3ZAHG6YPCXRKZMEN\"", null);
    FileUtils.copyInputStreamToFile( is1, new File("export_final.warc"));
    
    
  }
  
}
