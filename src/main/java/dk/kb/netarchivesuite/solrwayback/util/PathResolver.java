package dk.kb.netarchivesuite.solrwayback.util;

import dk.kb.netarchivesuite.solrwayback.facade.Facade;
import dk.kb.netarchivesuite.solrwayback.normalise.Normalisation;
import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.SolrWaybackResource;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDoc;
import dk.kb.netarchivesuite.solrwayback.service.exception.NotFoundServiceException;
import dk.kb.netarchivesuite.solrwayback.service.exception.SolrWaybackServiceException;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;

public class PathResolver {
    private static final Logger log = LoggerFactory.getLogger(PathResolver.class);

    public static URI mementoAPIResolver(String basePath, //should be: "/memento/"
                                              UriInfo uriInfo, HttpServletRequest httpRequest,
                                              String path ) throws URISyntaxException {

        log.debug("{} called with data:{}", basePath, path);
        String fullUrl = uriInfo.getRequestUri().toString();
        log.info("fullUrl: '{}'", fullUrl);

        int dataStart = fullUrl.indexOf(basePath);
        String url = fullUrl.substring(dataStart + basePath.length());
        log.info("url: " + url);

        url = checkForSingleSlash(url);
        String newUrl = replaceEncoding(url);

        String finalUrl = Normalisation.canonicaliseURL(newUrl);
        URI uri = new URI(finalUrl);
        log.info("new url:" + finalUrl);

        return uri;
    }

    public static Response waybackAPIResolverHelper(
            SolrWaybackResource resource,
            String basePath, // "/lenient/web/" or "/web/"
            UriInfo uriInfo, HttpServletRequest httpRequest, String path, Boolean lenient) throws SolrWaybackServiceException {

        //SolrWaybackResource resource = new SolrWaybackResource();
        try {
            //For some reason the var regexp does not work with comma (;) and other characters. So I have to grab the full url from uriInfo
            log.debug("{} called with data:{} lenient:{}", basePath, path, lenient);
            String fullUrl = uriInfo.getRequestUri().toString();
            //   log.info("full url:"+fullUrl);

            int dataStart = fullUrl.indexOf(basePath);

            String waybackDataObject = fullUrl.substring(dataStart + basePath.length());
            //log.info("Waybackdata object:"+waybackDataObject);

            int indexFirstSlash = waybackDataObject.indexOf("/");

            String waybackDate = waybackDataObject.substring(0, indexFirstSlash);
            String url = waybackDataObject.substring(indexFirstSlash + 1);

            url = checkForSingleSlash(url);

            //Validate this is a URL with domain (can be releative leak).
            //etc. http://images/horse.png.
            //use referer to match the correct url

            String solrDate = DateUtils.convertWaybackDate2SolrDate(waybackDate);

            boolean urlOK = UrlUtils.isUrlWithDomain(url);
            if (!urlOK) {
                String refererUrl = httpRequest.getHeader("referer");
                log.info("url not with domain:" + url + " referer:" + refererUrl);
                IndexDoc doc = Facade.matchRelativeUrlForDomain(refererUrl, url, solrDate);
                return resource.downloadRaw(doc.getSource_file_path(), doc.getOffset());
            }

            //log.info("solrDate="+solrDate +" , url="+url);
            IndexDoc doc = NetarchiveSolrClient.getInstance().findClosestHarvestTimeForUrl(url, solrDate);


            if (doc == null) {
                log.info("Url has never been harvested:" + url);
                throw new NotFoundServiceException("Url has never been harvested:" + url);
            }


            //THIS BLOCK WILL FORWARD URLS TO MATCH CRAWLTIME FOR HTML PLAYBACK
            // html forward to a new request so date in url will show the true crawldate of the document. Avoid having 2020 in url with the page is from 2021 etc.
            String htmlPageCrawlDate = DateUtils.convertUtcDate2WaybackDate(doc.getCrawlDate());
            if ("html".equalsIgnoreCase(doc.getContentTypeNorm()) && !htmlPageCrawlDate.equals(waybackDate)) {
                String newUrl = PropertiesLoader.WAYBACK_BASEURL + "services" + basePath + htmlPageCrawlDate + "/" + url;
                log.info("Forwarding html view to a url where crawldate matches html crawltime. url crawltime:" + htmlPageCrawlDate + " true crawl:" + htmlPageCrawlDate);
                newUrl = replaceEncoding(newUrl);

                URI uri = new URI(newUrl);
                log.info("new url:" + newUrl);
                return Response.seeOther(uri).build(); //Jersey way to forward response.
            }
            //END BLOCK

            //log.debug("return viewImpl for type:"+doc.getMimeType() +" and url:"+doc.getUrl());
            Response viewImpl = resource.viewImpl(doc.getSource_file_path(), doc.getOffset(), true, lenient);

            return viewImpl;
        } catch (Exception e) {
            throw resource.handleServiceExceptions(e);
        }

    }

    /**
     * Replace some encoding in URL.
     * @param url to replace encoding in.
     * @return the correct URL.
     */
    private static String replaceEncoding(String url) {
        url = url.replace("|", "%7C");//For some unknown reason Java does not accept |, must encode.
        url = url.replace("%2f", "/"); // or url will not match Rest pattern for method. (not clear why)
        url = url.replace("%2F", "/"); // or url will not match Rest pattern for method. (not clear why)
        return url;
    }

    /**
     * Checks for missing slash in http:// declaration.
     * @param url to check for slash.
     * @return url with correct number of slashes.
     */
    private static String checkForSingleSlash(String url) {
        //Stupid fix, some webservices makes parameter http:// into http:/  ( removes a slash)
        if (url.startsWith("http:/") && !url.startsWith("http://")) {
            url = url.replaceFirst("http:/", "http://");
        }
        if (url.startsWith("https:/") && !url.startsWith("https://")) {
            url = url.replaceFirst("https:/", "https://");
        }
        return url;
    }
}
