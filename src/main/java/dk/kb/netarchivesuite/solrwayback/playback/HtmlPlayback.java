package dk.kb.netarchivesuite.solrwayback.playback;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.parsers.ParseResult;
import dk.kb.netarchivesuite.solrwayback.parsers.HtmlParserUrlRewriter;
import dk.kb.netarchivesuite.solrwayback.parsers.WaybackToolbarInjecter;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;

public class HtmlPlayback  extends PlaybackHandler{
  
  private static final Logger log = LoggerFactory.getLogger(HtmlPlayback.class);
  
  public HtmlPlayback(ArcEntry arc, IndexDoc doc, boolean showToolbar){
    super(arc,doc,showToolbar);
  }

  @Override
  public ArcEntry playback() throws Exception{    
    log.debug(" Generate webpage from FilePath:" + doc.getSource_file_path() + " offset:" + doc.getOffset() +" content encoding:"+arc.getContentEncoding());
    long start = System.currentTimeMillis();
    
     String raw = arc.getBinaryContentAsStringUnCompressed();
    
      String charset = arc.getContentCharset();
      if (charset== null){
          charset="UTF-8";
          log.warn("no charset, default to UTF-8");
      }
      
      arc.setBinary(raw.getBytes(Charset.forName(charset)));
         
    
     ParseResult htmlReplaced = HtmlParserUrlRewriter.replaceLinks(arc);
      String textReplaced=htmlReplaced.getReplaced();

      boolean xhtml =doc.getContentType().toLowerCase().indexOf("application/xhtml") > -1;            
    //Inject tooolbar
     if (showToolbar ){ //If true or null. 
        textReplaced = WaybackToolbarInjecter.injectWaybacktoolBar(doc.getSource_file_path(),doc.getOffset(),htmlReplaced , xhtml);
     }
    
     try{
     if (!"gzip".equalsIgnoreCase(arc.getContentEncoding())){ //TODO x-gzip brotli
       arc.setBinary(textReplaced.getBytes(arc.getContentCharset()));
       }
       else{
        arc.setBinary(textReplaced.getBytes("UTF-8"));  
       }
      
     }
     catch(Exception e){       
       log.warn("unknown encoding, defaulting to utf-8:'"+arc.getContentEncoding()+"' . file:"+doc.getSource_file_path() +" offset:"+doc.getOffset());
       arc.setBinary(textReplaced.getBytes("UTF-8"));
     }

     log.info("Generating webpage total processing:"+(System.currentTimeMillis()-start) + " "+doc.getSource_file_path()+ " "+ doc.getOffset() +" "+arc.getUrl());
     arc.setHasBeenDecompressed(true);
     return arc;
  }
  
}