package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.util.DateUtils;

import java.text.ParseException;

public class MementoMetadata {
    private String firstMemento;
    private String lastMemento;
    private long firstWaybackDate;
    private long lastWaybackDate;

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
}