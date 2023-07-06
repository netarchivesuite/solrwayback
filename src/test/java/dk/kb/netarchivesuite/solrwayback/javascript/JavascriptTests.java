package dk.kb.netarchivesuite.solrwayback.javascript;


import java.util.ArrayList;
import java.util.Collection;


import javax.script.*;

//This is dirty import. Fix when project runs on java 15+
import jdk.nashorn.api.scripting.JSObject;

import org.junit.Test;


import dk.kb.netarchivesuite.solrwayback.util.FileUtil;

/**
 *  Unittest method to call the query parser defined in the VUE typescript. The method has been copied to a new file and is now pure javascript. 
 *  Remember to also make the changes in the checkQueryForBadSyntax.js when adding futher functionality and unittest 
 *  
 *  Note that for JAVA15+ this will not work since Nashorn is removed from the JDK, but can be added with a maven dependency 
 * 
 */

public class JavascriptTests {

    //The following two string will patch the java nashorn (ECMA version 5)  engine to support the includes function on string and arrays (introduced in EMCA version 6)     
    // Copied from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/includes#Polyfill
    public static final String NASHORN_POLYFILL_STRING_PROTOTYPE_INCLUDES = "if (!String.prototype.includes) { Object.defineProperty(String.prototype, 'includes', { value: function(search, start) { if (typeof start !== 'number') { start = 0 } if (start + search.length > this.length) { return false } else { return this.indexOf(search, start) !== -1 } } }) }";
    // Copied from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/includes#Polyfill
    public static final String NASHORN_POLYFILL_ARRAY_PROTOTYPE_INCLUDES  = "if (!Array.prototype.includes) { Object.defineProperty(Array.prototype, 'includes', { value: function(valueToFind, fromIndex) { if (this == null) { throw new TypeError('\"this\" is null or not defined'); } var o = Object(this); var len = o.length >>> 0; if (len === 0) { return false; } var n = fromIndex | 0; var k = Math.max(n >= 0 ? n : len - Math.abs(n), 0); function sameValueZero(x, y) { return x === y || (typeof x === 'number' && typeof y === 'number' && isNaN(x) && isNaN(y)); } while (k < len) { if (sameValueZero(o[k], valueToFind)) { return true; } k++; } return false; } }); }";


     @Test
     public void testExampleQuery() throws Exception {               
        String query="abc  ( def  AND or and [ def";
        ArrayList<String> results = getQueryParseResultFromJavascript(query);
        System.out.println(results);
    }
    
     private ArrayList<String> getQueryParseResultFromJavascript(String query) throws Exception{
         ScriptEngine scriptEngine = getJavascriptScriptEnginePatched();
         String method = FileUtil.fetchUTF8("javascript/checkQueryForBadSyntax.js");          
         scriptEngine.eval(method);        
         Invocable inv = (Invocable) scriptEngine;        
                  
         JSObject obj = (JSObject) inv.invokeFunction("checkQueryForBadSyntax", query); 
         Collection result = obj.values();
         ArrayList<String> parseResults = new ArrayList<String>();
         for (Object o : result) {
              parseResults.add(o.toString());
         }                       
         
         return parseResults;
     }
     

     private  ScriptEngine getJavascriptScriptEnginePatched() throws Exception {
         final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
         final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("nashorn");
         
         //Define the .includes function in the script engine.
         scriptEngine.eval(NASHORN_POLYFILL_ARRAY_PROTOTYPE_INCLUDES);
         scriptEngine.eval(NASHORN_POLYFILL_STRING_PROTOTYPE_INCLUDES);         
         
         return scriptEngine;
     }

}
    
