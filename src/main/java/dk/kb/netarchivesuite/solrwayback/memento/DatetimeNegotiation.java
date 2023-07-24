package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoader;
import dk.kb.netarchivesuite.solrwayback.solr.SRequest;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class implements the Datetime Negotiation of the Memento Framework
 * as specified in <a href="https://datatracker.ietf.org/doc/html/rfc7089#section-4.1">RFC 7089</a>.
 *
 */
public class DatetimeNegotiation {
    private static final Logger log = LoggerFactory.getLogger(DatetimeNegotiation.class);

    /**
     * Implements Pattern 1.1 of the memento framework, where the URI-R represents its own URI-R and delivers the URI-M
     * through a 302 Found HTTP status code.
     * @param host a URI-R to fetch URI-M for.
     */
    public static Response redirectToDistinctMemento(String url, String host, String acceptDatetime) throws Exception {
        Long waybackdate = DateUtils.convertMementoAcceptDateTime2waybackdate(acceptDatetime);

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

    private static boolean matchDates(SolrDocument doc, Long waybackdate) {
        log.info("Comparing wayback date in solr: '{}', with waybackdate from header: '{}'", doc.getFieldValue("wayback_date"), waybackdate );
        return doc.getFieldValue("wayback_date").equals(waybackdate);
    }

    //TODO: Implement the timemap first. That would make it easier to find timegates

}
