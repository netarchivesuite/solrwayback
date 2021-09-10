# SolrWayback

## SolrWayback 4.1.1 software bundle has been released
SolrWayback bundle release for 4.1.1 here: https://github.com/netarchivesuite/solrwayback/releases/tag/4.1.1

## About SolrWayback

SolrWayback is a web application for browsing historical harvested ARC/WARC files similar
to the Internet Archive Wayback Machine. The SolrWayback uses on a Solr server with ARC/WARC files indexed using the warc-indexer.

SolrWayback comes with additional features:
* Free text search in all resources (HTML pages, PDFs, metadata for different media types, URLs, etc.)
* Interactive link graph (ingoing/outgoing) for domains.
* Export of search results to a WARC file. Streaming download, no limit of size of resultset.
* CSV text export of searc result with custom field selection.
* Wordcloud generation for domain.
* N-gram search visuzalisation.
* Visualization of search result by domain.
* Visualization of various domain statistics over time such as size, number of in and out going links.
* Large scale export of linkgraph in Gephi format. (See https://labs.statsbiblioteket.dk/linkgraph/)
* Image search similar to google images.
* Search by uploading a file. (e.g., image, PDF) See if the resource has been harvested and find HTML pages using the image.
* View all fields indexed for a resource and show warc-header for records.
* Configure alternative playback engine to any playback engine using the playback-API such as OpenWayback or pywb.


## Live demo
The National Széchényi Library of Hungary has kindly set up the following demo site for SolrWayback <br>
http://webadmin.oszk.hu/solrwayback/


## See also
Warc-indexer: https://github.com/ukwa/webarchive-discovery/tree/master/warc-indexer<br>
The Warc indexer is the indexing engine for documents in SolrWayback. It is maintained by the British Library.

Netsearch(Archon/Arctika): https://github.com/netarchivesuite/netsearch<br>
Archon/Actika is a book keeping application for warc-files and can start multiple concurrent warc-indexer jobs for large scale netarchives. 

## SolrWayback Screenshots

<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/solrwayback_search.png?raw=true" />
</p>
<p align="center">
 Lising of search results with facets.
</p>

<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/image_search.png?raw=true"/>
</p>
<p align="center">
  Image search, show only images as results.
</p>

<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/solrwayback_playback.png?raw=true" />
</p>
<p align="center">
Solrwayback showing the playback of an archived webpage with playback toolbox overlay.
</p>

<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/solrwayback_linkgraph.png?raw=true" />
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
Search in images by gps location in images having exif location information about the location.
</p>

<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/solrwayback_wordcloud.png?raw=true" />
</p>
<p align="center">
Generate a wordcloud for a domain
</p>

<p align="center"> 
   <img src="https://github.com/netarchivesuite/solrwayback/blob/master/doc/solrwayback_ngram.png?raw=true" />
</p>
<p align="center">
n-gram visualization of results by year, relative to the number of results that year.
</p>


## API
The API for linking to and browsing archived webpages is the same as for Internet Archive:

* Internet Archive: https://web.archive.org/web/20080213093319/http://www.statsbiblioteket.dk/
* SolrWayback: http://server/solrwayback/services/web/20140515140841/http://statsbiblioteket.dk/
 

## Improving playback with the SolrWayback Root servlet
For moderne browsers that supports Serviceworkers, the root servlet will be obsolete. But for better playback in legacy browsers, the
root servlet can improve the playbackback. See https://caniuse.com/serviceworkers if the browser supports serviceworkers.


Installing the root-servlet will improve playback of sites that are leaking URLs. The root-servlet will
catch relative leaks (same domain) even without using proxy mode. The leaks will then be redirected back into SolrWayback to the correct URL and crawltime.
The root-servlet is included in the bundle install. In Tomcat it must be named ROOT.war.
Link to SolrWayback root proxy:
https://github.com/netarchivesuite/solrwaybackrootproxy
Absolute URL live-leaks (starting with http://domain...) will not be caught and can leak to the open web. Open network (F12) to see if any resources are leaking, or turn-off the internet connection to be sure there is no live leaks during playback.

 
 
## Requirements
 * Works on macOS/Linux/Windows.  
 * JDK 8/9/10/11 
 * A nice collection of ARC/WARC files or harvest your own with Heritrix, Webrecorder, Brozzler, Wget, etc. 
 * Tomcat 8+ or another J2EE server for deploying the WAR-file
 * A Solr 7.X server with the index build from the Arc/Warc files using the Warc-Indexer version 3.2.0-SNAPSHOT +
 * (Optional) chrome/(chromium) installed for page previews to work. (headless chrome) 
 
## Build and usage
 * Build the application with: `mvn package`
 * Deploy the `target/solrwayback-*.war` file in a web-container
 * Copy `src/test/resources/properties/solrwayback.properties` and `/src/test/resources/properties/solrwaybackweb.properties`
   to `user/home/` folder for the J2EE server
 * Modify the property files. (default all urls http://localhost:8080)
 * Open search interface: http://localhost:8080/solrwayback

## Contact
Thomas Egense (thomas.egense@gmail.com) 
Feel free to send emails with comments or questions.

# SolrWayback software bundle 4 install guide

With this download you will be able to index, search and playback web pages from your WARC files.
The bundle contains Solr, the warc-indexer tool and SolrWayback installed on a Tomcat webserver.
Just unzip the bundle and copy two files to your home directory and explore your WARC files. 

## Download
Download : https://github.com/netarchivesuite/solrwayback/releases/download/4.1.0/solrwayback_package.zip

Unzip and follow the instructions below.
 

## Install instructions

### 1) INITIAL SETUP  
Properties:  
Copy the two files `properties/solrwayback.properties` and `properties/solrwaybackweb.properties` to your HOME folder (or the home-folder for Tomcat user)

Optional: For screenshot previews to work you may have to edit `solrwayback.properties` and change the value of the last two properties : `chrome.command`  and `screenshot.temp.imagedir`. 
Chrome(Chromium) must has to be installed for screenshot preview images.  

If there are errors when running a script, try change the permissions for the file (`startup.sh` etc). Linux: `chmod +x filename.sh`

### 2) STARTING SOLRWAYBACK  
SolrWayback requires both Solr and Tomcat to be running. 

#### Tomcat:  

* Start tomcat: `apache-tomcat-8.5.60/bin/startup.sh`  
* Stop tomcat:  `apache-tomcat-8.5.60/bin/shutdown.sh`  
* (For windows navigate to `apache-tomcat-8.5.60/bin/` and type `startup.bat` or `shutdown.bat`)  
* To see Tomcat is running open: http://localhost:8080/solrwayback/  
  
#### Solr:  
* Start solr: `solr-7.7.3/bin/solr start`  
* Stop solr: `solr-7.7.3/bin/solr stop -all`  
* (For windows navigate to `solr-7.7.3/bin/` and type `solr.cmd start` or `solr.cmd stop -all`)    
* To see Solr is running open: http://localhost:8983/solr/#/netarchivebuilder  

### 3) INDEXING
SolrWayback uses a Solr index of WARC files to support freetext search and more complex queries.  
If you do not have existing WARC files, see steps below on harvesting with wget.        

The script `warc-indexer.sh` in the `indexing`-folder allows for multi processing and keeps track of already
indexed files, so the collection can be extended by adding more WARCs and running the script again.


Call `indexing/warc-indexer.sh -h` for usage and how to adjust the number of processes to use for indexing.
Example usage:
```
THREADS=20 ./warc-indexer.sh warcs1
```

This will start indexing files from the warcs1 folder using 20 threads. Assigning a higher number of threads than CPU
cores available will result in slower indexing.  Each indexing job require 1GB ram, so this can also be a limiting factor.

The script keeps track of processed files by checking if a log from a previous analysis is available. The logs are stored
in the `status`-folder (this can be changed using the `STATUS_ROOT` variable). To re-index a WARC file, delete the
corresponding log file.

The script `warc-indexer.sh` is not available for Windows. For that platform only a more primitive script is provided that also works for Linux/MacOs.
1. Copy ARC/WARC files into folder: `indexing/warcs1`  
2. Start indexing:  call `indexing/batch_warcs1_folder.sh` (or batch_warcs1_folder.bat for windows)


Indexing can take up to 20 minutes for 1GB warc-files. After indexing, the warc-files must stay in the same folder since SolrWayback is using them during playback etc.  

Having whitespace characters in WARC file names can result in pagepreviews and playback not working on some systems.
There can be up to 5 minutes delay before the indexed files are visible from search. Visit this url after index job have finished to commit them instantly: http://localhost:8983/solr/netarchivebuilder/update?commit=true  
There is a batch_warcs2_folder.sh similar script to show how to easily add new WARC files to the collection without indexing the old ones again.

For more information about the warc-indexer see: https://github.com/ukwa/webarchive-discovery/wiki/Quick-Start

## Scaling and using SolrWayback in production environment.
The stand alone Solr-server and indexing workflow using warc-indexer.sh can scale up to 20000 WARC files of size 1GB. Using 20 threads
indexing a collection of this size  can take up to 3 weeks.  This will result in
an index about 1TB having 500M documents and this will require to changing the Solr memory allocation to at least 12GB.
Storing the index on a SSD drive is required to reach acceptable performance for searches.
For collections larger than this limit Solr Cloud is required instead of the stand alone Solr that comes with the SolrWayback Bundle.
A more advanced distributed indexing flow can handled by the archon/arctika index workflow. See: https://github.com/netarchivesuite/netsearch 

 
#### Deleting an Index
If you want to index a new collection into solr and remove the old index.  

1. Stop solr  
2. Delete the folder `solr-7.7.3/server/solr/configsets/netarchivebuilder/netarchivebuilder_data/index` (or rename to `index1` etc, if you want to switch back later)  
3. Start solr  
4. Start the indexing script

### Faster indexing
A powerful laptop can handle up to 8 simultaneous indexing processes with Solr running on the same laptop. 
Using an SSD for the Solr-index will speed up indexing and also improve search/playback performance.

#### Solrwayback control GUI (Windows only)
For Windows users there is a executable GUI setup program that will start tomcat/solr and copy properties to the home directory.
From the GUI you can select WARC files with a file choose and start indexing. Click the /addOn/SolrSetup.exe file to start GUI.

For more information see: https://github.com/MadsGreen/SolrSetup/

### 4) SEARCHING AND ADDITIONAL FEATURES  
Click the question mark in the search-field to get help with the search syntax for more complex queries and using 
field queries.

The toolbar icon opens a menu with the available tools.


### 5) CREATING YOUR OWN WARCS - HARVESTING WITH WGET  
How to do your own web harvest websites (macOS/Linux only):  

* Using the wget command is an easy way to harvest web sites and create WARC files. The WARC files can then be indexed into SolrWayback.
* Create a new folder, since there will be several files written in this folder. Navigate to that folder in a prompt.
* Create a text file call `url_list.txt` with one URL per line in that folder.  
* Type the following in a prompt:\
  `wget --span-hosts --warc-cdx --page-requisites --warc-file=warcfilename --warc-max-size=1G -i url_list.txt`
  
The script will harvest all pages in the `url_list.txt` file with all resources required for that page (images, CSS, etc.) and be written to a WARC file(s) called `warcfilename.warc`.
The optional `--span-hosts` parameter will also harvest resources outside the domain of the page and can be removed 

* To perform a multi-level harvest:\
  `wget --span-hosts --level=1 --recursive --warc-cdx --page-requisites --warc-file=warcfilename --warc-max-size=1G -i url_list.txt`\
  where `level=1` means "starting URLs and the first level of URLs linked from the starting URLs".
  This will substantially increase the size of the WARC file(s).


