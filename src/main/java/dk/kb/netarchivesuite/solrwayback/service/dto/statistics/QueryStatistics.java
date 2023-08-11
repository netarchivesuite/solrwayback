package dk.kb.netarchivesuite.solrwayback.service.dto.statistics;

import org.apache.solr.client.solrj.response.FieldStatsInfo;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class QueryStatistics {
    private String name;
    private Object min;
    private Object max;
    private Object sum;
    private Long count;
    private Long missing;
    private Object mean;
    private Double sumOfSquares;
    private Double stddev;

    public QueryStatistics(FieldStatsInfo fieldStatsInfo){
        this.name = fieldStatsInfo.getName();
        this.min = fieldStatsInfo.getMin();
        this.max= fieldStatsInfo.getMax();
        this.sum= fieldStatsInfo.getSum();
        this.count= fieldStatsInfo.getCount().longValue();
        this.missing= fieldStatsInfo.getMissing().longValue();
        this.mean= fieldStatsInfo.getMean();
        this.sumOfSquares= fieldStatsInfo.getSumOfSquares();
        this.stddev= fieldStatsInfo.getStddev();
    }

    // Getters
    public String getName(){
        return name;
    }

    public Object getMin() {
        return min;
    }

    public Object getMax() {
        return max;
    }

    public Object getMean() {
        return mean;
    }

    public Long getCount() {
        return count;
    }

    public Object getSum() {
        return sum;
    }

    public Long getMissing() {
        return missing;
    }

    public Double getStddev() {
        return stddev;
    }

    public Double getSumOfSquares() {
        return sumOfSquares;
    }

    // Setters
    public void setName(String name){
        this.name = name;
    }

    public void setMin(Object min) {
        this.min = min;
    }

    public void setMax(Object max) {
        this.max = max;
    }

    public void setSum(Object sum) {
        this.sum = sum;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public void setMean(Object mean) {
        this.mean = mean;
    }

    public void setMissing(Long missing) {
        this.missing = missing;
    }

    public void setStddev(Double stddev) {
        this.stddev = stddev;
    }

    public void setSumOfSquares(Double sumOfSquares) {
        this.sumOfSquares = sumOfSquares;
    }

}
