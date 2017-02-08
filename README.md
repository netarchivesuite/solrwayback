# SolrWayback
Servlet that extracts resources from Warc/Arc-files containing harvested web-pages.
It can be used to enrich a web-search(Solr etc.) result in warc/arc files with images or direct download link of any mime type in the Warc/Arc-files.
A Simple front-end is included to demostrate image search from a solr-server where arc-files has been index using 
* https://github.com/ukwa/webarchive-discovery or
* https://github.com/netarchivesuite/netsearch
* Or you can try the more advanced 'shine' search front-end that uses webarchivemimetypeservlet: https://github.com/netarchivesuite/shine

## Requirements
 * JDK 1.7
 * Maven 3 
 * Some Arc/Warc files 
 * Tomcat 7+  or similar j2ee server to host to WAR-file
 * Optional: Solr-server for searching. The Solr server must return arcfilepath+offset for the documents.
 
## Build and usage
 * Build the application with: mvn package
 * Deploy the webarchivemimetypeservlet.war file in a web-container.
 * For the API open:  localhost:8080/webarchivemimetypeservlet
 * Optional: copy resources/properties/webarchivemimetypeservlet.properties to user/home/ folder for solr search
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

##Image search
A search text is passed to Solr and return a list of documents:
doc1
doc2
...
docn

For each document:
 * If it is an HTML page, load it from the arc-file(on disk) and parse the HTML to find
   images. For each image find the image-document is solr (temporally near) and collect these image documents.
 * If the document already is an image just add it to the collection (and no need to read the arc/warc file)
 
Since the images have a hash we only collect the same image once.
Now return a list of ArcFileEntries (for the images) to the frontend.
The front-end renders all the images by getting the image from the servlet with arcfile and offset as
queryparams(and scale parameters). The servlet reads the image binaries from the arc/warc and scale the image and return it.

=== Visualized: <br>
So from the original resultset from solr, we generate a list of images for each document <br>
doc1 <br>
   image1 <br>
   image2 <br>
   ...<br>
doc2<br>
   image1<br>
   image2<br>
   ...<br>
doc3<br>
    image1  (if it is an image there is also only one)      <br>
...<br>
docn:<br>
    image1<br>
    ...<br>
<br>
The method now returns all the images meta data (ArcEntryDescriptor) that has file+offset<br>

The frontend can now load all the images through the mimetypeservlet method that loads the binary from
the arc/warc-files.
    
       
##Future improvements:
Perfomance: When load and parse the html to find images, this is just done sequential. It can be made multithread (Executor)
and load/parse several arc/warc files simultaneous. <-- This is is now implemented . 20 Threads is default

Ranking:Only extract the images near where the search-term is found. This will probably give more
relevant images when there are many images on a page.

When indexing a html-page we can at index time find the images and add these to a new multivalued field
in the index. This will improve performance by many factors since the single hardest operation is loading and parsing all the HTML-pages from the binaries. But it will make it difficult (impossible?) to improve ranking by finding images that are close to the matching terms in the document. 


    




