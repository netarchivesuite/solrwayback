package dk.kb.netarchivesuite.solrwayback.service.dto.graph;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Link {

  private int source;
  private int target;
  private int weight = 1;
  
  public Link(){
  }

  public Link(int source, int target, int weight){
    this.source=source;
    this.target=target;
    this.weight=weight;    
  }
  
  public int getSource() {
    return source;
  }

  public void setSource(int source) {
    this.source = source;
  }

  public int getTarget() {
    return target;
  }

  public void setTarget(int target) {
    this.target = target;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }
  
  
  
  
  
}
