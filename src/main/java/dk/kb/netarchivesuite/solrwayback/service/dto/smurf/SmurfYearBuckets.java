package dk.kb.netarchivesuite.solrwayback.service.dto.smurf;


import java.util.ArrayList;


import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SmurfYearBuckets {

  
  private ArrayList<Double> yearCountPercent = new  ArrayList<Double>();  
  private ArrayList<YearCount> yearCountsTotal= new ArrayList<YearCount>();
  private int baseYear=1990; 
  private boolean emptyResult=true;
  
  public SmurfYearBuckets(){    
  }

  
  public ArrayList<Double> getYearCountPercent() {
    return yearCountPercent;
  }


  public void setYearCountPercent(ArrayList<Double> yearCountPercent) {
    this.yearCountPercent = yearCountPercent;
  }


  public ArrayList<YearCount> getYearCountsTotal() {
    return yearCountsTotal;
  }


  public void setYearCountsTotal(ArrayList<YearCount> yearCountsTotal) {
    this.yearCountsTotal = yearCountsTotal;
  }


  public int getBaseYear() {
    return baseYear;
  }

  public void setBaseYear(int baseYear) {
    this.baseYear = baseYear;
  }


  public boolean isEmptyResult() {
    return emptyResult;
  }


  public void setEmptyResult(boolean emptyResult) {
    this.emptyResult = emptyResult;
  }
  
}
