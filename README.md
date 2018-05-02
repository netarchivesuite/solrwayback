# SolrWayback

## Prototype release, Solrwayback 3.1 bundle 
Release planned for 1. May 2018


## About Solrwayback
This 3.0 branch will only work with warc-indexer version 3.0+

SolrWayback is web-application for browsing historical harvested ARC/WARC files similar
to the Internet Archive Wayback Machine. The SolrWayback depends on a Solr server where
Arc/Warc files have been indexed using the British Library WARC-Indexer. The Netsearch application is just a simple book keeping application on top of the WARC-Indexer that also
controls the indexing. Unlike the Wayback Machine the SolrWayback does not need the
CDX-server with meta data for the harvest. It only uses the Solr server and the raw
Arc/Warc files.

 Warc-indexer: https://github.com/ukwa/webarchive-discovery/tree/master/warc-indexer<br>
 Netsearch(Archon/Arctika): https://github.com/netarchivesuite/netsearch<br>



SolrWayback comes with additional features:
* Image search similar to google images
* Search by uploading a file. (image/pdf etc.) See if the resource has been harvested and from where.
* Link graph showing links (ingoing/outgoing) for domains using the D3 javascript framework.
* Raw download of any harvested resource from the binary Arc/Warc file.
* Export a search resultset to a Warc-file. Streaming download, no limit of size of resultset.
* Build in SOCKS proxy to view historical webpages without browser leaking resources from the live web.
 

<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/solrwayback_search.png?raw=true" width="600" height="400"/>
</p>
<p align="center">
  Search example showing hits. Images are shown in search-result.
</p>

<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/image_search.png?raw=true" width="600" height="400"/>
</p>
<p align="center">
  Google like image search in the web-archive
</p>

<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/solrwayback_demo.png?raw=true" width="600" height="400"/>
</p>
<p align="center">
Solrwayback showing an archived webpage with an overlay statistics and further navigation options.
</p>


<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/multiple_pagepreviews.png?raw=true" width="600" height="400"/>
</p>
<p align="center">
Page previews for different harvest times of a given url. Images are generated real-time and uses the build in socks proxy to prevent leaking to the live web.
</p>



<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/solrwayback_linkgraph.png?raw=true" width="600" height="400"/>
</p>
<p align="center">
Interactive domain link graph
</p>

<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/solrwayback_crawltimes.png?raw=true" />
</p>
<p align="center">
Github like visualization of crawltimes
</p>


<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/gps_exif_search.png?raw=true" />
</p>
<p align="center">
Search by gps location for images having exif location information.
</p>

 
## Streaming export
Any search result can be exported to WARC format. If any of the documents are indexed as ARC format, they will be converted to WARC format. There is useful when extracting a corpus  (typical specific domains). There is no limit to the size of the export.
Text information about a corpus can also be exported as a CSV file.


## SOCKS proxy - No leaking to the live web
Solrwayback starts up listing on two ports. The default port 8080 and then port 9000 for SOCKS proxy v.4 mode.
The default SOCKS  proxy port in the property file is 9000.
Configure your browser to run SOCKS v4 to <solrwaybackurl> port 9000
Ie. localhost port 9000
You can still connect to solrwayback without SOCKS mode to the normal port 8080.
This useful so the page can be seen both with and without leaking, using two different browsers with 1 using SOCKS v.4 mode.


## API
The API for linking to and browsing archived webpages is the same as for Internet Archive:<br>

Internet Archive:https://web.archive.org/web/20080213093319/http://www.statsbiblioteket.dk/ <br>
SolrWayback: http://server/solrwayback/services/web/20140515140841/http://statsbiblioteket.dk/ <br>

If using a Solr search based web archive such as Shine (https://github.com/netarchivesuite/shine) or Blacklight (https://github.com/projectblacklight/blacklight)
you only need to change to property pointing from the wayback server to the SolrWayback server.
 
 
 
## Requirements
 * JDK 1.8+
 * Maven 3 
 * Some Arc/Warc files 
 * Tomcat 8+  or another J2EE server for deploying the WAR-file
 * A Solr 7.1+ server with the index build from the Arc/Warc files using the Warc-Indexer version 3.0
 * (Optional) chrome/(chromium) installed for page previews to work. (headless chrome) 
 
## Build and usage
 * Build the application with: mvn package
 * Deploy the solrwayback.war file in a web-container. 
 * Copy resources/properties/solrwayback.properties and resources/properties/solrwaybackweb.properties 
   to user/home/ folder for the J2EE server
 * Modify the property files. (default all urls http:://localhost:8080)
 * Optional: configure the log4j using the files in resources/tomcat
 * Restart tomcat.
 * Open search interface: localhost:8080/solrwayback

## Run using Docker
 * Copy resources/properties/solrwayback.properties to the project root directory and modify it to your needs.
 * Make sure Docker Engine is installed.
 * Run ./docker-run.sh from the project root.

## Contact
* Thomas Egense (thomas.egense@gmail.com) 
* Niels Gamborg (nig@kb.dk) 

Feel free to send emails with comments and questions.

## Warc-indexer/Solr 
All entries from the arc/warc files are indexed as separate documents using the WARC-Indexer and using the lucene schema required by the WARCIndexer.
A document is Solr can be html, image, video, audio, js etc. (content_type_norm)
All document contains a field with arcfilename and offset, this is so  the binary from
the arc/warc file can be loaded again - not through solr but by IO read on the disk where the 
arc/warc file is stored.
 



      

