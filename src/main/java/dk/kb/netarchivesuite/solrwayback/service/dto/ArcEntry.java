package dk.kb.netarchivesuite.solrwayback.service.dto;



//Notice this class is returned both by the ArcParser and WarcParser.
//Could not think of a good common name...
public class ArcEntry {

    private byte[] binary;
    
    private String url;
    private long contentLength;
    private long warcEntryContentLength; //From warc header#1. This does not exist for arc files
    private String contentType; //As returned by the webserver when harvested
    private String contentTypeExt; //As returned by the webserver when harvested
    private String fileName;
    private String crawlDate; // format 2009-12-09T05:32:50Z
    private String contentEncoding;
    
    public byte[] getBinary() {
        return binary;
    }
    public void setBinary(byte[] binary) {
        this.binary = binary;
    }
    public long getContentLength() {
        return contentLength;
    }
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public long getWarcEntryContentLength() {
        return warcEntryContentLength;
    }
    public void setWarcEntryContentLength(long warcEntryContentLength) {
        this.warcEntryContentLength = warcEntryContentLength;
    }
    public String getCrawlDate() {
        return crawlDate;
    }
    public void setCrawlDate(String crawlDate) {
        this.crawlDate = crawlDate;
    }
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getContentEncoding() {
		return contentEncoding;
	}
	public void setContentEncoding(String contentEncoding) {
		this.contentEncoding = contentEncoding;
	}
	public String getContentTypeExt() {
		return contentTypeExt;
	}
	public void setContentTypeExt(String contentTypeExt) {
		this.contentTypeExt = contentTypeExt;
	}	
   
}
