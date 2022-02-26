cd /D "%~dp0"

FOR /R warcs1 %%G IN (*.*) do  java -Xmx2048M -Djava.io.tmpdir=tika_tmp -jar warc-indexer-3.2.0-SNAPSHOT-jar-with-dependencies.jar -c config3.conf -s  "http://localhost:8983/solr/netarchivebuilder"  "%%G"

