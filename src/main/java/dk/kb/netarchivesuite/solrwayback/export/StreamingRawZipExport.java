package dk.kb.netarchivesuite.solrwayback.export;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreaming;
import org.apache.cxf.helpers.IOUtils;
import org.apache.solr.common.SolrDocument;

import javax.ws.rs.core.StreamingOutput;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class StreamingRawZipExport {

    /**
     * TODO: JAVADOC
     * @param query
     * @param contentType
     * @param output
     * @throws IOException
     */
    public void getStreamingOutputWithZipOfContent(String query, String contentType, OutputStream output) throws IOException {
        StreamingOutput streamingOutput;

        SRequest request = SRequest.builder()
                .query(query)
                .filterQueries("content_type_norm:" + contentType)
                .fields("crawl_date", "source_file_path", "source_file_offset", "content_type_ext");

        ZipOutputStream zos = new ZipOutputStream(output);

        long streamedDocs = SolrGenericStreaming.create(request).stream()
                .map(StreamingRawZipExport::safeGetArcEntry)
                .map(entry -> addHtmlToZip(entry, zos))
                .count();

        zos.close();
    }

    private ArcEntry addHtmlToZip(ArcEntry entry, ZipOutputStream zos) {
        ZipEntry htmlFile = new ZipEntry(entry.getFileName() + "_" +  entry.hashCode() + ".html");
        try {
            zos.putNextEntry(htmlFile);
            IOUtils.copy(entry.getBinaryDecoded(), zos);
            zos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return entry;
    }

    private static ArcEntry safeGetArcEntry(SolrDocument doc) {
        try {
            return Facade.getArcEntry((String) doc.getFieldValue("source_file_path"), (long) doc.getFieldValue("source_file_offset"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
