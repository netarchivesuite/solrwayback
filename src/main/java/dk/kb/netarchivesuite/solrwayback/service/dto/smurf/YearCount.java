package dk.kb.netarchivesuite.solrwayback.service.dto.smurf;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class YearCount {

  private int year;
  private long count;
  private long total;
  
  public YearCount(){    
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

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }
  
}
