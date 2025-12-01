package dk.kb.netarchivesuite.solrwayback.export;

import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import org.apache.solr.common.SolrDocument;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class StreamingRawZipExportTest {

    /** Normalize non-ASCII and unsafe chars from filename. */
    @Test
    public void testNormalizeFilename_basic() {
        String in = "id_ürl@!#.html";
        String out = StreamingRawZipExport.normalizeFilename(in);
        assertEquals("id_rl.html", out);
    }

    /** Collapse dots/underscores in filename. */
    @Test
    public void testNormalizeFilename_dotsAndUnderscores() {
        String in = "abc__def_.txt";
        String out = StreamingRawZipExport.normalizeFilename(in);
        assertEquals("abc_def.txt", out);
    }

    /** Truncate overly long filenames to 255 chars. */
    @Test
    public void testNormalizeFilename_truncateLong() {
        // create a filename > 255 chars
        String longName = "a".repeat(300) + ".txt";
        String out = StreamingRawZipExport.normalizeFilename(longName);
        assertTrue(out.length() <= 255);
        assertTrue(out.endsWith(".txt"));
    }

    /** Create HTML filename from metadata. */
    @Test
    public void testCreateFilename_htmlAndNormalization() throws Exception {
        StreamingRawZipExport zipExport = new StreamingRawZipExport();
        WarcMetadataFromSolr meta = new WarcMetadataFromSolr();
        meta.setId("id");
        meta.setUrl("http://example.com/abc"); // setter normalizes slashes
        meta.setMimetype("text/html; charset=UTF-8");

        String filename = zipExport.createFilename(meta);
        assertEquals("id_http_examplecom_abc.html", filename);
    }

    /** Create filename using extension or default .dat. */
    @Test
    public void testCreateFilename_extensionAndDat() throws Exception {
        StreamingRawZipExport zipExport = new StreamingRawZipExport();
        WarcMetadataFromSolr meta = new WarcMetadataFromSolr();
        meta.setId("ID1");
        meta.setUrl("http://example.com/1");
        meta.setMimetype("application/octet-stream");
        // fileExtension null -> .dat
        String filenameDat = zipExport.createFilename(meta);
        assertTrue(filenameDat.endsWith(".dat"));

        meta.setFileExtension("png");
        String filenamePng = zipExport.createFilename(meta);
        assertTrue(filenamePng.endsWith(".png"));
    }

    /** Populate WarcMetadataFromSolr from a SolrDocument. */
    @Test
    public void testExtractMetadataSetsWarcMetadata() throws Exception {
        StreamingRawZipExport zipExport = new StreamingRawZipExport();
        SolrDocument doc = new SolrDocument();
        doc.setField("content_type_ext", "png");
        doc.setField("content_type", "image/png");
        doc.setField("id", "IDX");
        doc.setField("url", "http://ex/1");

        WarcMetadataFromSolr meta = new WarcMetadataFromSolr();
        SolrDocument ret = zipExport.extractMetadata(doc, meta);
        assertSame(doc, ret);
        assertEquals("png", meta.getFileExtension());
        assertEquals("image/png", meta.getMimetype());
        assertEquals("IDX", meta.getId());
        assertEquals("http:__ex_1", meta.getUrl());
    }

    /** safeGetArcEntry wraps Facade.getArcEntry exceptions in RuntimeException. */
    @Test(expected = RuntimeException.class)
    public void testSafeGetArcEntry_throwsRuntime() throws Throwable {
        SolrDocument doc = new SolrDocument();
        doc.setField("source_file_path", "nonexistent");
        doc.setField("source_file_offset", 123L);

        // Direct call should throw RuntimeException wrapping the original exception
        StreamingRawZipExport.safeGetArcEntry(doc);
    }

    /** Add ArcEntry to zip and stream its binary content. */
    @Test
    public void testAddArcEntryToZipWritesEntryAndContent() throws Exception {
        StreamingRawZipExport zipExport = new StreamingRawZipExport();
        // Prepare ArcEntry with string content
        ArcEntry entry = new ArcEntry();
        entry.setStringContent("hello-world");

        // Create Warc Metadata
        WarcMetadataFromSolr meta = new WarcMetadataFromSolr();
        meta.setId("IDZIP");
        meta.setUrl("http://zip/test");
        meta.setMimetype("application/octet-stream");
        meta.setFileExtension("txt");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        // Call protected method directly to add entry to zip
        ArcEntry ret = zipExport.addArcEntryToZip(entry, zos, meta);
        assertSame(entry, ret);
        zos.close();

        // Read back the zip and verify contents
        byte[] zipBytes = baos.toByteArray();
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes));
        ZipEntry ze = zis.getNextEntry();
        assertNotNull(ze);
        String name = ze.getName();
        // normalize the expected filename the same way the implementation does
        String expectedName = zipExport.createFilename(meta);
        assertEquals(expectedName, name);

        // read entry content
        byte[] buf = new byte[32];
        int read = zis.read(buf);
        String content = new String(buf, 0, read);
        assertEquals("hello-world", content);
        zis.closeEntry();
        zis.close();
    }
}
