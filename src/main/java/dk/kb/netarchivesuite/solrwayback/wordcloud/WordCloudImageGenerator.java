package dk.kb.netarchivesuite.solrwayback.wordcloud;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashSet;
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


import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;

public class WordCloudImageGenerator {

  private static final Logger log = LoggerFactory.getLogger(WordCloudImageGenerator.class);
  
  public static BufferedImage wordCloudForDomain(String domain) throws Exception {
    log.info("getting wordcloud for url:"+domain);

     String text = NetarchiveSolrClient.getInstance().getTextForDomain(domain); // Only contains the required fields for this method       
     InputStream in = IOUtils.toInputStream(text, "UTF-8");
     
     final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer(); //this has the normalizers
     frequencyAnalyzer.setWordFrequenciesToReturn(200); //If more than 250 withs 800*600 resolution/scale 20/100, , most of the longer more frequent words will not be plottet!
     frequencyAnalyzer.setMinWordLength(3);
     frequencyAnalyzer.setStopWords(getStopWords()); //hard coded danish. Will move to property and text file if this stays
           
     final List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(in);
     
     final Dimension dimension = new Dimension(800, 600);
     final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
     wordCloud.setPadding(2);       
     //wordCloud.setBackground(new PixelBoundryBackground("backgrounds/whale_small.png"));
     wordCloud.setColorPalette(new ColorPalette(new Color(0x4055F1), new Color(0x408DF1), new Color(0x40AAF1), new Color(0x40C5F1), new Color(0x40D3F1), new Color(0xFFFFFF)));
     wordCloud.setFontScalar(new LinearFontScalar(20, 100));   
     wordCloud.build(wordFrequencies);
                   
     BufferedImage bufferedImage = wordCloud.getBufferedImage();
     return bufferedImage;       
  }
  
  
  //Temp solution with hardcoded danish stop words.
  public static HashSet<String> getStopWords(){
    HashSet<String> stopwords = new  HashSet<String>();
    
    stopwords.add("ad");
    stopwords.add("af");
    stopwords.add("aldrig");
    stopwords.add("alle");
    stopwords.add("alt");
    stopwords.add("anden");
    stopwords.add("andet");
    stopwords.add("andre");
    stopwords.add("at");
    stopwords.add("bare");
    stopwords.add("begge");
    stopwords.add("blev");
    stopwords.add("blive");
    stopwords.add("bliver");
    stopwords.add("da");
    stopwords.add("de");
    stopwords.add("dem");
    stopwords.add("den");
    stopwords.add("denne");
    stopwords.add("der");
    stopwords.add("deres");
    stopwords.add("det");
    stopwords.add("dette");
    stopwords.add("dig");
    stopwords.add("din");
    stopwords.add("dine");
    stopwords.add("disse");
    stopwords.add("dit");
    stopwords.add("dog");
    stopwords.add("du");
    stopwords.add("efter");
    stopwords.add("ej");
    stopwords.add("eller");
    stopwords.add("en");
    stopwords.add("end");
    stopwords.add("ene");
    stopwords.add("eneste");
    stopwords.add("enhver");
    stopwords.add("er");
    stopwords.add("et");    
    stopwords.add("fem");
    stopwords.add("fik");
    stopwords.add("fire");
    stopwords.add("flere");
    stopwords.add("fleste");
    stopwords.add("for");
    stopwords.add("fordi");
    stopwords.add("forrige");
    stopwords.add("fra");
    stopwords.add("få");
    stopwords.add("får");
    stopwords.add("før");
    stopwords.add("god");
    stopwords.add("godt");
    stopwords.add("ham");
    stopwords.add("han");
    stopwords.add("hans");
    stopwords.add("har");
    stopwords.add("havde");
    stopwords.add("have");
    stopwords.add("hej");
    stopwords.add("helt");
    stopwords.add("hende");
    stopwords.add("hendes");
    stopwords.add("her");
    stopwords.add("hos");
    stopwords.add("hun");
    stopwords.add("hvad");
    stopwords.add("hvem");
    stopwords.add("hver");
    stopwords.add("hvilken");
    stopwords.add("hvis");
    stopwords.add("hvor");
    stopwords.add("hvordan");
    stopwords.add("hvorfor");
    stopwords.add("hvornår");
    stopwords.add("i");
    stopwords.add("ikke");
    stopwords.add("ind");
    stopwords.add("ingen");
    stopwords.add("intet");
    stopwords.add("ja");
    stopwords.add("jeg");
    stopwords.add("jer");
    stopwords.add("jeres");
    stopwords.add("jo");
    stopwords.add("kan");
    stopwords.add("kom");
    stopwords.add("komme");
    stopwords.add("kommer");
    stopwords.add("kun");
    stopwords.add("kunne");
    stopwords.add("lad");
    stopwords.add("lav");
    stopwords.add("lidt");
    stopwords.add("lige");
    stopwords.add("lille");
    stopwords.add("man");
    stopwords.add("mange");
    stopwords.add("med");
    stopwords.add("meget");
    stopwords.add("men");
    stopwords.add("mens");
    stopwords.add("mere");
    stopwords.add("mig");
    stopwords.add("min");
    stopwords.add("mine");
    stopwords.add("mit");
    stopwords.add("mod");
    stopwords.add("må");
    stopwords.add("ned");
    stopwords.add("nej");
    stopwords.add("ni");
    stopwords.add("nogen");
    stopwords.add("noget");
    stopwords.add("nogle");
    stopwords.add("nu");
    stopwords.add("ny");
    stopwords.add("nyt");
    stopwords.add("når");
    stopwords.add("nær");
    stopwords.add("næste");
    stopwords.add("næsten");
    stopwords.add("og");
    stopwords.add("også");
    stopwords.add("okay");
    stopwords.add("om");
    stopwords.add("op");
    stopwords.add("os");
    stopwords.add("otte");
    stopwords.add("over");
    stopwords.add("på");
    stopwords.add("se");
    stopwords.add("seks");
    stopwords.add("selv");
    stopwords.add("ser");
    stopwords.add("ses");
    stopwords.add("sig");
    stopwords.add("sige");
    stopwords.add("sin");
    stopwords.add("sine");
    stopwords.add("sit");
    stopwords.add("skal");
    stopwords.add("skulle");
    stopwords.add("som");
    stopwords.add("stor");
    stopwords.add("store");
    stopwords.add("syv");
    stopwords.add("så");
    stopwords.add("sådan");
    stopwords.add("tag");
    stopwords.add("tage");
    stopwords.add("thi");
    stopwords.add("ti");
    stopwords.add("til");
    stopwords.add("to");
    stopwords.add("tre");
    stopwords.add("ud");
    stopwords.add("under");
    stopwords.add("var");
    stopwords.add("ved");
    stopwords.add("vi");
    stopwords.add("vil");
    stopwords.add("ville");
    stopwords.add("vor");
    stopwords.add("vores");
    stopwords.add("være");
    stopwords.add("været");
    stopwords.add("alene");
    stopwords.add("allerede");
    stopwords.add("alligevel");
    stopwords.add("altid");
    stopwords.add("bag");
    stopwords.add("blandt");
    stopwords.add("burde");
    stopwords.add("bør");
    stopwords.add("dens");
    stopwords.add("derefter");
    stopwords.add("derfor");
    stopwords.add("derfra");
    stopwords.add("deri");
    stopwords.add("dermed");
    stopwords.add("derpå");
    stopwords.add("derved");
    stopwords.add("egen");
    stopwords.add("ellers");
    stopwords.add("endnu");
    stopwords.add("ens");
    stopwords.add("enten");
    stopwords.add("flest");
    stopwords.add("foran");
    stopwords.add("først");
    stopwords.add("gennem");
    stopwords.add("gjorde");
    stopwords.add("gjort");
    stopwords.add("gør");
    stopwords.add("gøre");
    stopwords.add("gørende");
    stopwords.add("hel");
    stopwords.add("heller");
    stopwords.add("hen");
    stopwords.add("henover");
    stopwords.add("herefter");
    stopwords.add("heri");
    stopwords.add("hermed");
    stopwords.add("herpå");
    stopwords.add("hvilke");
    stopwords.add("hvilkes");
    stopwords.add("hvorefter");
    stopwords.add("hvorfra");
    stopwords.add("hvorhen");
    stopwords.add("hvori");
    stopwords.add("hvorimod");
    stopwords.add("hvorved");
    stopwords.add("igen");
    stopwords.add("igennem");
    stopwords.add("imellem");
    stopwords.add("imens");
    stopwords.add("imod");
    stopwords.add("indtil");
    stopwords.add("langs");
    stopwords.add("lave");
    stopwords.add("lavet");
    stopwords.add("ligesom");
    stopwords.add("længere");
    stopwords.add("mellem");
    stopwords.add("mest");
    stopwords.add("mindre");
    stopwords.add("mindst");
    stopwords.add("måske");
    stopwords.add("nemlig");
    stopwords.add("nogensinde");
    stopwords.add("nok");
    stopwords.add("omkring");
    stopwords.add("overalt");
    stopwords.add("samme");
    stopwords.add("sammen");
    stopwords.add("selvom");
    stopwords.add("senere");
    stopwords.add("siden");
    stopwords.add("stadig");
    stopwords.add("synes");
    stopwords.add("syntes");
    stopwords.add("således");
    stopwords.add("temmelig");
    stopwords.add("tidligere");
    stopwords.add("tilbage");
    stopwords.add("tit");
    stopwords.add("uden");
    stopwords.add("udover");
    stopwords.add("undtagen");
    stopwords.add("via");
    stopwords.add("vore");
    stopwords.add("vær");
    stopwords.add("øvrigt");
    
       
    return stopwords;
    
    
    
  }
  
  
}
