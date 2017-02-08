package dk.kb.netarchivesuite.solrwayback.service.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WeightedArcEntryDescriptor extends ArcEntryDescriptor implements Comparable<WeightedArcEntryDescriptor> {

    private double weight = 1d;

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public int compareTo(WeightedArcEntryDescriptor o) {
        return getWeight() > o.getWeight() ? -1 : getWeight() < o.getWeight() ? 1 : 0;
    }
}
