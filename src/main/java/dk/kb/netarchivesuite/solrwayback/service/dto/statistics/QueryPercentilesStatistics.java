package dk.kb.netarchivesuite.solrwayback.service.dto.statistics;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@XmlRootElement
public class QueryPercentilesStatistics {
    private String name;
    private Map<String, Double> percentiles;

    public QueryPercentilesStatistics(){
    }

    // Getters
    public String getName(){
        return name;
    }

    public Map<String, Double> getPercentiles(){
        return percentiles;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setPercentiles(Map<String, Double> percentiles) {
        this.percentiles = percentiles;
    }
}
