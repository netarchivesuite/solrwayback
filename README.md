# SolrWayback

## SolrWayback 3.2 software bundle released
Scroll down to the install guide below and follow the instructions.


## About SolrWayback
Solrwayback will only work with warc-indexer version 3.0+

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


## Root servlet. Catching and forwarding live leaks.
Installing the root-servlet will improve playback of sites that are leaking urls. The root-servlet will
catch relative leaks (same domain) even without using proxy mode. Live leakes that are absolute (http://...) will
only be catch if running proxy mode. The leaks will then be redirected back into SolrWayback to the correct URL and crawltime.
The root-servlet is included in the bundle install. In Tomcat it must be named ROOT.war.
Link to SolrWayback root proxy:
https://github.com/netarchivesuite/solrwaybackrootproxy


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

Feel free to send emails with comments and questions.

## Warc-indexer/Solr 
All entries from the arc/warc files are indexed as separate documents using the WARC-Indexer and using the lucene schema required by the WARCIndexer.

A document is Solr can be html, image, video, audio, js etc. (content_type_norm)
Filename and offset of a document in warc files is stored in solr and will be used and loaded during playback.
 

# SolrWayback software bundle 3.2 install guide


With this download you will be able to index, search and playback web pages from your warc-files.
The bundle contains Solr, the warc-indexer tool and Solrwayback installed on a Tomcat webserver.
Just unzip the bundle and copy two files to your home directory and explore your warc files. 

## Download
Download : https://github.com/netarchivesuite/solrwayback/releases/download/3.2/solrwayback_package.zip  
Unzip and follow the instructions below.
 

## Requirements:
Works on MacOs/Linux/Windows.  
For the Solrwayback software bundle you only need to have Java 8 (64 bit) installed. 
To check java is installed, type the following from a prompt: java -version  
Any version 1.8+ will be compatible.  

## Install instructions:

### 1) INITIAL SETUP  
Properties:  
Copy the two files solrwayback.properties and solrwaybackweb.properties to your HOME folder.

Optional: For screenshot previews to work you may have to edit solrwayback.properties and change the value of the last two properties : chrome.command  and screenshot.temp.imagedir. 
Chrome(Chromium) must has to be installed for screenshot preview images.  

If there are errors when running a script, try change the permissions for the file (startup.sh etc). Linux: chmod +x filename.sh

### 2) STARTING SOLRWAYBACK  
Solrwayback requires both Solr and Tomcat to be running. 

Tomcat:  
Start tomcat: apache-tomcat-8.5.29/bin/startup.sh  
Stop tomcat:  apache-tomcat-8.5.29/bin/shutdown.sh  
(For windows navigate to apache-tomcat-8.5.29/bin/ and type startup.bat or shutdown.bat )  
To see Tomcat is running open: http://localhost:8080/solrwayback/  
This is the search interface frontpage  

Solr:  
Start solr: solrwayback_package/solr-7.3.1/bin/solr start  
Stop solr: solrwayback_package/solr-7.3.1/bin/solr stop  
(For windows navigate to solrwayback_package/solr-7.3.1/bin/ and type solr.cmd start or solr.cmd stop)    
To see Solr is running open: http://localhost:8983/solr/#/netarchivebuilder  

### 3) INDEXING
Solrwayback uses a Solr index of warc files and makes the archived webpages searchable and viewable.  
If you do not have existing warc files, see steps below on harvesting with wget.        

Creating an index:
Copy arc/warc files into folder: /solrwayback_package/indexing/warcs  
Start indexing:  call indexing/batch_warc_folder.sh  
Indexing can take up to 10 minutes/GB files. After indexing, the warc-files must stay in the same folder for solrwayback to work.  
Having whitespace characters in warc file names can result in pagepreviews and playback not working on some OS.
There can be up to 5 minutes delay before the indexed files are visible from search. Visit this url after index job have finished to commit them instantly: http://localhost:8983/solr/netarchivebuilder/update?commit=true  

Deleting an Index:  
If you want to index a new collection into solr and remove the old index.  
1) stop solr  
2) delete the folder:   
solr-7.3.1/server/solr/netarchivebuilder/netarchivebuilder_data/index  
(or rename to index1 etc, you if later want to switch back)  
3) start solr  
4) start the indexing script. 

Faster indexing:  
Copy batch_warc_folder.sh and rename to batch_warc1_folder.sh . Also create a warcs1 folder. Edit the batch_warc1_folder.sh script and change the foldername from warcs to warcs1.  
This way you can start two index jobs running at the same time. A powerful laptop can handle up to 4 simultaneous indexing processes. 
You can also just add more warc files to the index without having to index all content in the warc folder again.  
Major solrwayback performance improvement for searching and playback by having the solrwayback_package folder on a SSD.  


### 4) SEARCHING AND ADDITIONAL FEATURES  
Solrwayback provides a search interface to explore the content of the warc files that have been indexed.  
The basic query in solrwayback will return all documents in the index: *:*  
Results can then be filtered by fields like Domain, Content Type Norm, Crawl Year, Status Code, Public Suffix (tld).  
Advanced queries can be constructed using Boolean terms, and searching fields eg: domain:statsbiblioteket.dk AND title:"Aarhus University Library"  

Additional features include:  
- Image search similar to google images  
- Search by uploading a file. (image/pdf etc.) See if the resource has been harvested and from where.  
- Link graph showing links (ingoing/outgoing) for domains using the D3 javascript framework.  
- Raw download of any harvested resource from the binary Arc/Warc file.  
- Export a search resultset to a Warc-file. Streaming download, no limit of size of resultset.  
- An optional built in SOCKS proxy can be used to view historical webpages without browser leaking resources from the live web. Configure browser to SOCKS version 4 to localhost port 9000 and open http://localhost:8080/solrwayback/  

For more info on all additional features, see https://github.com/netarchivesuite/solrwayback

### 5) TO CREATE YOUR OWN WARCS - HARVESTING WITH WGET  
How to do your own web harvest websites (Linux/MacOS only):  
Using the wget command is an easy way to harvest websites compared to using Heritrix. The warc-files can then be indexed into solrwayback.  
Create a new folder since there will be several files written in this folder. Navigate to that folder in a prompt.  
Create a text file call url_list.txt with one URL pr. line in that folder.  
Type the following in a prompt:  
wget  --level=0  --warc-cdx   --page-requisites --warc-file=warcfilename --warc-max-size=1G -i url_list.txt    
(rename the warcfilename to your liking)  
The script will harvest all pages in the url_list.txt file with all resources required for that page (images, css etc.) and be written to a warc file(s) called warcfilename.warc  
Change --level=0 to --level=1 for following links. This will substantially increase the size of the warc file(s).  



