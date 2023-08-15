# SolrWayback bundle

Resources used when building the SolrWayback bundle.

- `install SolrWayback bundle`: See install guide [SolrWayback README](https://github.com/netarchivesuite/solrwayback/blob/master/README.md/)
- `indexing`: Scripts for indexing WARC files using [webarchive-discovery](https://github.com/ukwa/webarchive-discovery/)
- `Changes.md`: See version history [SolrWayback](https://github.com/netarchivesuite/solrwayback/blob/master/CHANGES.md/)

## How to for package managers

### Build WARs and JAR

Create the SolrWayback WAR
```
mvn clean package
```

Build a `warc-indexer-0.3.2-SNAPSHOT-jar-with-dependencies.jar` from [webarchive-discovery](https://github.com/ukwa/webarchive-discovery/).

Build a `solrwaybackrootproxy-4.3.1.war` from [solrwaybackrootproxy](https://github.com/netarchivesuite/solrwaybackrootproxy).

### Folder structure

```
mkdir solrwayback_package_4.5
cd solrwayback_package_4.5/
cp -r ../src/bundle/indexing/ .
cp 
cp -r ../src/test/resources/solr_9/ solr_9_files.
cp ../README.md ../CHANGES.md .
mkdir properties
cp ../src/test/resources/properties/solrwayback.properties properties/
cp ../src/test/resources/properties/solrwaybackweb.properties properties/
```

Copy the previously generated `warc-indexer-XXX-jar-with-dependencies.jar` to the `indexing/` folder.

### Tomcat 9

Download and unpack Tomcat 9 (in current folder `solrwayback_package_4.5`)
```
wget 'https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.78/bin/apache-tomcat-9.0.78.tar.gz'
tar -xzovf apache-tomcat-9.0.78.tar.gz
mv apache-tomcat-9.0.78 tomcat-9
rm apache-tomcat-9.0.78.tar.gz
```

Copy WAR and context:
```
cp ../target/solrwayback-*.war tomcat-9/webapps/solrwayback.war
mkdir -p conf/Catalina/localhost/
cp ../src/main/webapp/META-INF/context.xml tomcat-9/conf/Catalina/localhost/solrwayback.xml
```

Edit `tomcat-9/conf/Catalina/localhost/solrwayback.xml` and set
 * `solrwayback-config` to `properties/solrwayback.properties`
 * `solrwaybackweb-config` to `properties/solrwaybackweb.properties`

Copy and rename the previously generated `solrwaybackrootproxy-4.3.1.war` to `tomcat/webapps/ROOT.war`.

### Solr 9

Download and unpack Solr 9 (in current folder `solrwayback_package_4.5`)
```
wget 'https://www.apache.org/dyn/closer.lua/solr/solr/9.3.0/solr-9.3.0.tgz?action=download' -O solr-9.3.0.tgz
tar -xovf solr-9.3.0.tgz
mv solr-9.3.0 solr-9
rm solr-9.2.1.tgz
```

/Optional but makes it easier to debug:/ Open Solr to the World instead of just localhost
```
sed -i 's/#SOLR_JETTY_HOST="127.0.0.1"/SOLR_JETTY_HOST="0.0.0.0"/' solr-9/bin/solr.in.sh
sed -i 's/REM set SOLR_JETTY_HOST=127.0.0.1/set SOLR_JETTY_HOST=0.0.0.0/' solr-9/bin/solr.in.cmd
```

Start Solr in cloud mode, create a 1 shard `netarchivebuilder` collection and shut it down
```
solr-9/bin/solr start -c -m 1g
solr-9/bin/solr create_collection -c netarchivebuilder -d solr_9_files/netarchivebuilder/conf/ -n sw_conf_1 -shards 1
solr-9/bin/solr stop
```

### Finishing and packing (in current folder `solrwayback_package_4.5`)

Remove Emacs backup files (if any)
```
find . -iname "*~" | xargs rm
```

Create the bundle
```
cd ..
zip -r solrwayback_package_4.5.zip solrwayback_package_4.5/
```
