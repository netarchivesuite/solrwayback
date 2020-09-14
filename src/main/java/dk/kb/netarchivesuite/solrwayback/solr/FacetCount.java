package dk.kb.netarchivesuite.solrwayback.solr;

public class FacetCount {
private long count;
private String value;


public FacetCount(){
}


public long getCount() {
  return count;
}


public void setCount(long count) {
  this.count = count;
}


public String getValue() {
  return value;
}


public void setValue(String value) {
  this.value = value;
}


@Override
public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
}


@Override
public boolean equals(Object obj) {
    if (this == obj)
        return true;
    if (obj == null)
        return false;
    if (getClass() != obj.getClass())
        return false;
    FacetCount other = (FacetCount) obj;
    if (value == null) {
        if (other.value != null)
            return false;
    } else if (!value.equals(other.value))
        return false;
    return true;
}
  


}
