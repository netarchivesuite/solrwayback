package dk.kb.netarchivesuite.solrwayback.service.dto.statistics;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

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

    public QueryStatistics(){
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


    /**
     * Set all standard values from a FieldStatsInfo
     * @param stat to convert to QueryStatistics DTO
     */
    public void setAllStandardValuesFromSolrFieldStatsInfo(FieldStatsInfo stat){
        this.name = stat.getName();
        this.min = stat.getMin();
        this.max= stat.getMax();
        this.sum= stat.getSum();
        this.count= stat.getCount().longValue();
        this.missing= stat.getMissing().longValue();
        this.mean= stat.getMean();
        this.sumOfSquares= stat.getSumOfSquares();
        this.stddev= stat.getStddev();

    }


}
