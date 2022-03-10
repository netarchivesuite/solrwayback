#!/bin/bash
echo Will add sample data to ${SOLR_URL} after waiting for ${DELAY:-15} seconds for Solr to start up.
sleep ${DELAY:-15}
java -jar /jars/warc-indexer.jar -s ${SOLR_URL} /docker/test-warcs/TEST-20220304210400500-00000-80~h3w~8443.warc.gz /docker/test-warcs/TEST-WEBRENDER-20220304210341-20220304210402959-00000-pxa4tq3c.warc.gz
