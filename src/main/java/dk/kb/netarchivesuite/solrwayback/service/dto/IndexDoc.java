package dk.kb.netarchivesuite.solrwayback.service.dto;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class IndexDoc {
    
    private long offset;
    private String arc_full; //TODO replace with source_file-s       
    private String title;
    private String id;    
    private String url;
    private String url_norm;
    private String mimeType;
    private String contentTypeNorm;
    private String crawlDate; // format 2009-12-09T05:32:50Z    
    private long crawlDateLong;
    private String hash;
    private double score;
    private String contentEncoding;
    
    public IndexDoc(){        
    }
    
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getArc_full() {
        return arc_full;
    }
    public void setArc_full(String arc_full) {
        this.arc_full = arc_full;
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

       
}
