# This dockerfile sets up the SolrWayback bundle and attempts to run both Solr and Tomcat.
# To build the image, run:
# docker build . --tag solrwayback

# To run SolrWayback, you need to launch it with the following parameters
# docker run --publish 8080:8080 --publish 8983:8983  --volume <path/to/WARCs>:/host_dir --tty --interactive solrwayback bash
# where <path/to/WARCs> is a file path that only contains WARC files and directories.

# When the container is running, run the following commands to start Solr and Tomcat:
# export SOLRWAYBACK_VERSION=5.1.2
# export APACHE_TOMCAT_VERSION=9
# export SOLR_VERSION=9
# ./unpacked-bundle/solrwayback_package_$SOLRWAYBACK_VERSION/solr-$SOLR_VERSION/bin/solr start -c
# ./unpacked-bundle/solrwayback_package_$SOLRWAYBACK_VERSION/tomcat-$APACHE_TOMCAT_VERSION/bin/startup.sh

# You should now verify that the following links works with a browser:
# http://localhost:8080/solrwayback/
# http://localhost:8983/solr/#/

# If you have some WARC files you want to index, you can index them with the following commands:
# WARC_FILES=$(find /host_dir/ -type f)
# ./unpacked-bundle/solrwayback_package_$SOLRWAYBACK_VERSION/indexing/warc-indexer.sh $WARC_FILES

FROM ubuntu:22.04

ENV SOLRWAYBACK_VERSION 5.1.2
ENV APACHE_TOMCAT_VERSION 9
ENV SOLR_VERSION 9

RUN apt-get update --assume-yes --quiet
RUN apt-get install wget unzip --assume-yes --quiet

# Install dependencies
RUN apt-get install default-jre lsof curl bc --assume-yes --quiet

RUN useradd --create-home --shell /bin/bash builder
RUN chown builder:builder /home/builder -R

USER builder
WORKDIR /home/builder

# Download and unpack SolrWayback bundle
RUN mkdir --parents solrwayback-zip
RUN wget --quiet https://github.com/netarchivesuite/solrwayback/releases/download/${SOLRWAYBACK_VERSION}/solrwayback_package_${SOLRWAYBACK_VERSION}.zip \
    --output-document solrwayback-zip/bundle.zip

RUN mkdir unpacked-bundle
RUN unzip -q solrwayback-zip/bundle.zip -d unpacked-bundle
RUN rm --recursive solrwayback-zip

# Set up SolrWayback configuration
RUN cp unpacked-bundle/solrwayback_package_${SOLRWAYBACK_VERSION}/properties/solrwayback.properties .
RUN cp unpacked-bundle/solrwayback_package_${SOLRWAYBACK_VERSION}/properties/solrwaybackweb.properties .

# Verify that apache-tomcat works
RUN unpacked-bundle/solrwayback_package_${SOLRWAYBACK_VERSION}/tomcat-${APACHE_TOMCAT_VERSION}/bin/startup.sh
RUN unpacked-bundle/solrwayback_package_${SOLRWAYBACK_VERSION}/tomcat-${APACHE_TOMCAT_VERSION}/bin/shutdown.sh

# Verify that solr works
RUN unpacked-bundle/solrwayback_package_${SOLRWAYBACK_VERSION}/solr-${SOLR_VERSION}/bin/solr start -c
RUN unpacked-bundle/solrwayback_package_${SOLRWAYBACK_VERSION}/solr-${SOLR_VERSION}/bin/solr stop -all
