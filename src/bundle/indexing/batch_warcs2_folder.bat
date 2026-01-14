cd /D "%~dp0"

FOR /R warcs2 %%G IN (*.*) do  java -Dfile.encoding=UTF-8 -Xmx2048M -Djava.io.tmpdir=tika_tmp -jar warc-indexer-3.4.0-jar-with-dependencies.jar -c config3.conf -s  "http://localhost:8983/solr/netarchivebuilder"  "%%G"

