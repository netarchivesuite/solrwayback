package dk.kb.netarchivesuite.solrwayback.memento;

import dk.kb.netarchivesuite.solrwayback.properties.PropertiesLoaderWeb;
import dk.kb.netarchivesuite.solrwayback.util.DateUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class TimeMapAsLink {
    private static final Logger log = LoggerFactory.getLogger(TimeMapAsLink.class);

    /**
     * Writes a timemap (URI-T) for a URI-R to an outputstream in the link-type format.
     * @param originalResource  URI-R to create URI-T from.
     * @param output            Stream which the output is delivered to.
     */
    static void getTimeMapAsLinkFormat(URI originalResource, OutputStream output, Integer pageNumber) throws IOException {
        MementoMetadata metadata = new MementoMetadata();

        long count = TimeMap.getDocStreamAndUpdateDatesForFirstAndLastMemento(originalResource, metadata)
                .count();
        log.info("First stream has been consumed. '{}' documents have been streamed.", count);

        if (count < TimeMap.PAGING_LIMIT){
            metadata.setTimeMapHeadForLinkFormat(originalResource.toString(), 0);
            log.info("Creating timemap of '{}' entries, with dates in range from '{}' to '{}' in link-format.",
                    count, metadata.getFirstMemento(), metadata.getLastMemento());

            output.write(metadata.getTimeMapHead().getBytes());

            AtomicLong iterator = new AtomicLong(1);
            TimeMap.getMementoStream(originalResource)
                    .map(doc -> createMementoInLinkFormat(doc, iterator, count))
                    .forEach(s -> writeStringSafe(s, output));
        } else {
            if (pageNumber == null || (long) TimeMap.RESULTS_PER_PAGE * pageNumber > count){
                pageNumber = 1;
                log.info("Set page number to: " + pageNumber);
            }
            metadata.setTimeMapHeadForLinkFormat(originalResource.toString(), pageNumber);
            log.info("Creating paged timemaps of '{}' entries, with dates in range from '{}' to '{}' in link-format.",
                    count, metadata.getFirstMemento(), metadata.getLastMemento());
            Stream<SolrDocument> mementoStream = TimeMap.getMementoStream(originalResource);

            getLinkFormatPagedStreamingOutput(originalResource, metadata, mementoStream, count, pageNumber, output);
        }

    }

    /**
     * Creates a paged timemap formatted as application/link-type and writes it to the outputstream given to the method.
     * @param originalResource  URI-R to write timemap for.
     * @param metadata          object containing the beginning of the timemap.
     * @param mementoStream     stream containing all mementos from solr for given resource.
     * @param countOfMementos   total amount of mementos for resource.
     * @param pageNumber        the given page to return.
     * @param output            outputstream, which the result is written to.
     */
    private static void getLinkFormatPagedStreamingOutput(URI originalResource, MementoMetadata metadata,
                                                          Stream<SolrDocument> mementoStream, long countOfMementos,
                                                          Integer pageNumber, OutputStream output) throws IOException {

        //Page response
        TimeMap.Page<SolrDocument> pageOfResults = TimeMap.getPage(mementoStream, pageNumber, countOfMementos);

        output.write(metadata.getTimeMapHead().getBytes());

        AtomicLong iterator = new AtomicLong(((long) pageNumber * TimeMap.RESULTS_PER_PAGE - TimeMap.RESULTS_PER_PAGE + 1) );
        pageOfResults.items
                .map(doc -> createMementoInLinkFormat(doc, iterator, countOfMementos) )
                .forEach(s -> writeStringSafe(s, output));

        if (pageNumber - 1 != 0 ) {
            writeLinkTypePreviousPage(originalResource, output, pageNumber);
        }
        if ((long) TimeMap.RESULTS_PER_PAGE * pageNumber < countOfMementos) {
            writeLinkTypeNextPage(originalResource, output, pageNumber);
        }
    }

    /**
     * Create a link to the page before the current in a paged timemap series.
     * @param originalResource  which this timemap is about.
     * @param output            outputstream already containing information on the given URI-R and some links to
     *                          mementos for the resource.
     * @param pageNumber        the page of the paged timemap.
     */
    private static void writeLinkTypePreviousPage(URI originalResource, OutputStream output, Integer pageNumber) throws IOException {
        String previousPageLink = "<" +  PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/" +
                (pageNumber - 1) + "/link/" + originalResource + ">" +
                "; rel=\"prev\"; type=\"application/link-format\"\n";

        output.write(previousPageLink.getBytes());
    }

    /**
     * Create a link to the page after the current in a paged timemap series.
     * @param originalResource  which this timemap is about.
     * @param output            outputstream already containing information on the given URI-R and some links to
     *                          mementos for the resource.
     *                          Would also contain a link to the previous page, if such exists.
     * @param pageNumber        the page of the paged timemap.
     */
    private static void writeLinkTypeNextPage(URI originalResource, OutputStream output, Integer pageNumber) throws IOException {
        String nextPageLink = "<" +  PropertiesLoaderWeb.WAYBACK_SERVER + "services/memento/timemap/" +
                (pageNumber + 1) + "/link/" + originalResource + ">" +
                "; rel=\"next\"; type=\"application/link-format\"\n";

        output.write(nextPageLink.getBytes());
    }

    /**
     * Update the memento timemap "header", which contains information on the original resource and the range of dates
     * included in the timemap
     * @param doc               Solr document is not used by the method, only delivered and returned to make streaming
     *                          workflow continue.
     * @param metadata          object that gets updated with the timemap header.
     * @param originalResource  url of the original resource (URI-R). The URI-R is used in the header.
     * @return                  the original solr document for further streaming.
     */
    private static SolrDocument updateTimeMapHeadForLinkFormat(SolrDocument doc, MementoMetadata metadata, String originalResource, Integer pageNumber) {
        metadata.setTimeMapHeadForLinkFormat(originalResource, pageNumber);
        return doc;
    }

    /**
     * Create an application/link-format compliant memento representation of an archived resource from solr.
     *
     * @param doc               The solr document, that contains information on the individual harvested resource.
     * @param iterator          Used define the relations 'first memento' and 'last memento'
     * @param countOfMementos   Used define the relations 'first memento' and 'last memento'
     * @return                  The memento as a string, ready to be concatenated to a memento timemap.
     */
    private static String createMementoInLinkFormat(SolrDocument doc, AtomicLong iterator, Long countOfMementos) {
        String memento = "";
        try {
            if (iterator.longValue() == 1L){
                memento = "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" +
                        doc.getFieldValue("wayback_date") + "/" + doc.getFieldValue("url") + ">" +
                        "; rel=\"first memento\"; datetime=\"" + DateUtils.convertWaybackdate2Mementodate((Long) doc.getFieldValue("wayback_date")) + "\",\n";
                iterator.getAndIncrement();
            } else if (iterator.longValue() == countOfMementos) {
                memento = "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" +
                        doc.getFieldValue("wayback_date") + "/" + doc.getFieldValue("url") + ">" +
                        "; rel=\"last memento\"; datetime=\"" + DateUtils.convertWaybackdate2Mementodate((Long) doc.getFieldValue("wayback_date")) + "\",\n";
                iterator.getAndIncrement();
            } else {
                memento = "<" + PropertiesLoaderWeb.WAYBACK_SERVER + "services/web/" +
                        doc.getFieldValue("wayback_date") + "/" + doc.getFieldValue("url") + ">" +
                        "; rel=\"memento\"; datetime=\"" + DateUtils.convertWaybackdate2Mementodate((Long) doc.getFieldValue("wayback_date")) + "\",\n";
                iterator.getAndIncrement();
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        // TODO: Add license
        return memento;
    }

    /**
     * Writes a string/memento to an outputstream.
     * @param string        memento to add to outputstream.
     * @param outputStream  outputstream written to. This delivers the output to the request.
     */
    private static void writeStringSafe(String string, OutputStream outputStream) {
        try {
            outputStream.write(string.getBytes());
        } catch (RuntimeException | IOException e){
            throw new RuntimeException();
        }
    }
}
