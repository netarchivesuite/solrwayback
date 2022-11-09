package dk.kb.netarchivesuite.solrwayback.playback;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;

public abstract class PlaybackHandler {
  
  protected ArcEntry arc;
  protected IndexDoc doc;
  protected boolean showToolbar;
  
  public PlaybackHandler(ArcEntry arc,IndexDoc doc, boolean showToolbar){    
    this.arc=arc;
    this.doc=doc;
    this.showToolbar=showToolbar;
  }

  /**
   * Deliver a webpage for playback.
   * @param lenient if true, lenient resource URL resolving is used.
   *                If false, only {@code url_norm:"normURL"} is used.
   * @return a webpage for playback.
   * @throws Exception if the webpage could not be rendered.
   */
  public abstract ArcEntry playback(boolean lenient) throws Exception;
  
}
