package dk.kb.netarchivesuite.solrwayback.playback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import dk.kb.netarchivesuite.solrwayback.parsers.ParseResult;
import dk.kb.netarchivesuite.solrwayback.parsers.Jodel2Html;
import dk.kb.netarchivesuite.solrwayback.parsers.WaybackToolbarInjecter;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;

/*
 * This is for JSON harvesting from Jodel. (There is no HTML harvest for Jodel)
 */
public class JodelPlayback extends PlaybackHandler{
  
  private static final Logger log = LoggerFactory.getLogger(JodelPlayback.class);
  
  public JodelPlayback(ArcEntry arc, IndexDoc doc, boolean showToolbar){
    super(arc,doc,showToolbar);
  }

  @Override
  public ArcEntry playback() throws Exception{
    log.debug(" Generate Jodel post from FilePath:" + doc.getSource_file_path() + " offset:" + doc.getOffset());
    //Fake html into arc.

    String encoding=arc.getContentEncoding();
    String json = new String(arc.getBinary(), encoding);
    String html = Jodel2Html.render(json, arc.getCrawlDate());
    arc.setBinary(html.getBytes());        
    arc.setContentType("text/html");
    ParseResult htmlReplaced = new ParseResult(); //Do not parse.
    htmlReplaced.setReplaced(html);
    String textReplaced=htmlReplaced.getReplaced(); //TODO count linkes found, replaced
    
    //Inject tooolbar
    if (showToolbar){ //If true or null.
       textReplaced = WaybackToolbarInjecter.injectWaybacktoolBar(doc,htmlReplaced, false);
    }
    encoding="UTF-8"; // hack, since the HTML was generated as UTF-8.
    arc.setContentEncoding(encoding);
    arc.setBinary(textReplaced.getBytes(encoding));  //can give error. uses UTF-8 (from index) instead of ISO-8859-1
    
    return arc;
  }

}
