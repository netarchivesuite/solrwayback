#!/bin/sh

deployFolder='/media/teg/1200GB_SSD/solrwayback_package_3.2_webrecorder/apache-tomcat-8.5.29/webapps'

rm -r $deployFolder/solrwayback 
mvn clean package -DskipTests
mv target/solrwayback*.war target/solrwayback.war
cp target/solrwayback.war $deployFolder

echo "solrwayback deployed to localtomcat"

