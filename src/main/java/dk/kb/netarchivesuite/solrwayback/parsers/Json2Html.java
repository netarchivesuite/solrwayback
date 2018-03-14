package dk.kb.netarchivesuite.solrwayback.parsers;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.json.JSONObject;

public class Json2Html {

  /**
   * Get the JSON data formated in HTML
   */ 
  public String getHtmlData( String strJsonData ) {
      return jsonToHtml( new JSONObject( strJsonData ) );
  }

  /**
   * convert json Data to structured Html text
   * 
   * @param json
   * @return string
   */
  private String jsonToHtml( Object obj ) {
      StringBuilder html = new StringBuilder( );

      try {
          if (obj instanceof JSONObject) {
              JSONObject jsonObject = (JSONObject)obj;
              String[] keys = JSONObject.getNames( jsonObject );

              html.append("<div class=\"json_object\">");

              if (keys.length > 0) {
                  for (String key : keys) {
                      // print the key and open a DIV
                      html.append("<div><span class=\"json_key\">")
                          .append(key).append("</span> : ");

                      Object val = jsonObject.get(key);
                      // recursive call
                      html.append( jsonToHtml( val ) );
                      // close the div
                      html.append("</div>");
                  }
              }

              html.append("</div>");

          } else if (obj instanceof JSONArray) {
              JSONArray array = (JSONArray)obj;
              for ( int i=0; i < array.length( ); i++) {
                  // recursive call
                  html.append( jsonToHtml( array.get(i) ) );                    
              }
          } else {
              // print the value
              html.append( obj );
          }                
      } catch (JSONException e) { return e.getLocalizedMessage( ) ; }

      return html.toString( );
  }
  
}
