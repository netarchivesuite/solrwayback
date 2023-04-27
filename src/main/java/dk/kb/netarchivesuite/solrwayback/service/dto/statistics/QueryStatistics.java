package dk.kb.netarchivesuite.solrwayback.service.dto.statistics;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class QueryStatistics {
    private String name;
    private double min;
    private double max;
    private double sum;
    private double count;
    private double missing;
    private double mean;
    private double sumOfSquares;
    private double stddev;

    public QueryStatistics(){
    }

    // Getters
    public String getName(){
        return name;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getMean() {
        return mean;
    }

    public double getCount() {
        return count;
    }

    public double getSum() {
        return sum;
    }

    public double getMissing() {
        return missing;
    }

    public double getStddev() {
        return stddev;
    }

    public double getSumOfSquares() {
        return sumOfSquares;
    }

    // Setters
    public void setName(String name){
        this.name = name;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    public void setCount(double count) {
        this.count = count;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public void setMissing(double missing) {
        this.missing = missing;
    }

    public void setStddev(double stddev) {
        this.stddev = stddev;
    }

    public void setSumOfSquares(double sumOfSquares) {
        this.sumOfSquares = sumOfSquares;
    }
}
