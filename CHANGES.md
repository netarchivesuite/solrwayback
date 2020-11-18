
4.0.2
-----
Clean up in logging to the tomcat log-file.
Linkgraph remimplemeted with improve GUI. Less spaghetti for large graphs and local sub-graph highlighting for a node by click.
CVS export option for the n-gram query visualization
Page resources (playback toolbar) shows total timespan for resources in top so it is easier to see how far apart the resources are from the main page.
Added icon on search result to show warc headers for that record.  
Facet counts are formatted to be more readable for large numbers.(thousand delimiters)
Data fields on records new listed in alphabetical order.
Export to warc file also has option to export to a zipped warc.gz file.(still waiting frontend enabling)
Fixed encoding bug for playback (html content charset) that was introduced in 4.0.1 due to switch to chaining inputstreams.


Added optional proprerty to solrwaybackweb.properties. Default value is 1998 is not set. Start year of the collection is used in several visualizations.

```
#n-gram and domain statistics  etc. needs a start year for the visualizations. Will default to 1998 if not defined.
archive.start.year=1998
```


4.0.1
-----
Playback support for chunked transfer encoding. This also require new version of the warc-indexer when indexing (https://github.com/ukwa/webarchive-discovery/pull/232)

Images geo search reimplemented with better design. Lazy load of images, dynamic heatmap and spreading out images on same location.

Smurf (N-gram) visualization implemented in toolbox.

Domain stats visualization reimplemented (toolbox)

PWID reimplemented (playback toolbar)

Memory improvement for streaming warc-export. Minimal memory required for the web-app which was a scaling problem for multiple simultaneous exports.

Memory improvement during playback when servering large binaries (images/audio/video etc.)

Minor playback improvements.
 
Added unittest+warcs  for various decompression (brotli,gzip,chuncking)

Added unittest for warc-export. (Mockito)

Spinners added for more service-calls.

Minor css-changes to layout


The following features still needs to be implemented

HTML-tags graph

Link graph

Gephi  linkgraph export

Website previews




4.0.0
-----
Frontend completely rewritten (VUE framework)
Services upgraded from Jersey to apache-cxf

log4j upgraded to logback

Various third part dependencies upgraded as well.

Playback improvements.

Warc-export with resources will find more resources to include.
 
Ranking change (in solrconfig.xml). Boost html pages a little more.

Facet years sortet by year and not count.

Faster loading of results and pagination. Facets are loaded in a seperate service call and not reloaded when paging.

The two export to CVS options has been merged into one where you can select which fields to export from search result.

"About this archive" text added. This can be configured in solrwaybackweb.properties to load a custom file instead.

Search guidelines added. This can be configured in solrwaybackweb.properties to load a custom file instead.

Services has been split into frontend-services and backend-services.

Introduced toolbox on search page. Some features from the playback toolbar has been moved into this toolbox.
 
Wordcloud added as new feature to toolbox.

Result visualization by domain for search results. Shows top 30 domains in search results over the years.
 
Icons added for different document types. (video/audio/html etc.)

Url search reimplemented and working. Search by an url, it will be puny encoded and normalized.

Lazy loading of images. Once again performance improvement and not hitting the backend as hard.

"Back to top" and pagination link added after search results.

Spinner animations added to all service calls while it is loading.

In browser audio+video player added for documents of this time. If browser does not support the encoding, it can be downloaded as before.

Improved twitter playback for twitter warc-files harvesed with https://github.com/netarchivesuite/so-me

Still missing the folllowing features before version 4.0:

PWID (show XML in browser + clipboard function)

GPS location search

HTML-tags graph

Link graph

Domain stats

Gephi  linkgraph export

Website previews

Smurf (new feature)


Property changes in solrwayback.properties:
Socks v.4 proxy did not perform well and has been removed, so has the properties: proxy.port+proxy.allow.hosts

Property changes in solrwaybackweb.properties:
```
# About this archive. Will be shown when page is loaded and when about is clicked.
# Search help is shown when the icon next to search is clicked. 
# Both properties can be changed to a full filepath with a custom text.. HTML formating allowed.
# Below values uses the default text files in SolrWayback. 
about.text.file=/about_this_archive.txt 
search.help.text.file=/search_help.txt


#define fields that can be selected for CVS export
export.csv.fields=id,index_time, author, description,keywords,description,license_url,content,content_encoding,content_length,content_language, content_type_droid,content_type_ext,content_type_full,content_type_norm,content_type_served,content_type_tika,content_type,content_type_version,elements_used,hash,wayback_date,crawl_year,url_norm,url_path,url,url_type,domain,host,host_surt,public_suffix,resourcename,image_size,links_images,links_domains,links_hosts,links_hosts_surts,links_norm,links_public_suffixes,links,server,status_code,generator,redirect_to_norm,source_file_path,source_file_offset,source_file,text,title,type,warc_key_id,warc_ip ,ssdeep_hash_bs_3, ssdeep_hash_bs_6, ssdeep_hash_bs_12, ssdeep_hash_bs_24, ssdeep_hash_bs_48, ssdeep_hash_bs_96,ssdeep_hash_bs_192
```




Tomcat upgraded to 8.5.51 in  SolrwWayback bundle release. Due to GhostCat (CVE-2020-1938 - AJP). 
Special tomcat config required:
```
    <Connector port="9721" protocol="HTTP/1.1"
               URIEncoding="UTF-8" 
               useBodyEncodingForURI="true"
relaxedPathChars="[]|"
relaxedQueryChars="[]|{}^&#x5c;&#x60;&quot;&lt;&gt;"
               connectionTimeout="20000" />
```





3.2.6
-----
Twitter API playback updated and improved (require newer version of warc-indexer)
Timing logs for solr query, both for Solr Qtime and total time seen from SolrClient.  
Fixes UTC timing bug for some date conversion.
General cleanup and refactoring.
Few playback improvements (the 'utf-8' error fixed)
Attempt to fixed pagination (20+20 = 2020 instead of 40 in javascript). Probably only happens in some browsers. 

3.2.5
-----
Security/Vulnerability fix for all frontend dependencies.
Minor playback improvements (special characters like \ in url)


3.2.4
-----
Fixed white-space before url in PWID xml generation

PWID service. Can query collection for a PWID.
syntax is: (server)/services/pwid/web/urn:pwid:netarkivet.dk:2018-12-10T06:27:01Z:part:https://www.petdreams.dk/katteracer-siameser
Will return the document is collection(netarkivet) is correct and the given url (https://www.petdreams.dk/katteracer-siameser) is harvest
exactly on 2018-12-10T06:27:01Z.
For HTML pages playback will be shown, for resources (images etc.) the binary will be returned.

3.2.3
-----
Link graph export to cvs-format that can be loaded by Gephi. Export is streaming and can extract million of domains.
Implemented temporary web-page for the link graph export. (New menu link added)
Warc files made with WebRecorder can be now be indexed and gives better playback than with Heritrix3 warc-files. Fixes was mostly in the warc-indexer(Gzip support).
Improved Playback for twitter-API harvest. (missing images in some situations)
Various minor playback improvements. background-image for div tags most important fix. 
Minor fix with SVG images, converting them to PNG and scaling caused all kind of java troubles with ImagesIO.

3.2.2
-----
Property to also disable CSV export of result. Was already implemented for warc-export.
New property allow.export.csv=false in solrwaybackweb.properties
Various playback fixed when redirecting. Fixed a bug with redirect http -> https as only difference in URL.
Block redirect cycles early when detected to prevent browser going for maximum redirects.
Backend method to see the arc/warc header, not implemented in frontend yet.
Updated log-frame(slf4j) to 1.7.21
  

3.2.1
-----
Fix issued with warc-indexer 3.0.0 -> 3.0.1 broke backwards solr schema compatibility. Also some url normalization bugs was fixed. 
A property in solrwayback.properties can enable using the old schema if you dont want to reindex with warc-indexer 3.0.1.
 


3.2.0
-----
Apache2 license added.

Search option to group by URL, unchecked as default since it makes search slower. This will only show one result for the same URL.

When exporting a resultset to WARC-format there is an option to 'expand' HTML documents so all their resources are exported as well.

Added both 127.0.0.1 and localhost to solrwayback.properties proxy allow property. Required for supporting both socks proxy 4 and socks proxy 4a.
 
Domain statistics moved to search interface.

Graceful shutdown of socks proxy (fixing deploy to tomcat issue for developers)

Option property to link to an additional playback engine (local open wayback, archive.org etc.). Link is placed next to the Solrwayback playback link on title.
   
Playback improvements. (img srcset tag, embed url,  video/audio having nested source-tags)
  
Search by entering URL option in search interface. This will also convert the URL from IDN to puny code, making search by URL easier.

Faster search but only loading the fields showing in the short record presentation. Only load all fields when showing them for a record. For index-size > 300GB this is a factor 2 or more.
   
Domain growth stats graph also included in search-frontend and not only playback toolbar.

GPS image search. Replaced google maps with leaflet.js - Leaflet javascript/css included in WAR file. Map properties in solrwaybackweb.properties renamed, google prefix removed.

For image GPS search the start location parameters extracted to property-file.

For PID export the collection name has been extracted to property file.

For screenshot preview using headless chrome, the timeout parameter has been extracted to property-file. Optional, 10 seconds is default

Facets are now  defined in the property file (solrwaybackweb.properties), optional property.

If SolrWayback is deployed in the tomcat under another contextname than solrwayback, it will try load a property file with that context name before defaulting to solrwayback.properties.

Showing HTTP status code in toolbar. Also show toolbar for empty responses (301,302 etc.)
 
Rewrite of frontend urls so SolrWayback can run as HTTPS.

API for generating Wordcloud image generator for domain: http://server:port/solrwayback/services/wordcloud/domain?domain=test.dk


3.1
-----
The first release for IIPC newsletter

Binary bundled release: https://github.com/netarchivesuite/solrwayback/releases




