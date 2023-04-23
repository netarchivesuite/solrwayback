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
  public ArcEntry playback(boolean lenient) throws Exception{
    log.debug(" Generate webpage from FilePath:{} offset:{} content encoding:{} lenient:{}",
              doc.getSource_file_path(), doc.getOffset(), arc.getContentEncoding(), lenient);
    long start = System.currentTimeMillis();
    

     ParseResult htmlReplaced = HtmlParserUrlRewriter.replaceLinks(arc, lenient);
      String textReplaced=htmlReplaced.getReplaced();

      boolean xhtml =doc.getContentType().toLowerCase().indexOf("application/xhtml") > -1;            
    //Inject tooolbar
     if (showToolbar ){ //If true or null. 
        textReplaced = WaybackToolbarInjecter.injectWaybacktoolBar(doc.getSource_file_path(),doc.getOffset(),htmlReplaced , xhtml);
     }

     arc.setStringContent(textReplaced);

     log.info("Generating webpage total processing:"+(System.currentTimeMillis()-start) + " "+doc.getSource_file_path()+ " "+ doc.getOffset() +" "+arc.getUrl());
     arc.setHasBeenDecompressed(true);
     return arc;
  }
  
}