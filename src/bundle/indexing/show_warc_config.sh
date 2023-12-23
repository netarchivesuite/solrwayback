#!/bin/bash

pushd ${BASH_SOURCE%/*} > /dev/null

java -cp warc-indexer-3.3.1-jar-with-dependencies.jar uk.bl.wa.util.ConfigPrinter

