package dk.kb.netarchivesuite.solrwayback.normalise;

import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;

public class NormalisationMinimal {
    private static Log log = LogFactory.getLog( NormalisationLegacy.class );

    private static AggressiveUrlCanonicalizer canon = new AggressiveUrlCanonicalizer();

 
    public static String canonicaliseHost(String host) throws URIException {
        return canon.urlStringToKey(host.trim()).replace("/", "");
    }

    public static String canonicaliseURL(String url) {
        return canonicaliseURL(url, true, true);
    }

    
    public static String resolveRelative(String url, String relative) throws IllegalArgumentException {
        return resolveRelative(url, relative, true);
    }
   
    public static String resolveRelative(String url, String relative, boolean normalise) throws IllegalArgumentException {
        try {
            URL rurl = new URL(url);
            String resolved = new URL(rurl, relative).toString();
            return normalise ? canonicaliseURL(resolved) : resolved;
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format(
                    "Unable to resolve '%s' relative to '%s'", relative, url), e);
        }
    }

    public static String canonicaliseURL(String url, boolean allowHighOrder, boolean createUnambiguous) {
       //DO nothing
        return url;
    }
}

    
    
