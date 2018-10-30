#!/usr/bin/env bash

set -e

WARCFILE="IAH-20080430204825-00000-blackbook.arc"

#Find the offsets, by grepping after lines with http and a ip address
offsets=$(grep -E --text --byte-offset  "^http.*\s[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}" src/test/resources/example_arc/$WARCFILE)
#echo "$offsets"

#Try to get each
echo "$offsets" |  cut -d':' -f1 | xargs -n 1 -r -i -t sh -c "curl --silent  --show-error --fail 'http://localhost:8888/solrwayback/services/downloadRaw?source_file_path=/netarkiv/arc/$WARCFILE&offset={}' >> arc_contents.bin || exit 255"