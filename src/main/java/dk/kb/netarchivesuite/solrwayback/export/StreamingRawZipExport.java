package dk.kb.netarchivesuite.solrwayback.export;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.WarcMetadataFromSolr;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreaming;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 *
 */
public class StreamingRawZipExport {
    private static final Logger log = LoggerFactory.getLogger(StreamingRawZipExport.class);


    /**
     * Streams content of specific type (e.g. HTML, images, PDF.) to a zip file.
     * @param query       for solr to extract content from.
     * @param contentType defines which types of material to include in the zip file.
     * @param output      represents an output stream, where the zipped content gets delivered.
     */
    public void getStreamingOutputWithZipOfContent(String query, String contentType,
                                                   OutputStream output, String... filterQueries) throws IOException {

        String normalizedContentType = normalizeContentType(contentType);
        String fullFilters = SolrUtils.combineFilterQueries("content_type", normalizedContentType, filterQueries);

        SRequest request = SRequest.builder()
                .query(query)
                .filterQueries(fullFilters)
                .fields("crawl_date", "source_file_path", "source_file_offset",
                        "content_type_ext", "content_type", "id", "url");

        ZipOutputStream zos = new ZipOutputStream(output);
        WarcMetadataFromSolr warcMetadata = new WarcMetadataFromSolr();

        long streamedDocs = SolrGenericStreaming.create(request).stream()
                .map(doc -> extractMetadata(doc, warcMetadata))
                .map(StreamingRawZipExport::safeGetArcEntry)
                .map(entry -> addArcEntryToZip(entry, zos, normalizedContentType, warcMetadata))
                .count();

        zos.close();
        output.close();
        log.info("Streamed {} warc entries with the contentType: '{}'.", streamedDocs, normalizedContentType);
    }

    /**
     * Normalize content type.
     * @param contentType string to normalize.
     * @return            normalized contentType string.
     */
    private String normalizeContentType(String contentType) {
        String normalizedContentType = Normalizer.normalize(contentType, Normalizer.Form.NFD);
        return normalizedContentType.replaceAll("[^\\x00-\\x7F]", "");
    }

    /**
     * Extract metadata for a WARC entry from a Solr Document.
     * The method extracts, the ID, mimetype and fileextension for the WARC entry.
     * @param doc           SolrDocument to retrieve the WARC metadata from.
     * @param warcMetadata  Object to save metadata information to.
     * @return              The input SolrDocument is returned, which makes this method stream compliant.
     */
    private SolrDocument extractMetadata(SolrDocument doc, WarcMetadataFromSolr warcMetadata) {
        warcMetadata.setFileExtension((String) doc.getFieldValue("content_type_ext"));
        warcMetadata.setMimetype((String) doc.getFieldValue("content_type"));
        warcMetadata.setId((String) doc.getFieldValue("id"));
        warcMetadata.setUrl((String) doc.getFieldValue("url"));

        return doc;
    }

    /**
     * Safe streamable implementation of {@code Facade.getArcEntry}
     * @param doc represents a SolrDocument, which contains info on the ARC/WARC filepath and offset for entries.
     * @return     an arc entry, from the filepath and offset delivered by the solr document.
     */
    private static ArcEntry safeGetArcEntry(SolrDocument doc) {
        try {
            return Facade.getArcEntry((String) doc.getFieldValue("source_file_path"), (long) doc.getFieldValue("source_file_offset"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Add individual arc/warc entries to a zip stream.
     * @param entry that is to be added to the given zip stream.
     * @param zos   which entries gets added to.
     * @return      the input arc/warc entry, for further use in a stream.
     */
    private ArcEntry addArcEntryToZip(ArcEntry entry, ZipOutputStream zos, String contentType, WarcMetadataFromSolr warcMetadata) {
        String filename = createFilename(contentType, warcMetadata);
        ZipEntry zipArcEntry = new ZipEntry(filename);

        try {
            zos.putNextEntry(zipArcEntry);
            IOUtils.copy(entry.getBinaryRaw(), zos);
            zos.closeEntry();
            zos.flush(); // <-- This flush is very important. Without it, the service does not deliver any files.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return entry;
    }


    /**
     * Create unique filename for zip entries from metadata from solr.
     * Files in the zip entry gets named by the following structure: waybackdate_id_originalUrlStrippedForNonASCIIChars.extension.
     * @param contentType   is used to choose how the filename is created.
     * @param warcMetadata  contains the timestamp, id, originalUrl and file extension, which is used to create the filename.
     * @return              a string in the format timestamp_id_originalUrlStrippedForNonASCIIChars.extension.
     */
    private String createFilename(String contentType, WarcMetadataFromSolr warcMetadata) {
        // TODO: Look at naming from Tokes GH issue: https://github.com/netarchivesuite/solrwayback/issues/382

        String filename;
        if (contentType.equals("text/html")){
            filename = warcMetadata.getId() + "_" + warcMetadata.getUrl() + ".html";
        } else if (warcMetadata.getMimetype().contains("text/html")) {
            filename = warcMetadata.getId() + "_" + warcMetadata.getUrl() + ".html";
        } else {
            if (warcMetadata.getFileExtension() == null){
                filename = warcMetadata.getId() + "_" + warcMetadata.getUrl() + ".dat";
            } else {
                filename = warcMetadata.getId() + "_" + warcMetadata.getUrl() + "." + warcMetadata.getFileExtension();
            }
        }

        return filename;
    }
}
