# SolrWayback

## SolrWayback 4.0 software bundle will be released in November 2020.
Prerelease SolrWayback bundle for 4.0 here: https://github.com/netarchivesuite/solrwayback/releases/tag/4.0.3





## About SolrWayback

SolrWayback is web-application for browsing historical harvested ARC/WARC files similar
to the Internet Archive Wayback Machine. The SolrWayback depends on a Solr server with
Arc/Warc files indexed using the British Library WARC-Indexer. 
Unlike the Wayback Machine the SolrWayback does not need the
CDX-server with meta data for the harvest. It only uses the Solr server and the raw
Arc/Warc files.

 Warc-indexer: https://github.com/ukwa/webarchive-discovery/tree/master/warc-indexer<br>
 Netsearch(Archon/Arctika): https://github.com/netarchivesuite/netsearch<br>
 Archon/Actika is a bookbooking and  can start multiple  concurrent warc-indexer jobs for large scale netarchives. 


SolrWayback comes with additional features:
* Image search similar to google images
* Search by uploading a file. (image/pdf etc.) See if the resource has been harvested and from where.
* Dynamic Link graph (ingoing/outgoing) for domains.
* Export a search resultset to a Warc-file. Streaming download, no limit of size of resultset.
* CSV text export of result with custom field selection.
* Wordcloud generation for domain
* N-gram search visuzalisation
* Visualization of search result by domain.
* Large scale export of linkgraph in Gephi format. 



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

 


## Root servlet. Catching and forwarding live leaks.
Installing the root-servlet will improve playback of sites that are leaking urls. The root-servlet will
catch relative leaks (same domain) even without using proxy mode. The leaks will then be redirected back into SolrWayback to the correct URL and crawltime.
The root-servlet is included in the bundle install. In Tomcat it must be named ROOT.war.
Link to SolrWayback root proxy:
https://github.com/netarchivesuite/solrwaybackrootproxy


## API
The API for linking to and browsing archived webpages is the same as for Internet Archive:<br>

Internet Archive:https://web.archive.org/web/20080213093319/http://www.statsbiblioteket.dk/ <br>
SolrWayback: http://server/solrwayback/services/web/20140515140841/http://statsbiblioteket.dk/ <br>

If using a Solr search based web archive such as Shine (https://github.com/netarchivesuite/shine) or Warclight (https://github.com/archivesunleashed/warclight) you only need to change to property pointing from the wayback server to the SolrWayback server.
 
 
 
## Requirements
 * JDK 8/9. (Not working on Java 10+ yet) 
 * A nice collection of Arc/Warc files or harvest your own with Heritrix,Webrecorder,Brozzler, Wget etc. 
 * Tomcat 8+  or another J2EE server for deploying the WAR-file
 * A Solr 7.1+ server with the index build from the Arc/Warc files using the Warc-Indexer version 3.1.0+ (Version 3.0.0 does not handle some transfer encodings correct)
 * (Optional) chrome/(chromium) installed for page previews to work. (headless chrome) 
 
## Build and usage
 * Build the application with: `mvn package`
 * Deploy the `target/solrwayback-*.war` file in a web-container
 * Copy `src/test/resources/properties/solrwayback.properties` and `/src/test/resources/properties/solrwaybackweb.properties`
   to `user/home/` folder for the J2EE server
 * Modify the property files. (default all urls http://localhost:8080)
 * Open search interface: http://localhost:8080/solrwayback

## Contact
* Thomas Egense (thomas.egense@gmail.com) 

Feel free to send emails with comments and questions.

## Warc-indexer/Solr 
All entries in Warc files are indexed as separate documents using the WARC-Indexer in Solr with the defined schema (solrconfig.xml)
A document is Solr can be html, image, video, audio, js etc. (`content_type_norm`)
Filename and offset of the entries  warc files is stored in solr and will be used and loaded during playback.
 

# SolrWayback software bundle 4.0 install guide


With this download you will be able to index, search and playback web pages from your warc-files.
The bundle contains Solr, the warc-indexer tool and Solrwayback installed on a Tomcat webserver.
Just unzip the bundle and copy two files to your home directory and explore your warc files. 

## Download
Download : https://github.com/netarchivesuite/solrwayback/releases/download/3.2.1/solrwayback_package.zip  
Unzip and follow the instructions below.
 

## Requirements:
Works on MacOs/Linux/Windows.  
For the Solrwayback software bundle you only need to have Java  (64 bit) installed. 
Java version 8 is required for the indexing job, but not for Solr or Tomcat that can run on java 11.
To check java is installed, type the following from a prompt: java -version  


## Install instructions:

### 1) Upgrade from 3.x

To update from 3.x add the new additional properties in `solrwaybackweb.properties` and `solrwayback.properties`. Download the release and to see the new properties. Some properties has been removed or renamed.
Replace both war-file in tomcat with this those in this release(solrwayback.war+ROOT.WAR) and restart tomcat.

### 1) INITIAL SETUP  
Properties:  
Copy the two files `src/test/resources/properties/solrwayback.properties` and `/src/test/resources/properties/solrwaybackweb.properties` to your HOME folder (or the home-folder for Tomcat user)

Optional: For screenshot previews to work you may have to edit `solrwayback.properties` and change the value of the last two properties : `chrome.command`  and `screenshot.temp.imagedir`. 
Chrome(Chromium) must has to be installed for screenshot preview images.  

If there are errors when running a script, try change the permissions for the file (`startup.sh` etc). Linux: `chmod +x filename.sh`

### 2) STARTING SOLRWAYBACK  
Solrwayback requires both Solr and Tomcat to be running. 

Tomcat:  
Start tomcat: `apache-tomcat-8.5.60/bin/startup.sh`  
Stop tomcat:  `apache-tomcat-8.5.60/bin/shutdown.sh`  
(For windows navigate to `apache-tomcat-8.5.60/bin/` and type `startup.bat` or `shutdown.bat`)  
To see Tomcat is running open: http://localhost:8080/solrwayback/  
  
Solr:  
Start solr: `solrwayback_package/solr-7.7.3/bin/solr start`  
Stop solr: `solrwayback_package/solr-7.7.3/bin/solr stop`  
(For windows navigate to `solrwayback_package/solr-7.7.3/bin/` and type `solr.cmd start` or `solr.cmd stop`)    
To see Solr is running open: http://localhost:8983/solr/#/netarchivebuilder  

### 3) INDEXING
Solrwayback uses a Solr index of warc files to support freetext search and more complex queries.  
If you do not have existing warc files, see steps below on harvesting with wget.        

Copy arc/warc files into folder: `/solrwayback_package/indexing/warcs1`  
Start indexing:  call `indexing/batch_warcs1_folder.sh` (or batch_warcs1_folder.bat for windows)
Indexing can take up to 20 minutes for 1GB warc-files. After indexing, the warc-files must stay in the same folder since SolrWayback is using them during playback etc.  
Having whitespace characters in warc file names can result in pagepreviews and playback not working on some OS.
There can be up to 5 minutes delay before the indexed files are visible from search. Visit this url after index job have finished to commit them instantly: http://localhost:8983/solr/netarchivebuilder/update?commit=true  
There is a batch_warcs2_folder.sh similar script to show how to easy add new warc-files to the collection without indexing the old ones again.
Or you can use the command in the batch_warcs2_folder.sh(bat) to see how to just index a single warc-file with the warc-indexer java command. 

Deleting an Index:  
If you want to index a new collection into solr and remove the old index.  
1) stop solr  
2) delete the folder:   
`solr-7.7.3/server/solr/netarchivebuilder/netarchivebuilder_data/index`  
(or rename to `index1` etc, if you want to switch back later)  
3) start solr  
4) start the indexing script. 

Faster indexing:  
A powerful laptop can handle up to 6 simultaneous indexing processes with Solr running on the same laptop. 
Using an SSD for the Solr-index will speed up indexing and also improve search/playback performance drastic.   


### 4) SEARCHING AND ADDITIONAL FEATURES  
Click the question mark in the search-field to get help with the search syntax for more complex queries and using 
field queries.
The toolbar icon opens a menu with the available tools.


### 5) TO CREATE YOUR OWN WARCS - HARVESTING WITH WGET  
How to do your own web harvest websites (Linux/MacOS only):  
Using the wget command is an easy way to harvest websites and build warc-files. The warc-files can then be indexed into SolrWayback.  
Create a new folder since there will be several files written in this folder. Navigate to that folder in a prompt.  
Create a text file call `url_list.txt` with one URL pr. line in that folder.  
Type the following in a prompt:  
`wget  --span-hosts  --level=0 --recursive --warc-cdx   --page-requisites --warc-file=warcfilename --warc-max-size=1G -i url_list.txt`    
  
The script will harvest all pages in the `url_list.txt` file with all resources required for that page (images, css etc.) and be written to a warc file(s) called `warcfilename.warc`  
Change `--level=0` to `--level=1` for following links. This will substantially increase the size of the warc file(s).  
The optional  --span-hosts parameter will also harvest resources outside the domain of the page and can be removed 


