3.2
-----

Search option to group by URL, unchecked as default since it makes search slower. This prevents the same result (url_norm)
from appearing multiple times in the result set.
When exporting a resultset to WARC-format there is an option to 'expand' HTML documents so all their resources are exported as well.
Added both 127.0.0.1 and localhost to solrwayback.properties proxy allow property. Required for supporting both socks proxy 4 and socks proxy 4a. 
Domain statistics moved to search interface
Graceful shutdown of socks proxy (fixing deploy to tomcat issue for developers)
Option property to link to an additional playback engine (local open wayback, archive.org etc.). Link is placed next to the Solrwayback playback link on title.   
Playback improvements. (img srcset tag, embed url,  video/audio having nested source-tags)  
Search by entering URL option in search interface. This will also convert the URL from IDN to puny code, making search by URL easier.
Faster search but only loading the fields showing in the short record presentation. Only load all fields when showing them for a record. For index-size > 300GB this is a factor 2 or more.   
Domain growth stats graph also included in search-frontend and not only playback toolbar.
Google API key extracted to property file. It is required you use your own API key.
For image GPS search the google maps start location parameters extracted to property-file
For PID export the collection name has been extracted to property file.
For screenshot preview using headless chrome, the timeout parameter has been extraced to property-file. Optional, 10 seconds is default
Facets are now  defined in the property file (solrwaybackweb.properties). This is optional. They were hardcoded in 3.1
If solrwayback is deployed in the tomcat under another contextname that solrwayback, it will try load a property file with that context name before defaulting to solrwayback.properties 
Showing HTTP status code in toolbar. Also show toolbar for empty responses (301,302 etc.) 

3.1
-----
The first release for IIPC newsletter
Binary bundled release: https://github.com/netarchivesuite/solrwayback/releases




