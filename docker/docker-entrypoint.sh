#!/bin/sh

# Substitute environment variables:
envsubst < $HOME/solrwayback.properties.template > $HOME/solrwayback.properties
cat $HOME/solrwayback.properties
envsubst < $HOME/solrwaybackweb.properties.template > $HOME/solrwaybackweb.properties
cat $HOME/solrwaybackweb.properties

# And now start Tomcat:
catalina.sh run