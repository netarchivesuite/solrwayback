package dk.kb.netarchivesuite.solrwayback.export;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.service.dto.ArcEntry;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.solr.SolrGenericStreaming;
import dk.kb.netarchivesuite.solrwayback.util.SolrUtils;
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
     * @param output      represents an output stream, where the zipped content gets delivered.
     */
    public void getStreamingOutputWithZipOfContent(String query,
                                                   OutputStream output, String... filterQueries) throws IOException {

        SRequest request = SRequest.builder()
                .query(query)
                .filterQueries(filterQueries)
                .fields("crawl_date", "source_file_path", "source_file_offset",
                        "content_type_ext", "content_type", "id", "url");

        ZipOutputStream zos = new ZipOutputStream(output);
        WarcMetadataFromSolr warcMetadata = new WarcMetadataFromSolr();

        long streamedDocs = SolrGenericStreaming.create(request).stream()
                .map(doc -> extractMetadata(doc, warcMetadata))
                .map(StreamingRawZipExport::safeGetArcEntry)
                .map(entry -> addArcEntryToZip(entry, zos, warcMetadata))
                .count();

        zos.close();
        output.close();
        log.info("Zip export has completed. {} warc entries have been streamed, zipped and delivered.", streamedDocs);
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
    private ArcEntry addArcEntryToZip(ArcEntry entry, ZipOutputStream zos, WarcMetadataFromSolr warcMetadata) {
        String filename = createFilename(warcMetadata);
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
     * @param warcMetadata  contains the timestamp, id, originalUrl and file extension, which is used to create the filename.
     * @return              a string in the format timestamp_id_originalUrlStrippedForNonASCIIChars.extension.
     */
     private String createFilename(WarcMetadataFromSolr warcMetadata) {

        String filename;
        if (warcMetadata.getMimetype().contains("text/html")) {
            filename = warcMetadata.getId() + "_" + warcMetadata.getUrl() + ".html";
        } else {
            if (warcMetadata.getFileExtension() == null){
                filename = warcMetadata.getId() + "_" + warcMetadata.getUrl() + ".dat";
            } else {
                filename = warcMetadata.getId() + "_" + warcMetadata.getUrl()+"." + warcMetadata.getFileExtension();
            }
        }

        // Remove everything non-alphanumerical or underscore
         return normalizeFilename(filename);
     }

    /**
     * Create a safe filename from string. Removes everything non-alphanumerical or underscore.
     * Keeps last .
     * @param filename
     * @return
     */
    public static String normalizeFilename(String filename) {
        filename = filename.replaceAll("[^A-Za-z0-9_.]", "");

        // Remove all but last dot
        filename = filename.substring(0, filename.lastIndexOf(".")).replaceAll("\\." , "").concat(filename.substring(filename.lastIndexOf(".")));

        //Remove trailing underscore
        filename = filename.replaceAll("_\\.", ".");
        // Remove two or more consecutive underscores
        filename = filename.replaceAll("_{2,}", "_");

        // Check filename length and make sure not to long
        if (filename.length() > 255){
            int charsToRemove = filename.length() - 255;
            String correctFilename = filename.substring(0,130) + filename.substring(130+charsToRemove);
            return correctFilename;
        }

        return filename;
    }
}
