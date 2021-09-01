self.addEventListener('fetch', function(event) {   
destination_url = event.request.url; //destination url
referer_url=event.request.referrer; //where we can from. Use this to find the timestamp.
// console.log('SolrWayback serviceworker referer:'+referer_url); 

referer_solrwayback_url_index = referer_url.indexOf('/solrwayback/');


//This is the domain only without /solrwayback. Example https://kb.dk:4000/
solrwayback_server=self.location.origin;
//console.log('Solrwayback url:'+solrwayback_url);

//Where sw.js was loaded from and the solrwayback base url: example https://kb.dk:4000/solrwayback
solrwayback_url=self.location.origin +'/solrwayback'; 


//Use this fast 404 if referer can not be found in live leak. (rare)
NOT_FOUND = solrwayback_url+'/services/notfound';

//console.log('SolrWayback serviceworker got url:'+destination_url);
destinationUrl = new URL(destination_url);
 
//allow open streep map due to GUI location search
if (destinationUrl.host.indexOf('tile.openstreetmap.org') > 1 ){
	 console.log('SolrWayback serviceworker allowing live leak to Open Streetmap:'+ destinationUrl.host);
}
else if(!destination_url.startsWith(solrwayback_server) && referer_solrwayback_url_index == -1  ){ //Should not happen, but make sure live leaks are blocked
	 //You can block if referer (crawltime) is unknown
     //console.log('SolrWayback serviceworker blocking request. Referer does not contain solrwayback url part:'+referer_url +" for destination url:"+destination_url);	 	 	 
	 //event.respondWith(fetch(NOT_FOUND));
     //or redirect to latests.
	 crawltime_hardcoded =getYearCrawlDate();
	 newUrl = solrwayback_url+'/services/web/'+crawltime_hardcoded+'/'+destination_url;                 	 
     console.log('SolrWayback serviceworker forwarding live leak url to (crawltime is latest):'+newUrl);	 
     event.respondWith(fetch(newUrl));          
	
}
else if (destination_url.startsWith(solrwayback_server) && !destination_url.startsWith(solrwayback_url)){
    //This is a live relative leak back to the server, but missing the /solrwayback context.
     //console.log('SolrWayback Serviceworker found root leak:'+destination_url);		
	 //console.log('SolrWayback Serviceworker found root leak referer:'+referer_url);
     newUrl = solrwayback_url+'/services/webProxyLeak/'+destination_url;  //Can be both url or warc+offset. Handle this in java    	 
     console.log("SolrWayback Serviceworker redirecting relative leak:"+destination_url +" to "+newUrl);
     //The serviceworker will change the original referer to the serviceworker instead(Semi bug in fact!).  
     event.respondWith(fetch(newUrl, {    
    	  headers: {
    	    'serviceworker_referer': referer_url,
    	  },    	 
    	}))     
}
else{

   //If destination url is another domain, forward it back into solrwayback
	// The url: http://example.com/test.jpg is a live leak and will be rewritten to
	// https://yourdomain.com/solrwayback/services/web/<crawldate>/http://example.com/test.jpg
	// Where crawldate is taken from the referer url.	 
 if (destination_url.startsWith('http') && !destination_url.startsWith(solrwayback_server)){   	 					 
	 web_start = referer_url.indexOf('/services/web/'); 	 	 
	 if(web_start== -1 ){	 
		 //Block
		 //console.log('SolrWayback serviceworker blocked leak. Could not find crawltime in referer url:'+referer_url +' for destination:'+destination_url);		 		 
	   	 //event.respondWith(fetch(NOT_FOUND));
		 //Use hardcoded crawltime
		 crawltime_hardcoded =getYearCrawlDate();
		 newUrl = solrwayback_url+'/services/web/'+crawltime_hardcoded+'/'+destination_url;                 	 
	     console.log('SolrWayback serviceworker forwarding live leak url to (crawltime is latest):'+newUrl);	 
	     event.respondWith(fetch(newUrl));
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


function getYearCrawlDate() {
var date = new Date();
var year = date.getFullYear();
return year+"0101000000";
}

