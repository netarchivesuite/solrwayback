#!/bin/bash

pushd ${BASH_SOURCE%/*} > /dev/null

FILES=warcs1/*
for f in $FILES
do
    echo "Processing $f file..."
    java -Dfile.encoding=UTF-8 -Xmx1024M -Djava.io.tmpdir=tika_tmp -jar warc-indexer-3.3.1-jar-with-dependencies.jar -c config3.conf -s  "http://localhost:8983/solr/netarchivebuilder"  $f
done

echo "Flushing Solr. Documents will be visible after flush"
curl -s "http://localhost:8983/solr/netarchivebuilder/update?commit=true&openSearcher=true"  > /dev/null

popd > /dev/null
