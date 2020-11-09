package dk.kb.netarchivesuite.solrwayback.parsers;


import dk.kb.netarchivesuite.solrwayback.UnitTestUtils;
import dk.kb.netarchivesuite.solrwayback.export.StreamingSolrWarcExportBufferedInputStream;
import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreaming;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestExportWarcStreaming extends UnitTestUtils {
  private static final Logger log = LoggerFactory.getLogger(TestExportWarcStreaming.class);

  @Test
  public void testSingleRecordStreamingExport() throws Exception {
    final String WARC = getFile("compressions_warc/transfer_compression_none.warc.gz").getCanonicalPath();
    final long OFFSET = 881;
    final int EXPECTED_CONTENT_LENGTH = 246;
    final int EXPECTED_EXPORT_LENGTH = 1102;

    byte[] upFrontBinary;
    {
      ArcEntry warcEntry = WarcParser.getWarcEntry(WARC, OFFSET, true);
      upFrontBinary = warcEntry.getBinary();
      assertEquals("Length for up front load should be as expected", EXPECTED_CONTENT_LENGTH, upFrontBinary.length);
    }

    {
      SolrGenericStreaming mockedSolr = getMockedSolrStream(WARC, OFFSET, 1, 1);

      StreamingSolrWarcExportBufferedInputStream exportStream = new
              StreamingSolrWarcExportBufferedInputStream(mockedSolr, 1, false);

      byte[] exportedBytes = new byte[EXPECTED_EXPORT_LENGTH];
      int exported = IOUtils.read(exportStream, exportedBytes);
      assertEquals("Expected the right number of bytes to be read", EXPECTED_EXPORT_LENGTH, exported);
      assertEquals("There should be no more content in the export stream", -1, exportStream.read());

      assertBinaryEnding(upFrontBinary, exportedBytes);
    }
  }

  @Test
  public void testGzipExport() throws Exception {
    final String WARC = getFile("compressions_warc/transfer_compression_none.warc.gz").getCanonicalPath();
    final long OFFSET = 881;
    final int EXPECTED_CONTENT_LENGTH = 246;
    final int EXPECTED_EXPORT_LENGTH = 1102;
    final int batchSize = 30;
    final int batches = 3;

    final int EXPECTED_TOTAL_SIZE = EXPECTED_EXPORT_LENGTH*batchSize*batches;

    byte[] upFrontBinary;
    {
      ArcEntry warcEntry = WarcParser.getWarcEntry(WARC, OFFSET, true);
      upFrontBinary = warcEntry.getBinary();
      assertEquals("Length for up front load should be as expected", EXPECTED_CONTENT_LENGTH, upFrontBinary.length);
    }

    {
      SolrGenericStreaming mockedSolr = getMockedSolrStream(WARC, OFFSET, batchSize, batches);

      StreamingSolrWarcExportBufferedInputStream exportStream = new
              StreamingSolrWarcExportBufferedInputStream(mockedSolr, batchSize*batches, true);
      //GZIPInputStream gis = new GZIPInputStream(new BufferedInputStream(exportStream, 100)); // Fails after 6612
      //GZIPInputStream gis = new GZIPInputStream(new BufferedInputStream(exportStream, 100000)); // Does not fail

      // The build-in GZIPInputStream does not handle concatenated gzip-blocks well at all, if the inner stream
      // does not deliver the maximum possible bytes from calls to read(buf, offset, length). Or something like that.
      // Apache's GzipCompressorInputStream has explicit support for multi-block gzip-streams
      GzipCompressorInputStream gis = new GzipCompressorInputStream(exportStream, true);

      byte[] exportedBytes = new byte[EXPECTED_TOTAL_SIZE];

      log.info("Attempting to read " + exportedBytes.length + " bytes from GZIPInputStream(exportStream)");
      int exported = IOUtils.read(gis, exportedBytes);

      log.info("Got " + exported + " bytes, checking for trailing bytes");
      int extra = 0;
      while (gis.read() != -1) {
        extra++;
      }
      assertEquals("Expected the right number of bytes to be read", EXPECTED_TOTAL_SIZE, exported);
      assertEquals("There should be no more content in the export stream", 0, extra);

      assertBinaryEnding(upFrontBinary, exportedBytes);
    }
  }

  @Test
  public void testMultiExport() throws Exception {
    final String WARC = getFile("compressions_warc/transfer_compression_none.warc.gz").getCanonicalPath();
    final long OFFSET = 881;
    final int EXPECTED_EXPORT_LENGTH = 1102;
    final int batchSize = 50;

    final int EXPECTED_TOTAL_SIZE = EXPECTED_EXPORT_LENGTH*batchSize*2;

    {
      SolrGenericStreaming mockedSolr = getMockedSolrStream(WARC, OFFSET, batchSize, 2);

      StreamingSolrWarcExportBufferedInputStream exportStream = new
              StreamingSolrWarcExportBufferedInputStream(mockedSolr, batchSize*2, false);

      byte[] exportedBytes = new byte[EXPECTED_TOTAL_SIZE];

      log.info("Attempting to read " + exportedBytes.length + " bytes from exportStream");
      int exported = IOUtils.read(exportStream, exportedBytes);

      log.info("Got " + exported + " bytes, checking for trailing bytes");
      int extra = 0;
      while (exportStream.read() != -1) {
        extra++;
      }
      assertEquals("Expected the right number of bytes to be read", EXPECTED_TOTAL_SIZE, exported);
      assertEquals("There should be no more content in the export stream", 0, extra);

    }
  }

  /**
   * Checks that the last exported binary is exported correctly.
   *
   * Compares {@code expected} with the subset of {@code exported} that is expected.length in size and is located
   * immediately before the last 4 bytes.
   * @param expected      the expected bytes.
   * @param exported bytes from a WARC export.
   */
  private void assertBinaryEnding(byte[] expected, byte[] exported) {
    int minLength = expected.length+4;
    assertTrue("The exported bytes should be at least of length " + minLength + ", but was " + expected.length,
               exported.length >= minLength);
    int exportedOrigo = exported.length-4-expected.length;
    for (int i = 0 ; i < expected.length ; i++) {
      assertEquals(String.format(
              Locale.ENGLISH, "The bytes at expected[%d] and exportedBytes[%d (%d-4-%d+%d)] should be equal",
              i, exportedOrigo+i, exported.length, expected.length, i),
                   expected[i], exported[exportedOrigo+i]);
    }
  }

  public static void main(String[] args) throws Exception{
    PropertiesLoader.initProperties();
    String source_file_path="/home/teg/workspace/solrwayback/storedanske_export-00000.warc";
    int offset = 515818793;
    ArcEntry warcEntry = WarcParser.getWarcEntry(source_file_path,offset,true);
    
    byte[] bytes = warcEntry.getBinary(); // <--------- The binary
    String fileFromBytes = "image1.jpg";
    String fileFromBytesStream = "image2.jpg";
    String fileFromBytesWarcInputStream = "image3.jpg";
    String fileFromBytesWarcInputStream2 = "image4.jpg";
    
    /*
    FileUtils.writeByteArrayToFile(new File(fileFromBytes), bytes);
    
    
    FileOutputStream fos = new FileOutputStream(fileFromBytesStream);
    fos.write(bytes);
    fos.close();
    
    InputStream is = new ByteArrayInputStream(bytes);         
    System.out.println(is.read());
    System.out.println(is.read());
    System.out.println(is.read());
    System.out.println(is.read());
    System.out.println(is.read());
    System.out.println(is.read());
    
    
    FileUtils.copyInputStreamToFile( is, new File(fileFromBytesWarcInputStream)); 
    */
    
    try (InputStream is1 = Facade.exportWarcStreaming(false, false, false, "hash:\"sha1:PROTE66RZ6GDXPZI3ZAHG6YPCXRKZMEN\"")) {
      FileUtils.copyInputStreamToFile(is1, new File("export_final.warc"));
    }
    
  }

  // Returns 2 batches, each of size docCount
  private SolrGenericStreaming getMockedSolrStream(String WARC, long OFFSET, int docCount, int batches) throws Exception {
    SolrGenericStreaming mockedSolr = mock(SolrGenericStreaming.class);
    OngoingStubbing<SolrDocumentList> stub = when(mockedSolr.nextDocuments());

    for (int dl = 0 ; dl < batches ; dl++) {
      SolrDocumentList docs = new SolrDocumentList();
      docs.setMaxScore(1.0f);
      docs.setNumFound(docCount*2);
      docs.setStart(System.currentTimeMillis());

      for (int i = 0; i < docCount; i++) {
        SolrDocument doc = new SolrDocument();
        doc.addField("id", "MockedDocument_" + dl + "_" + i);
        doc.addField("source_file_path", WARC);
        doc.addField("source_file_offset", OFFSET);
        docs.add(doc);
      }
      stub = stub.thenReturn(docs);
    }
    stub.thenReturn(null);
    return mockedSolr;
  }

}
