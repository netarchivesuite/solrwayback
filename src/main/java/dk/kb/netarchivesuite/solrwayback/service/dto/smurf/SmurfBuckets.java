package dk.kb.netarchivesuite.solrwayback.service.dto.smurf;


import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SmurfBuckets {


  private List<Double> countPercent = new  ArrayList<>();  
  private List<DateCount> countsTotal= new ArrayList<>();
  private boolean emptyResult=true;

  public SmurfBuckets(){
  }


  public List<Double> getCountPercent() {
    return countPercent;
  }


  public void setCountPercent(List<Double> countPercent) {
    this.countPercent = countPercent;
  }


  public List<DateCount> getCountsTotal() {
    return countsTotal;
  }


  public void setCountsTotal(List<DateCount> countsTotal) {
    this.countsTotal = countsTotal;
  }


public boolean isEmptyResult() {
    return emptyResult;
  }


  public void setEmptyResult(boolean emptyResult) {
    this.emptyResult = emptyResult;
  }

}