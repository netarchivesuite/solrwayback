self.addEventListener('fetch', function(event) {   
 url = event.request.url;
 console.log('Serviceworker got url:'+url);
 console.log('Serviceworker referer:'+event.request.referrer); 
 console.log('OK'); 
 if (url.startsWith('http') && !url.startsWith('https://solrwb-test.kb.dk:4000/')){   
	 console.log('Found leak url:'+url);		
	newUrl = 'https://solrwb-test.kb.dk:4000/solrwayback/web/20210121153119/'+url;                 
	console.log('forwarding live leak url to:'+newUrl);	
    event.respondWith(
      fetch(newUrl));                             
  }
 else{	 
	 console.log('not forwarding url');
 }
});