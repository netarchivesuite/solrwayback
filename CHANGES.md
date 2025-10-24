# SolrWayback changelog

UNRELEASED
-----
* Remove port in the url_norm field.  This wil fix playback for very old ARC files from IA (1995-2002). They have port:80 in the ARC HTTP-header. 
This require the index has been build with warc-indexer version 3.4.0+. If normalisation mode is set to LEGACY in solrwayback.properties the port will not be removed.
See https://github.com/ukwa/webarchive-discovery/issues/284 for more information

* Added functionality to the DOMAIN STATS tool in the toolbox. Added a visualisation for average context length over time and a toggle button for showing the graphs combined or on individual canvases. Thanks to @VictorHarbo for this feature
* Added start script to start both Tomcat and Solr in root folder of SolrWayback Bundle (Linux+Windows)
* Fixed some unittest that depended on System Locale or propertity files in home folder. Closing https://github.com/netarchivesuite/solrwayback/issues/475

5.2.1
-----
* Fixed memento null-pointer.

5.2.0
-----
* Upgraded solr dependencies from v9.1.0 to v9.4.1
* HTML pages with geo tag will no longer be found in image GEO search.
* Fixed Gephi export regression bug, not all results was extracted due to Gephi also was limit by CSV export limit size in property file.
* Added SolrWayback ASCII logo in log file when started successfully.
* Added support for Memento API, including timegates and timemaps. Memento properties added to solrwayback.properties (Thanks @VictorHarbo ) 
* Two new memento properties added in solrwayback.properties. Will use same default values if not defined in property file.
* Removed Jetty 'mvn jetty:run' as development option and switched to 'mvn cargo:run that will start a Tomcat instead. Routing was not working in Jetty. See README.md for details how to use.
* Upgrade from deprecated HttpSolrClient to HttpJdkSolrClient compatible with Http1 and Http2.
* Download button added to toolbox n-gram to download data in csv format. 


5.1.2
-----
Bug fix. Chunking was not removed in all cases. This was only relevant for WARC-files that are created with chunking. (not Heritrix) 
Dockerfile has been updated to build SolrWayback bundle 5.1.0. (Will be upgraded each release) See: https://github.com/netarchivesuite/solrwayback/pull/456 implemented by @c-vandendyck-kbr
Geo search was not working for Solr 9.4 in cloud mode. Solr function query syntax rewrite was required and it also is backwards compatible with Solr7.

5.1.1
-----
Little cleanup in log messages due to shard-splitting to avoid repeated stack traces.
Solr9 bug temporary bug fix due to invalid Json from Solr.  See:https://github.com/netarchivesuite/solrwayback/issues/449


5.1.0
-----
Substatial speed up when exporting (csv,warc etc.) from large multi sharded collections. See https://github.com/netarchivesuite/solrwayback/issues/329  (Thanks Toke Eskildsen) This feature still needs a little more testing. Feedback will be welcome.

Minor tweaking of log info/debug. Less log lines in default solrwayback.log when running with log level INFO.
Fix regression bug where "page resources" was not showing missing resources for the webpage.

Updated the bundle install documentation. Added new section how to redeploy the Solr configuration.

5.0.0
-----
Upgrade Java 1.8 → 11, Tomcat 8.5 → 9 and Solr 7 → 9. SolrWayback 5.0.0 is backwards compatible with existing Solr 7 installations.

Better guide for using start and stop scripts.

Fixed csv/json export when more than 1 facet was selected. (regression bug... sorry)

warc-indexer.sh script will now also finds arc files when searching recursive(thanks to @fedorw) 

Frontend third-parties dependencies updated.


4.4.3
-----

Add Zip Export feature. It is now possible to extract raw files from SolrWayback in a combined zip file. This could for example be used to extract all HTML content, images, video etc. from a search result. (github #382 and #245). Add additional property in solrwaybackweb.properties to increase the default max file limit: export.zip.maxresults=1000000

Docker support. The docker file will install the SolrWayback in the docker container. You can index WARC files from a folder outside the docker contain. See the docker file for documentation. (Thanks to Trym Bremnes for this PR)

Query hints fix (range queries). The search validation helper did like range queries and showed warning when they was correct. (github #380)
Remove an error message that would be shown while waiting to load "Page resources" 

CTRL+click on a facet will open the search-result in a new tab. On macOS use CMD+click.  (github #404)

Setting encoding to UTF-8 when indexing into Solr using the indexing scripts in the bundle install. Some OS/docker containers may not have UTF-8 as default.


4.4.2
-----
The SolrWayback default playback engine can be changed by defining a new property in solrwaybackweb.properties: playback.primary.engine
Use if you have a PyWb playback engine etc.

The property in solrwaybackweb.properties for alternative playback engine (icon next to title in search result) has been renamed to: playback.alternative.engine
The old misleading name 'openwayback.baseurl' will still work, but will be removed in a future version. (Will give a WARN in log-file during restart of the Tomcat)

The warc-indexer.sh script in the SolrWayback bundle will use 'curl' instead of 'wget'. This will always work on macOS that may not have 'wget' installed

Playback fix for legacy warc-indexer (v3.0+v3.1). This is not relevant for anyone but Royal Danish Library.

Fixed invalid SSL certificate for dependency. (https://maven.restlet.com -> https://maven.restlet.talend.com). Thanks to @fedorw 


4.4.1
-----
Increased precision on estimated number of results when performing a grouped search. This will come with a  small performance cost in search time on SolrCloud.

Clicking on SolrWayback logo will take you to the SolrWayback homepage(start page). 

The result set can now be sorted on the crawl_date field ascending or descending. Ranking(score) is still the default sort. (see https://github.com/netarchivesuite/solrwayback/issues/311).Thanks to Leslie Bellony from BnF for implementing this.

The Ngram viewer in the toolbar can now also be refined by day,week and month. Thanks to Leslie Bellony from BnF for implementing this.

The default number of 20 results on each page can be changed with this new property in solrwaybackweb.properties. (See https://github.com/netarchivesuite/solrwayback/issues/313). Thanks to Leslie Bellony from BnF for implementing this.

search.pagination=20

Bugfix: The property key for expanded warc limit was the same as for non-expanded

Default log configuration will create and additional log file called solrwayback_error.log. This will only contain log lines of severity 'error'.  

Hardening against Out Of Memory in export and playback of very large webpages. Added new property for max size of HTML that are parsed for export in solrwaybackweb.properties:

warc.entry.text.max.characters=100000000

Search boost ranking for frontpages(url_type:slashpage^5) changed in SolrWayback bundle. Change in solrconfig.xml for existing indexes to enable this boost.

Export fields improvement. Can now also export to JSON and JSONL besides CVS. Option to flatten fields. The grouping (url_norm) if selected in query will also be used in export. ( see https://github.com/netarchivesuite/solrwayback/issues/279)

Bugfix: Caching was effectively disabled when setting maxAgeSeconds to a year or more

Added two new services methods to extract statistics about the collection (services/frontend/statistics/querystats and services/frontend/statistics/percentilestats). At a later point there will be added GUI support for these two methods.

The forced uppercase in titles for search results are instead using the original title case.

Frontend dependencies security updated (again...).

4.4.0
-----

'Visualization of search result by domain' can now be shown by day,week and month instead 
of only year. Same goes for the domain statistics in the toolbar. This is useful for recent collection that does not go back years.   
(see https://github.com/netarchivesuite/solrwayback/issues/270)
Thanks to Leslie Bellony from BnF for implementing this) 

Support for multiple different(urls) playback engines. This require the WARC files has been indexed 
with the collection or collection_id parameter. (See README.md). Each collection that then be mapped to a playback engine 
url. It is possible to use a template pattern for collection or collection_id to avoid 
creating multiple mappings. See documentation in solrwaybackweb.properties  

Cache invalidation based on background checks for index changes. Better solr-index caching implementation. Using a Solr query that detect changes that  
can be defined to run at given interval. (see documentation in solrwayback.properties).
The new property can be added to solrwayback.properties:

solr.server.check.interval.seconds=60


Fixed double calling of service metod when using url_search. (had no effect on users)  (https://github.com/netarchivesuite/solrwayback/issues/258)

Fixed frontend encoding bug for url-search. (https://github.com/netarchivesuite/solrwayback/issues/232)

Fixed frontend encoding bug for exporting result. (https://github.com/netarchivesuite/solrwayback/issues/271)

Fixed frontend encoding bug (https://github.com/netarchivesuite/solrwayback/issues/259)

Fixed frontend redundant service call (https://github.com/netarchivesuite/solrwayback/issues/257)

Major rewrite of all streaming logic that is used for the various export methods and 
solr queries. There is now support for export a group'ed search result and image search.  (Still needs to be implemented in frontend.)
Thanks for Toke Eskildsen for this major improvement and cleanup/refactoring of the streaming logic.

Export results supports in format json and jsonl besides csv (not implemented in frontend yet) 

Improvemented expanded WARC-import. Redirects would sometime result in some required 
resources  would not be included the WARC file.  
 
Building the project can be done on Windows platform again. (https://github.com/netarchivesuite/solrwayback/issues/290)

Lenient playback (still in testing/beta mode). This can tested by changing the playback url from '/web/' to '/lenient/web/'
Lenient playback will match url's not found using url_norm by allowed only a subset  
of query param/values to match. This will solve the common playback issue with a dynamic 
generated timestamp added as a parameter etc. This idea is similar to the prefix match configuration  
in pywb.  

Bug fix. Exporting resultset to WARC file now also works for WARC type 'resource'.   

Precision for total number of hits when using grouped search has been increased at a cost of slightly slower search time. Label text now also tell is it is an approximated number of results.

Frontend dependencies security updated (again...).


4.3.1
-----
New propety must be added to the solrwaybackweb.properties: 

collection.text.file=/about_collection.txt

Just as with the about-text this can be customized and point to a file on the local disk.
The "About this archive" text has been changed to "About us". This is intended for short information 
about the institution. While "About the Collection" should contain text of the corpus 
behind the collection and curator information about crawl-years etc.


Playback improvement. Fixed some redirect/url-parsing bugs in the ROOT.war (solrwaybackrootproxy).  https://github.com/netarchivesuite/solrwayback/issues/231
 
Playback improvement: Queries for page resource resolving are now properly quited, avoiding a scenario where resolving of all page resources failed. https://github.com/netarchivesuite/solrwayback/issues/230

Playback improvement: image data-src URLs are now bypassed is resource URL rewriting and are thus supported for playback. https://github.com/netarchivesuite/solrwayback/issues/230

The SolrWaybackRootProxy (ROOT.war) in the Software bundle has been updated to fix rare playback issues.

GUI improvement. Use escape-key to close all modal pop-ups. (toolbox, search syntax, full size 
images)




4.3.0
-----
Updated frontend dependencies (security)

Added support for WARC file reading with Inputstream, this can be used if WARC files are not on a file-system. (Skipping HttpInputStream implementation for reading WARCs with offset)

Warc-indexer not supports WARC type 'resource' without URL in WARC header  (Typical created with Warcit from a file system)

Minor Solr query syntax fix, so it will also work on Solr 6. (not recommended to use Solr6!)

Docker support. Only recommended for trying a demo SolrWayback if you do not want to use the bundle install. See README for more info.

Support for legacy WARC-Indexer before version 3.0 that does not have url_norm field. (not recommended)

Fixed n-gram to show statistics for years after 2020.

SolrWayback can now be deployed at a deeper url than 'https://kb.dk/solrwayback' . Etc. : 'https://kb.dk/covid-collection/solrwayback'.

If the webapp base above is not just domain/solrwayback, then an additional property needs to be defined in solrwaybackweb.properties. In the case above the property : webapp.prefix=/covid-collection/solrwayback/  . See more in the README.md how to install SolrWayback under another subdirectory after domain. If the property is not defined it will default to /solrwayback/

When loading binaries from WARC-file+offset check that the the resource is in the collection (in Solr). This will prevent URL hacking from guessing WARC-files and offset that is not in the collection but on the file-system. This can be enabled by new a property. Enabled this property will have minor performance impact on playback.


Solr memory increased from 512MB to 1024MB in SolrWayback bundle. Some large text blocks in WARC files could cause Solr memory error with multiple threads.

###### New properties:

New optional property in solwayback.properties 'fields'. Will default to all fields if not defined. Use comma seperated list of fields to be shown when clicking "Show Data fields" from the results page.

New optional property in solwayback.properties 'url.normaliser'. Will default to normal. Other options are minimal and legacy. 

New optional property in solwayback.properties 'solr.search.params'. Add default solr params to every query.

New optional property in solwayback.properties 'warc.files.verify.collection'. Default false. Will check WARC file +offset is in the collection before returning binaries.

New optional property in solrwayback.properties 'playback.disabled'.Default false. If set to true all playback and access to binaries (pdf, full size images etc.) will be disabled. Will only allow images tumbnail preview 200*200 pixels.

New optional property in solrwaybackweb.properties 'search.uploaded.file.disabled'. Default false. If set to true search by fileupload (hash-value) will be disabled.

New optional property in solrwaybackweb.properties 'webapp.prefix'. Default to /solrwayback/. If SolrWayback is deployed at kb.dk/covid-19/solrwayback, then set this property to '/covid-19/solrwayback/'


4.2.3
-----
Fixed in-player video player for some MP4 videos that was classified by Tika as 'application/mp4'.
Fixed log4shell vulnerability in SolrWayback bundle (Solr and warc-indexer)


4.2.2
-----
Support for Warc record type 'resource'. Also required fix in the warc-indexer and resourcetype added to config3.xml (in indexing folder)
Improved playback for Twitter API harvest (https://github.com/netarchivesuite/so-me). (also changes in solrconfig.xml)
Implemented new WARC file resolver. If WARCS files are removed after indexed, you can add a text file with the new location. Whenever a WARC needs needs to be loaded, if the WARC file is on the list, it will use that location instead of the one indexed into Solr.

4.2.1
-----
Further improvements in serviceworker:
a) The SolrWaybackRoot-servlet application is no longer required if the Serviceworker is loaded. For legacy browsers where servicerworker does not work, the root servlet will required for improved playback.
b) In rare cases referer is missing so crawltime for the origin resource is unknown. As a default it uses current year as crawltime. This situation is often not relevant for playback since the requests often are to trackers and adds.

Cleaned up in logging to the solrwayback.log file. It should not be as spammy now.
Upgraded frontend dependencies   (security updates).   

Fixed bug in load more facets for domain facet when there also was a filter query involved.

4.2.0
-----
All Playback live leaks are now blocked or redirected back to SolrWayback with a javascript Serviceworker added to playback. No more leaking to the live web!  This will also improve playback when the live leak can be resolved in SolrWayback. (Thanks to Ilya Kreymer for pointing me in this direction). 
The Serviceworker implementation require the SolrWayback server to run under HTTPS. This can be archived by setting an Apache or Nginx in front of the Tomcat.
The Serviceworker feature is supported by most recent browser versions. See: https://caniuse.com/serviceworkers 
Playback will still work in legacy browsers using url rewrite, but can leak to the live web in if not blocked by proxy server or sandboxed. 
Encoding fix in javascript rewrite: Modify < > handling to preserve the original representation (including faulty ones). This closes SOLRWBFB-58
Upgraded frontend depencencies (security updates).


4.1.2
-----
Wordcloud stop words works can be configured in solrwaybackweb.properties.
Added new property(wordcloud.stopwords) in solrwaybackweb.properties with default stopwords (english). Will use empty stopword list if not defined
Word cloud html pages extraction reduced from 10.000 to 5.000 as difference was minimal, but doubles performance
API method to extract word+count for a query+filterquery(optional) :  /services/frontend/wordcloud/wordfrequency?q=xxx&fg=yyy
API method to extract wordcloud image for query+filterquery(optional): /services/frontend/wordcloud/query?q=xxx&fg=yyy

Solr query caching for performance boost. 
Added new optional properties in solrwayback.properties
#Solr caching. Will be default false if not defined
solr.server.caching=true
solr.server.caching.max.entries=10000
solr.server.caching.age.seconds=86400

When clicking a link and opening playback in a new tab. The browser URL will match the crawl-time of the html page.

4.1.1
-----
Added a better parallel indexing script for Linux/macOS with more options. (warc-indexer.sh)
With warc-indexer.sh you can define number of threads. It keeps track of already index WARC-file so you can start it again after adding new WARC-files to the folder.
Example: THREADS=20 ./warc-indexer.sh warcs1

The file location of the two property-files solrwayback.properties and solrwaybackweb.properties can be configured so they do not have
to be in the HOME directory. 
To change to location copy this file: https://github.com/netarchivesuite/solrwayback/blob/master/src/main/webapp/META-INF/context.xml
to the folder '/apache-tomcat-8.5.60/conf/Catalina/localhost' and rename it to solrwayback.war
Remnove the uncomment of the environment variables and edit the location of the files. During start up of the tomcat server, the
values will be logged in solrwayback.log.
  
Updated the README.md with more information about scaling and using SolrWayback in production.  

4.1.0
-----
Indexing scripts updated for SolrWayback Bundle
SolrWayback bundle release 4.1.0 uploaded to Github releases

4.0.8
-----
Brotli encoding fix for javascript.
Introduced JavascriptPlayback class. Does nothing but handle brotli, but can later be improved to do url-replacement in javascript files.

4.0.7
-----
Fixed chunked transfer encoding error when HTTP header declared it was chunked, but was not.

New optional properties can be added to solrwaybackweb.properties to limit maximum number of export results.
These values is their default values.

# Limit export size
# 10M for CSV , 1M for warc, 10K for warc-expanded
# For warc.expanded the total number of documents can be many times the max-results size.        

export.csv.maxresults=10000000

export.warc.maxresults=1000000 

export.warc.expanded.maxresults=10000     
        

4.0.6
-----
Start year for domain statistics is not using the archive.start.year value in solrwaybackweb.properties
Hover text added to search guidelines and the 2 questions marks (image/url search)
First production ready of release of SolrWayback 4.x on github. 
README.md various grammer fixes (thanks Mat Kelly!)

4.0.5
-----
Fixed SolrWayback not loading at all in Safari browser (was due to regexp)
Fixed facet query encoding regression error.
Version of Solrwayback added to the web-properties method.

4.0.4
-----
Upgraded vue-cli framework from 4.4.0 to 4.5.9  (frontend framework)
Facets and links below images can be right clicked and open in a new tab.
Spinner added  when loading images in result set for HTML pages.
Realtime search hints if query seems faulty. The following query:  cats and dogs")
   will result in 3 warnings. 1) lowercase 'and' 2) unbalanced quotes 3) unbalanced parentheses


Custom logo+link can be inserted top left corner. Defined in solrwaybackweb.properties

#Show a custom image in top left corner. (png,jpeg,svg). (150x60 pixel) 
#Link when clicking the logo
top.left.logo.image=/kb_logo_desktop_blue.svg
top.left.logo.image.link=https://www.kb.dk/

4.0.3
-----
Gephi link-graph (search toolbox) implemented with new GUI. Description+help text improved.
HTML tags n-gram implemented(seacrh toolbox). Found on same tab as the normal n-gram search(radio button)
Notification if the cap of 500 images is reach for image  geo-search.
Both warc-file exports also have the option to export in warc.gz format now (zipped).
Page resources (playback toolbox) now also shows maximum timespam back- and forward for the resources.


4.0.2
-----
Clean up in logging to the tomcat log-file.
Linkgraph remimplemeted with improved GUI. Less spaghetti for large graphs and local sub-graph highlighting for a node by click. Maximum node degree increased to 40.
CVS export option for the n-gram query visualization
Page resources (playback toolbar) shows total timespan for resources in top so it is easier to see how far apart the resources are from the main page.
Added icon on search result to show warc headers for that record.  
Facet counts are formatted to be more readable for large numbers.(thousand delimiters)
Data fields on records new listed in alphabetical order.
Export to warc file also has option to export to a zipped warc.gz file.(still waiting frontend enabling)
Fixed encoding bug for playback (html content charset) that was introduced in 4.0.1 due to switch to chaining inputstreams.
Backend + GUI refactoring

Added optional property to solrwaybackweb.properties. Default value is  1998 if property not set. Start year of the collection is used in several visualizations.

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




