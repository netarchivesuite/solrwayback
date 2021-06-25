package dk.kb.netarchivesuite.solrwayback.wordcloud;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.font.scale.LinearFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.palette.ColorPalette;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.service.dto.WordCloudWordAndCount;


public class WordCloudImageGenerator {

  private static final Logger log = LoggerFactory.getLogger(WordCloudImageGenerator.class);
  
  public static BufferedImage wordCloudForDomain(String text) throws Exception {
    InputStream in = IOUtils.toInputStream(text, "UTF-8");
    
    final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer(); //this has the normalizers, but too many bugs in them. Write one that both lower case, remove non letters and trim's corrext
    frequencyAnalyzer.setWordFrequenciesToReturn(250); //If more than 250 withs 800*600 resolution/scale 20/100, , most of the longer more frequent words will not be plottet!
    frequencyAnalyzer.setMinWordLength(4); 
         
    List<String> stopWords= PropertiesLoaderWeb.WORDCLOUD_STOPWORDS;
    frequencyAnalyzer.setStopWords(stopWords); //Configured in solrwaybackweb.properties
          
    final List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(in);
    
    final Dimension dimension = new Dimension(800, 600);
    final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
    wordCloud.setPadding(2);       
    //wordCloud.setBackground(new PixelBoundryBackground("backgrounds/whale_small.png"));
    wordCloud.setColorPalette(new ColorPalette(new Color(0x4055F1), new Color(0x408DF1), new Color(0x40AAF1), new Color(0x40C5F1), new Color(0x40D3F1), new Color(0xFFFFFF)));
    wordCloud.setFontScalar(new LinearFontScalar(14, 80));   
    wordCloud.build(wordFrequencies);
                  
    BufferedImage bufferedImage = wordCloud.getBufferedImage();
    return bufferedImage;       
 }
  
 
  
  
  public static List<WordCloudWordAndCount> wordCloudWordWithCount(String text) throws Exception {
    log.info("generating wordcloud from text size:"+text.length());
    InputStream in = IOUtils.toInputStream(text, "UTF-8");
    
    final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer(); //this has the normalizers, but too many bugs in them. Write one that both lower case, remove non letters and trim's corrext
    frequencyAnalyzer.setWordFrequenciesToReturn(250); //If more than 250 withs 800*600 resolution/scale 20/100, , most of the longer more frequent words will not be plottet!
    frequencyAnalyzer.setMinWordLength(4); 
         
    List<String> stopWords= PropertiesLoaderWeb.WORDCLOUD_STOPWORDS;
    frequencyAnalyzer.setStopWords(stopWords); //Configured in solrwaybackweb.properties
          
    final List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(in);
    Collections.sort(wordFrequencies);
    //need to wrap in DTO for service
    List<WordCloudWordAndCount> result = new ArrayList<WordCloudWordAndCount>();
    
    for (WordFrequency current : wordFrequencies) {
      log.info(current.getWord());
      WordCloudWordAndCount item = new WordCloudWordAndCount(current.getWord(),current.getFrequency());
      result.add(item);
    }
        
    return result;       
 }
  
}
