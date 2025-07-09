package dk.kb.netarchivesuite.solrwayback.service.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HarvestDates {

    private String url;
    private long numberOfHarvests=0;    
    private List<Long>  dates = new ArrayList<Long>();
    public long getNumberOfHarvests() {
      return numberOfHarvests;
    }
    public void setNumberOfHarvests(long numberOfHarvests) {
      this.numberOfHarvests = numberOfHarvests;
    }
    public List<Long> getDates() {
      return dates;
    }
    public void setDates(List<Long> dates) {
      this.dates = dates;
    }
    public String getUrl() {
      return url;
    }
    public void setUrl(String url) {
      this.url = url;
    }
    
    
}
