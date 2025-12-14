/**
 * SolrWayback Query History Tracker
 * 
 * This script tracks playback navigation for query history.
 * It monitors URL changes in the playback window and sends tracking
 * data to the backend API.
 */
(function() {
    var baseUrl = '__WAYBACK_BASEURL__';
    var lastUrl = location.href;
    
    /**
     * Track a playback URL by extracting wayback date and original URL
     * and sending them to the query history tracking endpoint.
     */
    function trackPlayback(url) {
        // Extract wayback date and original URL from the playback URL format:
        // /services/web/YYYYMMDDHHmmss/http://example.com
        var match = url.match(/\/services\/web\/(\d{14})\/(.*?)(?:[?#]|$)/);
        if (!match) {
            return;
        }
        
        try {
            fetch(baseUrl + 'services/queryhistory/track/playback', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin',
                body: JSON.stringify({
                    url: url,
                    waybackDate: match[1],
                    originalUrl: match[2]
                })
            });
        } catch (e) {
            console.error('Query history tracking error:', e);
        }
    }
    
    // Track the initial page load
    trackPlayback(location.href);
    
    // Monitor URL changes every 500ms to catch navigation within playback
    setInterval(function() {
        var currentUrl = location.href;
        if (currentUrl !== lastUrl) {
            trackPlayback(currentUrl);
            lastUrl = currentUrl;
        }
    }, 500);
})();
