package dk.kb.netarchivesuite.solrwayback.playback;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.parsers.HtmlParserUrlRewriter;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;

public class CssPlayback  extends PlaybackHandler{
  
  private static final Logger log = LoggerFactory.getLogger(CssPlayback.class);
  
  public CssPlayback(ArcEntry arc, IndexDoc doc, boolean showToolbar){
    super(arc,doc,showToolbar);
  }

  @Override
  public ArcEntry playback() throws Exception{    
    //Never show the toolbar.
      arc.setBinary(IOUtils.toByteArray(arc.getBinaryContentAsStringUnCompressed())); //TODO charset;
      
    String textReplaced = HtmlParserUrlRewriter.replaceLinksCss(arc);                
    if (!"gzip".equalsIgnoreCase(arc.getContentEncoding())){ //TODO x-gzip brotli
      arc.setBinary(textReplaced.getBytes(arc.getContentCharset()));
      }
      else{
       arc.setBinary(textReplaced.getBytes("UTF-8"));  
      }    
    return arc;
  }
  
}