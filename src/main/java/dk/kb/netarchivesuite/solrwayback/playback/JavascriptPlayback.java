package dk.kb.netarchivesuite.solrwayback.playback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.netarchivesuite.solrwayback.parsers.HtmlParserUrlRewriter;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;

public class JavascriptPlayback  extends PlaybackHandler{
  
  private static final Logger log = LoggerFactory.getLogger(CssPlayback.class);
  
  public JavascriptPlayback(ArcEntry arc, IndexDoc doc, boolean showToolbar){
    super(arc,doc,showToolbar);
  }

  // TODO: Enable propagation of lenient through HtmlParserUrlRewriter.replaceLinksCss
  @Override
  public ArcEntry playback(boolean lenient) throws Exception{
    //Never show the toolbar.
    // TODO: What was the purpose of this round trip? If re-enabled, please state why in a comment
      //arc.setBinary(IOUtils.toByteArray(arc.getStringContentAsStringSafe())); //TODO charset;
      //log.debug("javascript playback");
      
      
    String textReplaced = HtmlParserUrlRewriter.replaceLinksCss(arc);
    // TODO: This logic was wrong. Content Encoding states compression and is independent of Content Charset
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Encoding
    arc.setStringContent(textReplaced);
    return arc;
  }
  
}