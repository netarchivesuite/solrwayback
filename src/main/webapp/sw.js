self.addEventListener('fetch', function(event) {   
 url = event.request.url;
 console.log('Serviceworker got url:'+url);
 console.log('1');
 if (url.startsWith('http') && !url.startsWith('https://solrwb-test.kb.dk:4000/')){   
	 console.log('2');
	 console.log('Found leak url:'+url);		
	newUrl = 'https://solrwb-test.kb.dk:4000/solrwayback/web/20210121153119/'+url;                 
	console.log('forwarding live leak url to:'+newUrl);	
    event.respondWith(
      fetch(newUrl));                             
  }
 else{
	 console.log('3');
	 console.log('not forwarding url');
 }
});