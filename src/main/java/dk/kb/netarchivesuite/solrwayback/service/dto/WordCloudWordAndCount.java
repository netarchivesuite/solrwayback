package dk.kb.netarchivesuite.solrwayback.service.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class WordCloudWordAndCount {

  private int count;
  private String word;

  public WordCloudWordAndCount() {
  }

  public WordCloudWordAndCount(String word, int count) {
   this.word=word;
   this.count=count;
  }


  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public String getWord() {
    return word;
  }

  public void setWord(String word) {
    this.word = word;
  }

}
