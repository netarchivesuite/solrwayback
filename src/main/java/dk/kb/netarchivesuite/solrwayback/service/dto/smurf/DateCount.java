package dk.kb.netarchivesuite.solrwayback.service.dto.smurf;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DateCount {

  private String date;
  private long count;
  private long total;
  
  public DateCount(){    
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }
  
}