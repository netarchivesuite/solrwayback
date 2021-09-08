/*
 * The serviceworker will intercept all traffic and redirect live leaks back into SolrWayback.
 * 
 * There are two types of leaks:
 * 1) Live leak to another domain than the solrwayback server. Live leaks to openstreepmap is allowed because it is used in location search from the GUI.
 * 2) Live leak to the tomcat context root. It is missing the /solrwayback/ context root
 * 
 * Whenever a leak is found it will be directed back into SolrWayback with correct crawltime if possible.
 * The java layer handles more complex logic with referer. It can be a warc-file+offset etc.
 * 
 * Serviceworkers only works in modern browsers, see: https://caniuse.com/serviceworkers
 *  
 * When using legacy browser the SolrWaybackRootProxy will still be required to fix root leaks.
 * Live leaks to other domains will not be blocked in legacy browsers.
 *  
 */
self.addEventListener('fetch', function(event) {   
  destination_url = event.request.url; //destination url
  referer_url=event.request.referrer; //where we can from. Use this to find the timestamp.
  //console.log('SolrWayback serviceworker referer:'+referer_url); 

  referer_solrwayback_url_index = referer_url.indexOf('/solrwayback/');

  //This is the domain only without /solrwayback. Example https://kb.dk:4000/
  solrwayback_server=self.location.origin;
  //console.log('Solrwayback url:'+solrwayback_url);

  //Where sw.js was loaded from and the solrwayback base url: example https://kb.dk:4000/solrwayback
  solrwayback_url=self.location.origin +'/solrwayback'; 
 
  //console.log('SolrWayback serviceworker got url:'+destination_url);
  destinationUrl = new URL(destination_url);
 
  //allow open streep map due to GUI location search
  if (destinationUrl.host.indexOf('tile.openstreetmap.org') > 1 ){
	 console.log('SolrWayback Serviceworker allowing live leak to Open Streetmap:'+ destinationUrl.host);
  }
  // Links are correct going to the solrwayback server. But can be missing contextroot '/solrwayback'
  else if (destination_url.startsWith(solrwayback_server)){	
	//Leak to tomcat root servlet. Direct it to /solrwayback context root
	if( !destination_url.startsWith(solrwayback_url)){
	  console.log('SolrWayback Serviceworker found relative leak to:'+destination_url);
	  newUrl = solrwayback_url+'/services/webProxyLeak/'+destination_url;  //Can be both url or warc+offset. Handle this in java
	  console.log('Forwarding leak to:'+ newUrl);	    
	  event.respondWith(fetch(newUrl, {    
	     headers: {
	     'serviceworker_referer': referer_url,
	    },    	 
	  }))     
	}
	else{ //The normal situation.
  	  // console.log('SolrWayback Serviceworker. No rewrite for url:'+destination_url); 
	}	
  }
  //Link is to another domain (live leak). Redirect into solrwayback.
  else{			
	//Missing referer. Hardcode this year as crawltime. (Can be discussed if this is the best solution). These leaks seems to be trackers/adds or fonts. So rarely relevant which crawltime
	if (referer_solrwayback_url_index == -1){	
		 crawltime_hardcoded =getYearCrawlDate();
		 newUrl = solrwayback_url+'/services/web/'+crawltime_hardcoded+'/'+destination_url;                 	 
	     console.log('SolrWayback Serviceworker forwarding live leak url to (crawltime is latest):'+newUrl);	 
	     event.respondWith(fetch(newUrl));          
	}
	else{ //Sometimes referer is just the domain and not the full url. Hardcode crawltime
		 web_start = referer_url.indexOf('/services/web/'); 	
         if(web_start== -1 ){	 			
             crawltime_hardcoded =getYearCrawlDate();
             newUrl = solrwayback_url+'/services/web/'+crawltime_hardcoded+'/'+destination_url;                 	 
             console.log('SolrWayback Serviceworker forwarding live leak url to (crawltime is latest):'+newUrl);	 
             event.respondWith(fetch(newUrl));
		 }
		 else{ //Most common case: Forward the live leak back into solrwayback. This live leak has been patched perfectly.
	       crawltime= referer_url.substring(web_start+14, web_start+28);
	       newUrl = solrwayback_url+'/services/web/'+crawltime+'/'+destination_url;                 	 
           console.log('SolrWayback Serviceworker forwarding live leak url to:'+newUrl);	 
	       event.respondWith(fetch(newUrl));                             
	     }		 
	 }	
  }
}); //End event listener

function getYearCrawlDate() {
  var date = new Date();
  var year = date.getFullYear();
  return year+"0101000000";
}

