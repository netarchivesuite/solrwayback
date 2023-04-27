package dk.kb.netarchivesuite.solrwayback.service.dto.statistics;

import org.apache.solr.client.solrj.response.FieldStatsInfo;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement
public class QueryPercentilesStatistics {
    private String name;
    private Map<Double, Double> percentiles;

    public QueryPercentilesStatistics(){
    }

    // Getters
    public String getName(){
        return name;
    }

    public Map<Double, Double> getPercentiles(){
        return percentiles;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setPercentiles(Map<Double, Double> percentiles) {
        this.percentiles = percentiles;
    }

    public void setAllValuesFromFieldStatsInfo(FieldStatsInfo fieldStatsInfo){
        this.name= fieldStatsInfo.getName();
        this.percentiles= fieldStatsInfo.getPercentiles();
    }
}
