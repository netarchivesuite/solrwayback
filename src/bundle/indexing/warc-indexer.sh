#!/bin/bash

#
# Starts multiple instances of warc-indexer.jar for populating a Solr index
# from WARCs.
#
# Keeps track of already processed WARCs.
#
# 2021-06-07: Initial script
# 2021-06-08: Status & tmp folder moved away from the WARC location
#

###############################################################################
# CONFIG
###############################################################################


if [[ -s "warc-indexer.conf" ]]; then
    source "warc-indexer.conf"     # Local overrides
fi
pushd ${BASH_SOURCE%/*} > /dev/null
if [[ -s "warc-indexer.conf" ]]; then
    source "warc-indexer.conf"     # General overrides
fi
WI_HOME=`pwd`
SOLR_URL_DEFAULT="http://localhost:8983/solr/netarchivebuilder"
THREADS_DEFAULT="2"

: ${WARCS:="$@"}

: ${INDEXER_JAR:="${WI_HOME}/warc-indexer-3.2.0-SNAPSHOT-jar-with-dependencies.jar"}
: ${INDEXER_MEM:="1024M"}
: ${INDEXER_CONFIG:="${WI_HOME}/config3.conf"}
: ${INDEXER_CUSTOM:=""} # Custom arguments, e.g. "--collection OurHeritage2020" goes here

: ${SOLR_URL:="$SOLR_URL_DEFAULT"}
: ${THREADS:="$THREADS_DEFAULT"}
: ${STATUS_ROOT:="${WI_HOME}/status"}
: ${TMP_ROOT:="${STATUS_ROOT}/tmp"}
: ${CHECK_SOLR:="true"}
popd > /dev/null

function usage() {
    cat <<EOF

warc-indexer.sh

Parallel processing of WARC files using webarchive-discovery from UKWA:
https://github.com/ukwa/webarchive-discovery

The scripts keeps track of already processed WARCs and stores the output logs
from processing alongside the WARCs.


Usage: ./warc-indexer.sh [warc|warc-folder]*

THREADS:  The amount of concurrent indexing jobs (default: $THREADS_DEFAULT)
SOLR_URL: Solr end point (default: $SOLR_URL_DEFAULT)

Sample calls:

./warc-indexer.sh mywarcfile1.warc.gz mywarcfile2.warc.gz

THREADS=20 ./warc-indexer.sh folder_with_warc_files

THREADS=20 SOLR_URL="http://localhost:8983/solr/netarchivebuilder" ./warc-indexer.sh folder_with_warc_files

Note:
Each threads starts its own Java process with -Xmx${INDEXER_MEM}.
Make sure that there is enough memory on the machine.
EOF
    exit $1
}

check_parameters() {
    if [[ "-h" == "$WARCS" ]]; then
        usage
    fi

    if [[ -z "$WARCS" ]]; then
        >&2 echo "Error: No WARCs specified"
        usage 2
    fi
    if [[ ! -s "$INDEXER_JAR" ]]; then
        >&2 echo "Error: INDEXER_JAR '$INDEXER_JAR' is unavailable"
        usage 3
    fi
    if [[ ! -s "$INDEXER_CONFIG" ]]; then
        >&2 echo "Error: INDEXER_CONFIG '$INDEXER_CONFIG' is unavailable"
        usage 4
    fi
    mkdir -p "$STATUS_ROOT"
    if [[ ! -d "$STATUS_ROOT" ]]; then
        >&2 echo "Error: The folder '$STATUS_ROOT' defined by STATUS_ROOT could not be created. This folder holds the status for all processed WARCs and the script does not work without it"
        usage 5
    fi
    mkdir -p "$TMP_ROOT"
    if [[ ! -d "$TMP_ROOT" ]]; then
        >&2 echo "Error: The folder '$TMP_ROOT' defined by TMP_ROOT could not be created. This folder holds temporary files for running jobs and the script does not work without it"
        usage 6
    fi
}

################################################################################
# FUNCTIONS
################################################################################

index_warc() {
    local WARC="$1"
    local WARC_BASE="$(sed 's%.*/%%' <<< "$WARC")"
    local WARC_LOG="${STATUS_ROOT}/${WARC_BASE}.log"
    local WARC_FAILED="${STATUS_ROOT}/${WARC_BASE}.failed.log"
    local WARC_TMP="${TMP_ROOT}/${WARC_BASE}.tmp"
    if [[ ! -s "$WARC" ]]; then
        >&2 echo "Error: WARC does not exist: $WARC"
        return
    fi
    if [[ -d "$WARC_TMP" ]]; then
        echo "   - Skipping WARC '$WARC' due to old TMP folder (probably caused by a crash). Remove the TMP folder to retry processing: $WARC_TMP"
        return
    fi
    if [[ -s "$WARC_FAILED" ]]; then
        echo "   - Skipping WARC '$WARC' due to previously failed indexing. Remove the log file to retry processing: $WARC_FAILED"
        return
    fi
    if [[ -s "$WARC_LOG" ]]; then
        echo "   - Skipping WARC '$WARC' due to previously completed indexing. Remove the log file to retry processing: $WARC_LOG"
        return
    fi

    echo "   - Indexing $WARC"
    mkdir "$WARC_TMP"
    local CALL="java -Xmx1024M -Djava.io.tmpdir=\"$WARC_TMP\" -jar \"$INDEXER_JAR\" -c \"$INDEXER_CONFIG\" $INDEXER_CUSTOM -s  \"$SOLR_URL\"  \"$WARC\" &> \"$WARC_LOG\""
    echo "$CALL" >> "$WARC_LOG"
    java -Xmx1024M -Djava.io.tmpdir="$WARC_TMP" -jar "$INDEXER_JAR" -c "$INDEXER_CONFIG" $INDEXER_CUSTOM -s  "$SOLR_URL"  "$WARC" &>> "$WARC_LOG"

    local RC=$?
    if [[ $(wc -l < "$WARC_LOG") -eq 1 ]]; then
        mv "$WARC_LOG" "$WARC_FAILED"
        echo "   - Error indexing ${WARC} (no output produced). Remove $WARC_FAILED to retry"
    elif [[ $RC -ne 0 ]]; then
        mv "$WARC_LOG" "$WARC_FAILED"
        echo "   - Error indexing ${WARC} (return code $RETURN_CODE != 0). Please inspect $WARC_FAILED"
    elif [[ ! -z "$(grep "org.apache.solr.client.solrj.SolrServerException: Server refused connection" "$WARC_LOG")" ]]; then
        mv "$WARC_LOG" "$WARC_FAILED"
        echo "   - Error indexing ${WARC} (SolrServerException: Server refused connection). Please inspect $WARC_FAILED"
    fi
    if [[ -d "$WARC_TMP" ]]; then
        rm -r "$WARC_TMP"
    fi
}
export -f index_warc

index_warcs() {
    local WARCS="$1"
    if [[ ! -z "$(sed 's%.*/%%' "$WARCS" | sort | uniq -c | grep -v "\s*1 ")" ]]; then
        >&2 echo "Error: Duplicate WARC file names found. The script only works with unique WARC file names"
        sed 's%.*/%%' "$WARCS" | sort | uniq -c | grep -v "\s*1 "
        exit 21
    fi
    
    WARC_COUNT=$(wc -l < "$WARCS")
    echo " - Processing $WARC_COUNT WARCs using $THREADS threads"
    # We need to export these as we call index_warc in new processes
    export INDEXER_JAR
    export INDEXER_MEM
    export INDEXER_CONFIG
    export INDEXER_CUSTOM
    export SOLR_URL
    export STATUS_ROOT
    export TMP_ROOT
    cat "$WARCS" | xargs -P "$THREADS" -n 1 -I "{}" bash -c 'index_warc "{}"'
}

index_all() {
    FILE_WARCS=$(mktemp)
    for WARC in $WARCS; do
        if [[ -d "$WARC" ]]; then
            echo " - Recursively finding all WARCs in folder '$WARC'"
            find "$WARC" -iname "*.warc" -o -iname "*.warc.gz" >> "$FILE_WARCS"
        else
            echo "$WARC" >> "$FILE_WARCS"
        fi
    done
    if [[ ! -s "$FILE_WARCS" ]]; then
        >&2 echo "Error: unable to locate any WARCs from input '$WARCS"
        exit 11
    fi
    index_warcs "$FILE_WARCS"
    rm "$FILE_WARCS"
}

# TODO: Make a proper check for collection existence
# http://localhost:8983/solr/admin/cores
check_solr() {
    if [[ "$CHECK_SOLR" == "false" ]]; then
        return;
    fi
    echo " - Checking if Solr is running"
    wget  -nv -O- "${SOLR_URL}/admin/ping" > /dev/null
    if [[ $? -ne 0 ]]; then
        >&2 echo ""
        >&2 echo "Warning: Solr commit did not respond to ping request"
        >&2 echo "Inspect that Solr is running at ${SOLR_URL}"
        >&2 echo "Disable this check with CHECK_SOLR=false"
        exit 41
    fi
}

commit() {
    echo " -Triggering solr commit"
    wget  -nv -O- "${SOLR_URL}/update?commit=true&openSearcher=true"  > /dev/null
    if [[ $? -ne 0 ]]; then
        >&2 echo "Warning: Solr commit did not respond with success. Inspect that Solr is running at ${SOLR_URL}"
    fi
}


###############################################################################
# CODE
###############################################################################

check_parameters "$@"
check_solr
index_all
commit
