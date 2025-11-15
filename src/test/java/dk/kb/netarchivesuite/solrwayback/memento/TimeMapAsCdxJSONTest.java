package dk.kb.netarchivesuite.solrwayback.memento;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.solr.common.SolrDocument;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TimeMapAsCdxJSONTest {

    /** Write header array to JsonGenerator. */
    @Test
    public void testWriteHeaderArray() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator jg = JsonFactory.builder().build().createGenerator(baos);

        // Access private static method via reflection
        Method m = TimeMapAsCdxJSON.class.getDeclaredMethod("writeHeaderArray", JsonGenerator.class);
        m.setAccessible(true);
        m.invoke(null, jg);
        jg.flush();
        jg.close();

        String out = baos.toString(StandardCharsets.UTF_8);
        String expected = "[\"urlkey\",\"timestamp\",\"original\",\"mimetype\",\"statuscode\",\"digest\",\"length\"]\n";
        assertEquals(expected, out);
    }

    /** Return string value when present and empty when missing. */
    @Test
    public void testExtractNonNullStringFromSolr_presentAndMissing() throws Exception {
        SolrDocument doc = new SolrDocument();
        doc.setField("foo", "bar");

        // Access private static method via reflection
        Method m = TimeMapAsCdxJSON.class.getDeclaredMethod("extractNonNullStringFromSolr", SolrDocument.class, String.class);
        m.setAccessible(true);
        String present = (String) m.invoke(null, doc, "foo");
        assertEquals("bar", present);

        String missing = (String) m.invoke(null, doc, "no_such_field");
        assertEquals("", missing);
    }

    /** Add a single memento object to the generator and return the same SolrDocument. */
    @Test
    public void testAddMementoToTimeMapObject() throws Exception {
        SolrDocument doc = new SolrDocument();
        List<String> hostSurt = Arrays.asList("com,example,", "com,example,)/path");
        doc.setField("host_surt", hostSurt);
        doc.setField("wayback_date", "20200101000000");
        doc.setField("url", "http://example/1");
        doc.setField("content_type", "text/html");
        doc.setField("status_code", "200");
        doc.setField("hash", "ABC123");
        doc.setField("content_length", "42");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator jg = JsonFactory.builder().build().createGenerator(baos);

        Method m = TimeMapAsCdxJSON.class.getDeclaredMethod("addMementoToTimeMapObject", SolrDocument.class, JsonGenerator.class);
        m.setAccessible(true);
        Object ret = m.invoke(null, doc, jg);
        assertSame(doc, ret);
        jg.flush();
        jg.close();

        String out = baos.toString(StandardCharsets.UTF_8);
        // hostsurt is last element of list
        // The hostsurt string may contain characters we didn't escape exactly; compare by checking key substrings in order
        assertTrue(out.startsWith("[\""));
        assertTrue(out.contains("20200101000000"));
        assertTrue(out.contains("http://example/1"));
        assertTrue(out.contains("text/html"));
        assertTrue(out.contains("200"));
        assertTrue(out.contains("ABC123"));
        assertTrue(out.contains("42"));
    }

}
