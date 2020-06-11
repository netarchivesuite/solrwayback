#!/bin/sh

#TODO change this to point to webapps in the solrwayback folder
deployFolder='...../solrwayback_package_3.2/apache-tomcat-8.5.29/webapps'

rm  $deployFolder/solrwayback -r
mvn clean package -DskipTests
mv target/solrwayback*.war target/solrwayback.war
cp target/solrwayback.war $deployFolder

echo "solrwayback deployed to localtomcat"

