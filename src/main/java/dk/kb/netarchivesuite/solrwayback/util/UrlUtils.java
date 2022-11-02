package dk.kb.netarchivesuite.solrwayback.util;

import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;

import java.net.IDN;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {
    public static void main(String[] args){

        //System.out.println(isUrlWithDomain("http://Portal_gfx/KL/farvepakker/topmenu/topmenu_markering_groen_mBo.gif"));
        //System.out.println(getDomainFromWebApiParameters("http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285"));
    }
    /*
     * Must start with http:// and have a domain(must have . as one of the characters)
     *
     */
    public static boolean isUrlWithDomain(String url){
        if (!(url.startsWith("http://") || url.startsWith("https://")) ){
            return false;
        }
        String[] tokens= url.split("/");
        if (tokens.length < 3){
            return false;
        }

        //String domain = tokens[2];

        return true;
    }

    /*
     * Url must be legal and have a domain, also remove port.
     */
    public static String getDomain(String url){
        String[] tokens= url.split("/");
        String domainWithPort =  tokens[2];
        int portIndex = domainWithPort.indexOf(":");
        if (portIndex == -1){
            return domainWithPort;
        }
        else{
            return domainWithPort.substring(0, portIndex);
        }
    }

    //http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285
    //return kl.dk
    public static String getDomainFromWebApiParameters(String fullUrl){
        int dataStart=fullUrl.indexOf("/web/");
        String waybackDataObject = fullUrl.substring(dataStart+5);
        int indexFirstSlash = waybackDataObject.indexOf("/");
        String url = waybackDataObject.substring(indexFirstSlash+1);
        return getDomain(url);
    }


    //http://teg-desktop.sb.statsbiblioteket.dk:8080/solrwayback/services/web/20071221033234/http://kl.dk/ncms.aspx?id=11fb172c-dbf2-4c58-bdf7-30f9cdfd4d95&menuid=361285
    //return 20071221033234
    public static String getCrawltimeFromWebApiParameters(String fullUrl){
        int dataStart=fullUrl.indexOf("/web/");
        String waybackDataObject = fullUrl.substring(dataStart+5);
        int indexFirstSlash = waybackDataObject.indexOf("/");
        String crawlTime = waybackDataObject.substring(0,indexFirstSlash);
        return crawlTime;

    }


    //Extract crawltime and url (after crawltime) from and url
    //Example: https://solrwb-test.kb.dk:4000/solrwayback/services/web/20110225142047/http://www.bt.dk/debat
    //Return [20110225142047],[http://www.bt.dk/debat]
    //It pattern is not found return null;
    public static String[] getCrawltimeAndUrlFromWebProxyLeak(String fullUrl){
        Pattern p = Pattern.compile("^.+?/(\\d{14})/(.+)$");
        Matcher matcher1 = p.matcher(fullUrl);
        if (matcher1.matches()) {
            String[]  result= new String[2];
            result[0]=matcher1.group(1);
            result[1]=matcher1.group(2);
            return result;


        }else {
            return null;
        }

    }


    /*
     * last path element wit query params as well.
     */
    public static String getResourceNameFromWebApiParameters(String url){
        String[] tokens= url.split("/");
        return tokens[tokens.length-1];

    }

    public static String punyCodeAndNormaliseUrl(String url) throws Exception {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new Exception("Url not starting with http:// or https://");
        }

        URL uri = new URL(url);
        String hostName = uri.getHost();
        String hostNameEncoded = IDN.toASCII(hostName);

        String path = uri.getPath();
        if ("".equals(path)) {
            path = "/";
        }
        String urlQueryPath = uri.getQuery();
        String urlPunied = null;
        if (urlQueryPath == null) {
             urlPunied = "http://" + hostNameEncoded + path;
        }
        else {
            urlPunied = "http://" + hostNameEncoded + path +"?"+ urlQueryPath;
        }
        String urlPuniedAndNormalized = Normalisation.canonicaliseURL(urlPunied);
        return urlPuniedAndNormalized;
    }
}

