package dk.kb.netarchivesuite.solrwayback.service.dto;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class IndexDoc {
    
    private long offset;
    private String source_file_path;
    private String resourceName;
    private int statusCode;
    private String title;
    private String id;    
    private String url;
    private String url_norm;
    private String mimeType;
    private String contentTypeNorm;
    private long lastModifiedLong;
    private String contentType;
    private String domain;
    private String type;
    private String crawlDate; // format 2009-12-09T05:32:50Z    
    private long crawlDateLong;
    private String hash;
    private double score;
    private String contentEncoding;
    private ArrayList<String> imageUrls = new ArrayList<String>(); //This field is not normally set.
    private String exifLocation;
    
    public IndexDoc(){        
    }
    
    public long getLastModifiedLong() {
      return lastModifiedLong;
    }

    public void setLastModifiedLong(long lastModifiedLong) {
      this.lastModifiedLong = lastModifiedLong;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
   
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
 
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    public String getContentTypeNorm() {
        return contentTypeNorm;
    }
    public void setContentTypeNorm(String contentTypeNorm) {
        this.contentTypeNorm = contentTypeNorm;
    }
        
    public String getContentType() {
      return contentType;
    }

    public void setContentType(String contentType) {
      this.contentType = contentType;
    }

    public String getCrawlDate() {
        return crawlDate;
    }
    public void setCrawlDate(String crawlDate) {
        this.crawlDate = crawlDate;
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

    public double getScore() {
        return score;
    }
    public void setScore(double score) {
        this.score = score;
    }

	public String getContentEncoding() {
		return contentEncoding;
	}

	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}

  public String getUrl_norm() {
    return url_norm;
  }

  public void setUrl_norm(String url_norm) {
    this.url_norm = url_norm;
  }

  public long getCrawlDateLong() {
    return crawlDateLong;
  }

  public void setCrawlDateLong(long crawlDateLong) {
    this.crawlDateLong = crawlDateLong;
  }

  public ArrayList<String> getImageUrls() {
    return imageUrls;
  }

  public void setImageUrls(ArrayList<String> imageUrls) {
    this.imageUrls = imageUrls;
  }

  public String getSource_file_path() {
    return source_file_path;
  }

  public void setSource_file_path(String source_file_path) {
    this.source_file_path = source_file_path;
  }

  public String getExifLocation() {
    return exifLocation;
  }

  public void setExifLocation(String exifLocation) {
    this.exifLocation = exifLocation;
  }

  public String getResourceName() {
    return resourceName;
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

    
}
