package dk.kb.netarchivesuite.solrwayback.service.dto;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SearchResult {

    private long numberOfResults=0;    
    private List<IndexDoc>  results = new ArrayList<IndexDoc>();
    public long getNumberOfResults() {
        return numberOfResults;
    }
    public void setNumberOfResults(long numberOfResults) {
        this.numberOfResults = numberOfResults;
    }
    public List<IndexDoc> getResults() {
        return results;
    }
    public void setResults(List<IndexDoc> results) {
        this.results = results;
    } 
    
    
    
    
}
