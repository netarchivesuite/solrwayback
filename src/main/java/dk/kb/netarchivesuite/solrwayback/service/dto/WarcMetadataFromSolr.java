package dk.kb.netarchivesuite.solrwayback.service.dto;

public class WarcMetadataFromSolr {

    private String id;
    private String fileExtension;
    private String hash;

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

    public WarcMetadataFromSolr(String fileExtension, String hash){
        this.fileExtension = fileExtension;
        this.hash = hash;
    }

    public WarcMetadataFromSolr(){
    }
}
