# Solr 7

## Solr notes

Solr 7 deprecates the numeric Trie-classes used by previous Solr versions.
The replacement Points-classes are simpler as they don't need special tweaks to optimize
for range searches.

## Guiding principle

The schema explicitly sets defaults for
  
 * atomic fields to `indexed="true" stored="false" docValues=true" multiValued="false"`
 * `Text`-fields to `indexed="true" stored="true" multiValued="false"` 

The `stored="false" docValues="true"` for atomic fields (`string`, `int`, `boolean`, ...)
is chosen to reduce the memory overhead for faceting, grouping and sorting. In Solr 7, all
content for `docValues="true"`-fields can be part of the document response,
exactly as if `stored="true"` was specified.

The two persistence possibilities differ in internal representation and performance, with 
the most notable being an alleged performance cost for retrieving documents if 
`docValues="true"` is used. It is worth noting that Elasticsearch uses the same defaults
as stated above: https://www.elastic.co/guide/en/elasticsearch/reference/current/doc-values.html

## Upgrade notes

The Solr 7 schema is field-name compatible with previous schemas, but some fields has
changed behaviour.

 * The catch-all `text`-field is now single valued. It has always contained at most 1 elment,
 but now the schema reflects this.
 
 