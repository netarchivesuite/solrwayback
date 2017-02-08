package dk.kb.netarchivesuite.solrwayback.parsers;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HTMLParser;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class HtmlParser {


    public static  void main(String[] args) throws Exception{

        String html="<?xml version=\"1.1\" encoding=\"iso-8859-1\"?>"+
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\">"+
                "<head>"+
                "   <title>kimse.rovfisk.dk/katte/</title>"+
                "   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\" />"+         
                "   <link rel=\"stylesheet\" href=\"/style.css\"  type=\"text/css\" media=\"screen\" />"+
                " <link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\"/rss2.php\" />"+
                "</head>"+
                "<body>"+
                "<a class=\"toplink\" href=\"/\">kimse.rovfisk.dk  </a><a class=\"toplink\" href=\"/katte/\">katte / </a><br /><br /><table cellspacing=\"8\"><tr><td></td><td class=\"itemw\"><a href=\"/katte/?browse=DSC00175.JPG\"><img class=\"lo\" src=\"/cache/katte/DSC00175.JPG\" /></a></td>"+
                "<td></td><td class=\"itemw\"><a href=\"/katte/?browse=DSC00209.JPG\"><img class=\"lo\" src=\"/cache/katte/DSC00209.JPG\" /></a></td>"+
                "</table><br />  </body>"+
                "</html>";



        ArrayList<String> imageUrls = getImageUrls("http://test.dk/foo/bar?test=true", html);
        System.out.println(imageUrls);

    }


    public static ArrayList<String> getImageUrls(String url,String htmlString) throws Exception{

        HashSet<String> imagesSet = new HashSet<String>();  // To remove duplicates

        URL urlType = new URL(url);
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
        StringWebResponse response = new StringWebResponse(htmlString, urlType);
        WebClient client = new WebClient();        
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setAppletEnabled(false);
        client.getOptions().setRedirectEnabled(false);


        HtmlPage page = HTMLParser.parseHtml(response, client.getCurrentWindow());


        List<?> imageList = page.getByXPath("//img");
        ListIterator li = imageList.listIterator();

        while (li.hasNext() ) {
            try{

                HtmlImage image = (HtmlImage)li.next();

                String imgLink=image.getSrcAttribute();

                URI uri = new URI(url);
                URI relative = uri.resolve(new URI(imgLink));
                imagesSet.add(relative.toString());       
            }
            catch(Exception e){
                //ignore
            }
        }

        return new ArrayList<String>(imagesSet);
    }
}
