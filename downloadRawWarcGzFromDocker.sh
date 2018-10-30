#!/usr/bin/env bash

set -e
WARCFILE="IAH-20080430204825-00000-blackbook.warc.gz"

offsets=$(cat "src/test/resources/example_warc/$WARCFILE.cdx" | cut -d' ' -f10 | tail -n +2)
#Find the offsets
#offsets=$(grep --text --byte-offset --only-matching  "^WARC/" src/test/resources/example_warc/$WARCFILE | cut -d':' -f1)

#Try to get each
echo "$offsets" | xargs -n 1 -r -i -t sh -c "curl --silent --show-error --fail 'http://localhost:8888/solrwayback/services/downloadRaw?source_file_path=/netarkiv/warc/$WARCFILE&offset={}' >> warcGzContent.bin || exit 255"