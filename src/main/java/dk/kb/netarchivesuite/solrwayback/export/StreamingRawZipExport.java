package dk.kb.netarchivesuite.solrwayback.export;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.service.dto.WarcMetadataFromSolr;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreaming;
import org.apache.cxf.helpers.IOUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
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
    public void getStreamingOutputWithZipOfContent(String query, String contentType, OutputStream output) throws IOException {

        SRequest request = SRequest.builder()
                .query(query)
                .filterQueries("content_type_norm:" + contentType)
                .fields("crawl_date", "source_file_path", "source_file_offset", "content_type_ext", "hash", "id");

        ZipOutputStream zos = new ZipOutputStream(output);
        WarcMetadataFromSolr warcMetadata = new WarcMetadataFromSolr();

        long streamedDocs = SolrGenericStreaming.create(request).stream()
                .map(doc -> extractMetadata(doc, warcMetadata))
                .map(StreamingRawZipExport::safeGetArcEntry)
                .map(entry -> addArcEntryToZip(entry, zos, contentType, warcMetadata))
                .count();

        zos.close();
        output.close();
        log.info("Streamed {} warc entries.", streamedDocs);
    }

    private SolrDocument extractMetadata(SolrDocument doc, WarcMetadataFromSolr warcMetadata) {
        warcMetadata.setFileExtension((String) doc.getFieldValue("content_type_ext"));
        warcMetadata.setHash((String) doc.getFieldValue("hash"));
        warcMetadata.setId((String) doc.getFieldValue("id"));

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
        // TODO: Do some smart naming. Some files have content_type_ext, others have content_type_norm, if non maybe do .dat?
        // TODO: Look at naming from Tokes GH issue: https://github.com/netarchivesuite/solrwayback/issues/382
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
     * @param contentType   is used to choose how the filename is created.
     * @param warcMetadata  contains the timestamp, id and file extension, which is used to create the filename.
     * @return              a string in the format timestamp_id.extension.
     */
    private String createFilename(String contentType, WarcMetadataFromSolr warcMetadata) {
        String filename;
        if (contentType.equals("html")){
            filename = warcMetadata.getId() + "." + contentType;
        } else {
            if (warcMetadata.getFileExtension() == null){
                filename = warcMetadata.getId() + ".dat";
            } else {
                filename = warcMetadata.getId() + "." + warcMetadata.getFileExtension();
            }
        }

        return filename;
    }
}
