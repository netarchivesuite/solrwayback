package dk.kb.netarchivesuite.solrwayback.service.dto.graph;


import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class D3Graph {

  public List<Node> nodes = new ArrayList<Node>();
  public List<Link> links = new ArrayList<Link>();
  
  public List<Node> getNodes() {
    return nodes;
  }
  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }
  public List<Link> getLinks() {
    return links;
  }
  public void setLinks(List<Link> links) {
    this.links = links;
  }
    
}
