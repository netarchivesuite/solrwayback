package dk.kb.netarchivesuite.solrwayback.phantom;

public class PhantomTest {

  
  public static void main(String[] args) throws Exception{
    ProcessBuilder pb =
        new ProcessBuilder("phantomjs", "/home/teg/workspace/solrwayback/rasterize.js", "http://belinda:9721/solrwayback/services/view?arcFilePath=/netarkiv/0204/filedir/17579-33-20070522211611-00025-sb-prod-har-004.arc&offset=4440073","test.png","1280px*2560px");
    
    System.out.println("start");
    Process start = pb.start();
    start.waitFor(); //Wait until completed
    
    System.out.println("end");
    
    
  }
  
}
