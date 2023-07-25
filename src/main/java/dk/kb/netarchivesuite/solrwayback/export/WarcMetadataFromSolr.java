package dk.kb.netarchivesuite.solrwayback.export;

import java.io.OutputStream;
import java.text.Normalizer;

import dk.kb.netarchivesuite.solrwayback.export.StreamingRawZipExport;

/**
 * Object used to store metadata for a WARC entry from a SolrDocument.
 * Variables in the object are used for constructing filenames for
 * {@link StreamingRawZipExport#getStreamingOutputWithZipOfContent(String, OutputStream, String...)
 * ZIP export}.
 *
 */
public class WarcMetadataFromSolr {

    private String id;
    private String mimetype;
    private String fileExtension;
    private String hash;
    private String url;

    public String getFileExtension() {
        return fileExtension;
    }
    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getHash() {
        return hash;
    }
    public void setHash(String hash){
        this.hash = hash;
    }

    public String getId(){
        return id;
    }
    public void setId(String id) {
        id = id.replace("==", "");
        this.id = id.replace("/", "_");
    }

    public String getMimetype() {
        return mimetype;
    }
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public String getUrl() {return url;}
    public void setUrl(String url) {
        url = Normalizer.normalize(url, Normalizer.Form.NFD);
        url = url.replaceAll("[^\\x00-\\x7F]", "");
        this.url = url.replaceAll("/", "_");
    }

    public WarcMetadataFromSolr(String fileExtension, String hash){
        this.fileExtension = fileExtension;
        this.hash = hash;
    }

    public WarcMetadataFromSolr(){
    }
}
