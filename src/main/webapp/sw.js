self.addEventListener('fetch', function(event) {   
destination_url = event.request.url; //destination url
referer_url=event.request.referrer; //where we can from. Use this to find the timestamp.
// console.log('SolrWayback serviceworker referer:'+referer_url); 

referer_solrwayback_url_index = referer_url.indexOf('/solrwayback/');

//Where sw.js was loaded from and the solrwayback base url: example https://kb.dk:4000/solrwayback
solrwayback_url=self.location.origin +'/solrwayback'; 
solrwayback_server=self.location.origin;
//console.log('Solrwayback url:'+solrwayback_url);

//Use this fast 404 if referer can not be found in live leak. (rare)
NOT_FOUND = solrwayback_url+'/solrwayback/services/notfound';
 
destinationUrl = new URL(destination_url);
 
if (destinationUrl.host.indexOf('tile.openstreetmap.org') > 1 ){ //allow open streep map due to GUI location search
	 console.log('SolrWayback serviceworker allowing live leak to Open Streetmap:'+ destinationUrl.host);
}
else if(!destination_url.startsWith(solrwayback_server) && referer_solrwayback_url_index == -1  ){ //Should not happen, but make sure live leaks are blocked
	 console.log('SolrWayback serviceworker blocking request. Referer does not contain solrwayback url part:'+referer_url +" for destination url:"+destination_url);	 
	 event.respondWith(fetch(NOT_FOUND));		 		
}
else{

   //If destination url is another domain, forward it back into solrwayback
	// The url: http://example.com/test.jpg is a live leak and will be rewritten to
	// https://yourdomain.com/solrwayback/services/web/<crawldate>/http://example.com/test.jpg
	// Where crawldate is taken from the referer url.	 
 if (destination_url.startsWith('http') && !destination_url.startsWith(solrwayback_server)){   	 					 
	 web_start = referer_url.indexOf('/services/web/'); 	 	 
	 if(web_start== -1 ){	 
		 console.log('SolrWayback serviceworker blocked leak. Could not find crawltime in referer url:'+referer_url +' for destination:'+destination_url);		 		 
		 event.respondWith(fetch(NOT_FOUND));		 
	 }
	 else{ //forward the live leak back into solrwayback
	 crawltime= referer_url.substring(web_start+14, web_start+28);
     newUrl = solrwayback_url+'/services/web/'+crawltime+'/'+destination_url;                 	 
	 console.log('SolrWayback serviceworker forwarding live leak url to:'+newUrl);	 
     event.respondWith(fetch(newUrl));                             
     }
   } 

 else{
		//console.log('SolrWayback serviceworker. No rewrite for url:'+destination_url);
	}
   }
});

