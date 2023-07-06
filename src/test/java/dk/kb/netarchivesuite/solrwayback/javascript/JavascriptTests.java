package dk.kb.netarchivesuite.solrwayback.javascript;


import dk.kb.netarchivesuite.solrwayback.util.FileUtil;
import jdk.nashorn.api.scripting.JSObject;
import org.junit.Before;
import org.junit.Test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    private Invocable inv;

    @Before
    public void initScripts() throws Exception {
        ScriptEngine scriptEngine = getJavascriptScriptEnginePatched();
        scriptEngine.eval(extractQueryChecker());
        inv = (Invocable) scriptEngine;
    }

    @Test
    public void testBooleanCase() throws Exception {
        assertClean("foo AND bar");

        assertWarning("foo and bar", "Booleans must be uppercase");
    }

    @Test
    public void testAmbiguous() throws Exception {
        assertClean("foo OR bar OR zoo");
        assertClean("foo AND bar AND zoo");

        assertWarning("foo AND bar OR zoo", "Ambiguous AND/OR");
        assertWarning("foo OR bar AND zoo", "Ambiguous AND/OR");
    }

    @Test
    public void testQuotes() throws Exception {
        assertClean("foo:\"bar\"");
        
        assertWarning("foo:‟bar‟", "Smart quotes", "use simple quote signs");
    }

    @Test
    public void testDoubleColon() throws Exception {
        assertClean("foo:\"bar:zoo\"");
        assertClean("foo:[2022:07:06T13:05:00Z TO *]");
        assertClean("foo:[* TO 2022:07:06T13:05:00Z]");

        assertWarning("foo:bar:zoo", "Two colons without quote signs");
    }

    @Test
    public void testURL() throws Exception {
        assertClean("url:\"http://example.com\"");
        assertClean("url:http\\://example.com");
        assertClean("\"http\\://example.com\"");

        assertWarning("http://example.com", "Standalone URL");
        assertWarning("https://example.com", "Standalone URL");
    }

    @Test
    public void testUnbalancedParentheses() throws Exception {
        assertClean("(foo bar)");
        assertClean("((foo AND bar) OR zoo)");

        assertWarning("(foo bar", "missing end parenthesis", "Make sure to balance parentheses");
        assertWarning("((foo bar) zoo", "missing end parenthesis", "Make sure to balance parentheses");
        assertWarning("foo bar)", "missing start parenthesis", "Make sure to balance parentheses");
    }

    @Test
    public void testSinglestar() throws Exception {
        assertClean("*:* NOT foo");

        assertWarning("* NOT foo", "single star");
    }

    @Test
    public void testMix() throws Exception {
        assertWarning("foo:‟bar‟ AND foo:bar:zoo", "use simple quote signs", "Two colons without quote signs");
    }

    public void disabledtestExampleQuery() throws Exception {
        String query="abc  ( def  AND or and [ def";
        List<String> results = getQueryParseResultFromJavascript(query);
        System.out.println(results);
    }

    /**
     * Calls {@code checkQueryForBadSyntax} with {@code query} and checks that the result is the {@code expectedMessage}.
     * @param query a query as used in SolrWayback.
     * @param expectedContains the response from the sanity checker should contain this text.
     */
    private void assertWarning(String query, String... expectedContains) throws Exception {
        String warning = getQueryParseResultFromJavascript(query).toString();
        if (expectedContains.length == 0) { // Any warning
            assertFalse("Sanity checking the query '" + query + "' should deliver a warning",
                    warning.isEmpty());
        }

        for (String expected: expectedContains) {
            assertTrue("Sanity checking the query '" + query + "' should deliver a warning containing '" +
                            expected + "' but delivered '" + warning + "'",
                    warning.contains(expected));
        }
    }

    /**
     * Calls {@code checkQueryForBadSyntax} with {@code query} and checks no warning is raised.
     * @param query a query as used in SolrWayback.
     */
    private void assertClean(String query) throws Exception {
        String warning = getQueryParseResultFromJavascript(query).toString();
        assertTrue("Sanity checking the query '" + query + "' should deliver no warning but returned'" +
                        warning + "'", "[]".equals(warning));
    }

    private List<String> getQueryParseResultFromJavascript(String query) throws Exception{
        JSObject obj = (JSObject) inv.invokeFunction("checkQueryForBadSyntax", query);
        return obj.values().stream().map(Object::toString).collect(Collectors.toList());
    }

    private static ScriptEngine getJavascriptScriptEnginePatched() throws Exception {
        final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("nashorn");

        //Define the .includes function in the script engine.
        scriptEngine.eval(NASHORN_POLYFILL_ARRAY_PROTOTYPE_INCLUDES);
        scriptEngine.eval(NASHORN_POLYFILL_STRING_PROTOTYPE_INCLUDES);

        return scriptEngine;
    }

    /**
     * Highly specialized extractor that takes the method {@code $_checkQueryForBadSyntax} from {@code SearchUtils.js},
     * converts it to plain JavaScript and returns it.
     * @return a unit-testable version of the JavaScript query sanity checker.
     */
    private static String extractQueryChecker() throws IOException {
        Path searchUtilsFile = FileUtil.resolve("logback-test.xml"). // Known file in the "target"-folder
                getParent().getParent().getParent().                         // Root of project
                resolve("src").resolve("js").resolve("src").resolve("mixins").resolve("SearchUtils.js"); // SearchUtils
        assertTrue("The 'SearchUtils.js' should be available", Files.isReadable(searchUtilsFile));
        String searchUtilsRaw = FileUtil.fetchUTF8(searchUtilsFile.toString());
        Matcher m = SEARCH_UTILS_QUERY_CHECKER.matcher(searchUtilsRaw);
        assertTrue("The 'checkQueryForBadSyntax' method should be extractable from '" + searchUtilsFile + "'",
                m.matches());
        String checkQueryRaw = m.group(1);
        return "function " + SEARCH_UTILS_LET_FINDER.matcher(checkQueryRaw).replaceAll("$1var ");
    }
    // The TypeScript prefix "$_" must be removed
    private static final Pattern SEARCH_UTILS_QUERY_CHECKER = Pattern.compile(
            ".*[$]_(checkQueryForBadSyntax.*return responses[^}]*}).*",
            Pattern.MULTILINE + Pattern.DOTALL);
    // let must be replaced with var
    private static final Pattern SEARCH_UTILS_LET_FINDER = Pattern.compile("(\\p{Blank}*)let ");
}
    
