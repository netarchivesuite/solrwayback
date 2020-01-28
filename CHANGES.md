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




