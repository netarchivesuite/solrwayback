package dk.kb.netarchivesuite.solrwayback.service.dto.graph;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Node {
  private String name;
  private int group;
  private int size;
  private String color;

  public Node(){
  }

  public Node(String name, int group, int size){
    this.name=name;
    this.group=group;
    this.size=size;
  }
  
  public Node(String name, int group, int size, String color){
    this.name=name;
    this.group=group;
    this.size=size;
    this.color=color;
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getGroup() {
    return group;
  }

  public void setGroup(int group) {
    this.group = group;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  
  
}