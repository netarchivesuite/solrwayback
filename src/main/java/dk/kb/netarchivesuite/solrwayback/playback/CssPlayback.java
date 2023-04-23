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

  // TODO: Enable propagation of lenient through HtmlParserUrlRewriter.replaceLinksCss(arc)
  @Override
  public ArcEntry playback(boolean lenient) throws Exception{
    //Never show the toolbar.
      // TODO: What was the purpose of this round trip? If re-enabled, please state why in a comment
    //  arc.setBinary(IOUtils.toByteArray(arc.getStringContentAsStringSafe())); //TODO charset;
      
    String textReplaced = HtmlParserUrlRewriter.replaceLinksCss(arc);
    // content-encoding is about compression; not relevant for charset
    // if (!"gzip".equalsIgnoreCase(arc.getContentEncoding())){
    arc.setStringContent(textReplaced);
    return arc;
  }
  
}