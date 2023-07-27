package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.service.dto.IndexDocShort;
import dk.kb.netarchivesuite.solrwayback.solr.NetarchiveSolrClient;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


// TODO: Not sure that Host header is used correctly. It should point to the archive that hosts the material, not the original host of the website.

/**
 * This class implements the Datetime Negotiation of the Memento Framework
 * as specified in <a href="https://datatracker.ietf.org/doc/html/rfc7089#section-4.1">RFC 7089</a>.
 *
 */
public class DatetimeNegotiation {
    private static final Logger log = LoggerFactory.getLogger(DatetimeNegotiation.class);

    /**
     * Implements Pattern 1.1 of the memento framework, where the URI-R represents its own URI-G and delivers the URI-M
     * through a 302 Found HTTP status code. This is only usefull if the URI-R resides at the same server as the URI-M.
     * This is not the case for webarchives.
     * @param host a URI-R to fetch URI-M for.
     */
    public static Response redirectToDistinctMemento(String url, String host, String acceptDatetime) throws Exception {
        Long waybackdate = DateUtils.convertMementoAcceptDateTime2Waybackdate(acceptDatetime);

        log.info("Extracted host: '{}' and accept-datetime '{}' headers from http request",
                    host, acceptDatetime);

        // TODO: This stream is not really used. Maybe this endpoint could just try to resolve the direct url instead of doing a lookup
        List<SolrDocument> docs = SRequest.builder().query("url:" + url)
                                    .fields("url", "url_norm", "wayback_date")
                                    .stream()
                                    .filter(doc -> matchDates(doc, waybackdate))
                                    .collect(Collectors.toList());

        return Response.status(302)
                .header("Location", PropertiesLoader.WAYBACK_BASEURL + "services/web/" + waybackdate + "/" + url)
                .header("Vary", acceptDatetime)
                .header("Link", "<"+url+">; rel=\"original timegate\"")
                .build();
    }

    /**
     * Implements Pattern 2.2 of the memento framework.
     * @param url
     * @param host
     * @param acceptDatetime
     * @return
     * @throws ParseException
     */
    public static Response remoteTimeGateForOriginalResource(String url, String host, String acceptDatetime) throws ParseException {
        //TODO: Create an actual memento2solrdate converter and vice-versa
        Long waybackdate = DateUtils.convertMementoAcceptDateTime2Waybackdate(acceptDatetime);
        String solrDate = DateUtils.convertWaybackDate2SolrDate(String.valueOf(waybackdate));
        log.info("Extracted host: '{}' and accept-datetime '{}' headers from http request",
                host, acceptDatetime);

        Stream<IndexDocShort> result = NetarchiveSolrClient.getInstance().findNearestHarvestTimeForSingleUrlFewFields(url, solrDate);
        return Response.status(200)
                .entity(result)
                .build();
    }

    private static boolean matchDates(SolrDocument doc, Long waybackdate) {
        log.info("Comparing wayback date in solr: '{}', with waybackdate from header: '{}'", doc.getFieldValue("wayback_date"), waybackdate );
        return doc.getFieldValue("wayback_date").equals(waybackdate);
    }

    //TODO: Implement the timemap first. That would make it easier to find timegates

}
