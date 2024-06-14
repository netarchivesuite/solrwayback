package dk.kb.netarchivesuite.solrwayback.javascript;


import dk.kb.netarchivesuite.solrwayback.util.FileUtil;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *  Unittest method to call the query parser defined in the VUE typescript.
 *  The method has been copied to a new file and is now pure javascript.
 */
public class JavascriptTests {

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


    /**
     * Calls {@code checkQueryForBadSyntax} with {@code query} and checks that the result is the {@code expectedMessage}.
     * @param query a query as used in SolrWayback.
     * @param expectedContains the response from the sanity checker should contain this text.
     */
    private void assertWarning(String query, String... expectedContains) throws Exception {
        // Defining the engine explicitly so that testing doesn't warn about no runtime compilation of JavaScript
        Engine engine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
        try (Context context = Context.newBuilder("js").engine(engine).allowIO(true).build()) {

            String jsCode = extractQueryChecker();
            context.eval("js", jsCode);

            String warning = getQueryParseResultFromJavascript(query, context).toString();
            if (expectedContains.length == 0) { // Any warning
                assertFalse("Sanity checking the query '" + query + "' should deliver a warning",
                        warning.isEmpty());
            }

            for (String expected : expectedContains) {
                assertTrue("Sanity checking the query '" + query + "' should deliver a warning containing '" +
                                expected + "' but delivered '" + warning + "'",
                        warning.contains(expected));
            }
        }
    }

    /**
     * Calls {@code checkQueryForBadSyntax} with {@code query} and checks no warning is raised.
     * @param query a query as used in SolrWayback.
     */
    private void assertClean(String query) throws Exception {
        // Defining the engine explicitly so that testing doesn't warn about no runtime compilation of JavaScript
        Engine engine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
        try (Context context = Context.newBuilder("js").engine(engine).allowIO(true).build()) {
            String jsCode = extractQueryChecker();
            context.eval("js", jsCode);

            String warning = getQueryParseResultFromJavascript(query, context).toString();
            assertTrue("Sanity checking the query '" + query + "' should deliver no warning but returned'" +
                    warning + "'", "[]".equals(warning));
        }
    }

    private Value getQueryParseResultFromJavascript(String query, Context context){
        Value checkQueryForBadSyntax = context.getBindings("js").getMember("checkQueryForBadSyntax");
        return checkQueryForBadSyntax.execute(query);
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
    
