package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;

import javax.ws.rs.core.MultivaluedMap;
import java.text.ParseException;

/**
 * Object which contains metadata about a single original resource, used to produce correct mementos.
 */
public class MementoMetadata {

    private String timeMapHead;
    private String firstMemento;
    private String lastMemento;
    private long firstWaybackDate = 99999999999999L;
    private long lastWaybackDate = 19500101010000L;
    
    private MultivaluedMap<String, Object> httpHeaders;

    public MementoMetadata(){}
    public String getFirstMemento() {
        return firstMemento;
    }

    public String getLastMemento() {
        return lastMemento;
    }

    public long getFirstWaybackDate(){
        return firstWaybackDate;
    }

    public long getLastWaybackDate(){
        return lastWaybackDate;
    }

    public String getTimeMapHead() {
        return timeMapHead;
    }

    public MultivaluedMap<String, Object> getHttpHeaders() {
        return httpHeaders;
    }

    public void setFirstMemento(String firstMemento) {
        this.firstMemento = firstMemento;
    }

    /**
     * Sets the first memento to given wayback date.
     * @param waybackDate represented as 14 digits
     */
    public void setFirstMemento(Long waybackDate) throws ParseException {
        this.firstMemento = DateUtils.convertWaybackdate2Mementodate(waybackDate);
    }

    public void setLastMemento(String lastMemento) {
        this.lastMemento = lastMemento;
    }

    public void setFirstMementoFromFirstWaybackDate() throws ParseException {
        this.firstMemento = DateUtils.convertWaybackdate2Mementodate(this.firstWaybackDate);
    }

    public void setLastMementoFromLastWaybackDate() throws ParseException {
        this.lastMemento = DateUtils.convertWaybackdate2Mementodate(this.lastWaybackDate);
    }

    public void setFirstWaybackDate(long firstWaybackDate) {
        this.firstWaybackDate = firstWaybackDate;
    }

    public void setLastWaybackDate(long lastWaybackDate) {
        this.lastWaybackDate = lastWaybackDate;
    }

    public void setTimeMapHeadForLinkFormat(String originalResource, Integer pageNumber) {
        String timemapLink = "";
        if (pageNumber == null || pageNumber == 0){
             timemapLink = PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/link/" + originalResource;
        } else {
            timemapLink = PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/"+pageNumber+"/link/" + originalResource;
        }

        this.timeMapHead = "<" + originalResource + ">;rel=\"original\",\n" +
               "<"+ timemapLink + ">" +
               "; rel=\"self\"; type=\"application/link-format\"" +
               "; from=\"" + this.getFirstMemento() + "\"" +
               "; until=\"" + this.getLastMemento() + "\",\n" +
               "<"+ PropertiesLoaderWeb.WAYBACK_SERVER +"services/memento/" + originalResource + ">" +
               "; rel=\"timegate\",\n";
    }

    public void setHttpHeaders(MultivaluedMap<String, Object> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }
}
