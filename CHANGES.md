3.2
-----
1) facets are now  defined in the property file (solrwaybackweb.properties). They were hardcoded in 3.1
2) Search option to group by URL, unchecked as default since it makes search slower. This prevents the same result (url_norm)
from appearing multiple times in the result set.
3) When exporting a resultset to WARC-format there is an option to 'expand' HTML documents so all their resources are exported as well.
127.0.0.1 to solrwayback.properties 
 
 

3.1
-----
The first release for IIPC newsletter
Binary bundled release: https://github.com/netarchivesuite/solrwayback/releases




