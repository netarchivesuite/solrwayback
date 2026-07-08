# Solr configuration

This folder contains a copy of the Solr configuration and can be used upload a new Solr configuration to Solr. Only for experience Solr users that knows what they are doing.
See the' Update Solr cloud configuration' in the project README.md 

To update the solr configuration, you can upload a new version of the conf folder with:

bin/solr zk upconfig \
  -n sw_conf1 \
  -d /home/xxx/solrwayback/solrwayback_package_5.4.3/solr_config/conf \
  -z localhost:9983
  
 And the refresh the collection:
 curl "http://localhost:8983/solr/admin/collections?action=RELOAD&name=netarchivebuilder"
 
The config name sw_conf1 is default in the SolrWayback Bundle distribution. You can add new fields to an existing index
without breaking backwards compatibility. The old data will just miss data from this field.

To make sure Solr data is flushed to the index, you can force a flush with:
http://localhost:8983/solr/netarchivebuilder/update?commit=true&openSearcher=true