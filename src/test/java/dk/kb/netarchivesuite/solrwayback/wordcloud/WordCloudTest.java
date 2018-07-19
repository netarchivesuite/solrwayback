package dk.kb.netarchivesuite.solrwayback.wordcloud;

import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.font.scale.LinearFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.palette.ColorPalette;

public class WordCloudTest {

  public static void main(String[] args) throws Exception{
    
    final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer();
    frequencyAnalyzer.setWordFrequenciesToReturn(500);
    frequencyAnalyzer.setMinWordLength(4);
    //frequencyAnalyzer.setStopWords(loadStopWords());

    final List<WordFrequency> wordFrequencies = frequencyAnalyzer.load("/home/teg/Desktop/profil.txt"); // change path to an existing text file    
    final Dimension dimension = new Dimension(800, 600);
    final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
    wordCloud.setPadding(2);
    //wordCloud.setBackground(new PixelBoundryBackground("backgrounds/whale_small.png"));
    wordCloud.setColorPalette(new ColorPalette(new Color(0x4055F1), new Color(0x408DF1), new Color(0x40AAF1), new Color(0x40C5F1), new Color(0x40D3F1), new Color(0xFFFFFF)));
    wordCloud.setFontScalar(new LinearFontScalar(20, 100));   
    wordCloud.build(wordFrequencies);
    wordCloud.writeToFile("target/wordcloud_test.png");
  }

}
