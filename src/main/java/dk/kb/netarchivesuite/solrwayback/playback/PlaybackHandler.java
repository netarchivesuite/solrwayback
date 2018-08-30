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
    
  public abstract ArcEntry playback() throws Exception;
  
}
