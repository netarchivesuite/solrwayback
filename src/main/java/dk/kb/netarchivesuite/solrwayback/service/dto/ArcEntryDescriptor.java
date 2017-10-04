package dk.kb.netarchivesuite.solrwayback.service.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ArcEntryDescriptor {

    private String source_file_path;
    private String url;
    private String url_norm;
    private String hash;
    private long offset;
    private String content_type;    
    
    public ArcEntryDescriptor(){        
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
   

    public String getSource_file_path() {
      return source_file_path;
    }

    public void setSource_file_path(String source_file_path) {
      this.source_file_path = source_file_path;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public String getContent_type() {
        return content_type;
    }

    public void setContent_type(String content_type) {
        this.content_type = content_type;
    }
    
    public String getUrl_norm() {
      return url_norm;
    }

    public void setUrl_norm(String url_norm) {
      this.url_norm = url_norm;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hash == null) ? 0 : hash.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArcEntryDescriptor other = (ArcEntryDescriptor) obj;
        if (hash == null) {
            if (other.hash != null)
                return false;
        } else if (!hash.equals(other.hash))
            return false;
        return true;
    }
    
    
    
}
