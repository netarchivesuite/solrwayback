# SolrWayback

SolrWayback is web-application for browsing historical harvested ARC/WARC files similar
to the Internet Archive Wayback Machine. The Solrwayback depend on a Solr server where
Arc/Warc files has been indexed using the British Library WARC-Indexer. The netsearch application is just a simple book keeping application on top of the WARC-Indexer that also
controls the indexing.

 Warc-indexer: https://github.com/ukwa/webarchive-discovery/tree/master/warc-indexer<br>
 Netsearch(Archon/Arctika): https://github.com/netarchivesuite/netsearch<br>
 

<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/solrwayback_demo.png?raw=true" width="600" height="400"/>
</p>
<p align="center">
Solrwayback showing an archived webpage with an overlay statistics and further navigation options.
</p>

Solrwayback comes with additional features:
* Image search similar to google images
* Link graphs showing how links (ingoing/outgoing) for domains using the D3 javascript framework.
* Raw download of any harvested resource from the binary Arc/Warc file.


<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/solrwayback_linkgraph.png?raw=true" width="600" height="400"/>
</p>
<p align="center">
Interactive domain link graph
</p>

For image search see the documentation <a href="https://github.com/netarchivesuite/solrwayback/blob/master/doc/imagesearch.txt" >Image search documentation </a>


The API for linking to and browsing archived webpages is the same as for Internet Archive:<br>

Internet Archive:https://web.archive.org/web/20080213093319/http://www.statsbiblioteket.dk/ <br>
SolrWayback: http://server/solrwayback/services/wayback?waybackdata=20140515140841/http://statsbiblioteket.dk/ <br>

If using a Solr search based web archive such as Shine (https://github.com/netarchivesuite/shine) or Blacklight (https://github.com/projectblacklight/blacklight)
you only need to change to property pointing from the wayback server to the SolrWayback server.

The Solrwayback web application comes with a simple front-end for testing Solr-search and image search.
 
 
 
## Requirements
 * JDK 1.7
 * Maven 3 
 * Some Arc/Warc files 
 * Tomcat 7+  or another J2EE server for deploying the WAR-file
 * A Solr server with the index Arc/Warc files using the Warc-Indexer.
 * The J2EE server must have the Arc/Warc file drive mounted
 
## Build and usage
 * Build the application with: mvn package
 * Deploy the solrwayback.war file in a web-container.
 * For the API open:  localhost:8080/solrwayback
 * Copy resources/properties/solrwayback.properties to user/home/ folder for the J2EE server
 * Optional: configure the log4j using the files in resources/tomcat

## Contact
Developed by Thomas Egense (thomas.egense@gmail.com) 
Feel free to send emails with comments and questions.

##Demo
See a live demo  of the application running on a 8 billion docs index (280MB avi)
https://dl.dropboxusercontent.com/u/51314887/WebarchivemimetypeServlet_demo.avi    


## How Image search works

##Solr 
All entries from the arc/warc files are indexed as separate documents using the WARCindexer and using the lucene schema required by the WARCIndexer.
A document is solr can be html, image, video, audio, js etc. (content_type_norm)
All document contains a field with arcfilename and offset, this is so  the binary from
the arc/warc file can be loaded again - not through solr but by IO read on the disk where the 
arc/warc file is stored.

       
##Future improvements:
Perfomance: When load and parse the html to find images, this is just done sequential. It can be made multithread (Executor)
and load/parse several arc/warc files simultaneous. <-- This is is now implemented . 20 Threads is default

Ranking:Only extract the images near where the search-term is found. This will probably give more
relevant images when there are many images on a page.

When indexing a html-page we can at index time find the images and add these to a new multivalued field
in the index. This will improve performance by many factors since the single hardest operation is loading and parsing all the HTML-pages from the binaries. But it will make it difficult (impossible?) to improve ranking by finding images that are close to the matching terms in the document. 


 
