#!/bin/bash

pushd ${BASH_SOURCE%/*} > /dev/null

java -cp warc-indexer-3.4.0-jar-with-dependencies.jar uk.bl.wa.util.ConfigPrinter

