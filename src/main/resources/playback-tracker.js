/**
 * SolrWayback Query History Tracker
 *
 * This script tracks playback navigation for query history.
 * It monitors URL changes in the playback window and sends tracking
 * data to the backend API, together with what the browser knows about
 * how the page was reached (referrer, navigation type and link clicks on
 * the previous page), so the backend can tell search results, links,
 * toolbar navigation etc. apart.
 */
(function() {
    var baseUrl = '__WAYBACK_BASEURL__';
    var lastUrl = location.href;
    // Clicks on playback links are kept in localStorage, so the page they open can read them, also in a new tab
    var CLICKS_KEY = 'solrwaybackNavigationClicks';
    var CLICK_MAX_AGE_MS = 60000;
    // Per-tab ID kept in sessionStorage, shared with the search page (NavigationTrackingUtils.js)
    var TAB_ID_KEY = 'solrwaybackTabId';
    var tabId = getTabId();

    /**
     * The ID of this browser tab, so entries from different tabs can be told apart in the history.
     */
    function getTabId() {
        try {
            var id = sessionStorage.getItem(TAB_ID_KEY);
            try {
                // Tabs opened by script get a copy of the opener's sessionStorage, and with it the opener's ID
                if (id && window.opener && window.opener.sessionStorage.getItem(TAB_ID_KEY) === id) {
                    id = null;
                }
            } catch (e) {
                // Opener on another origin
            }
            if (!id) {
                id = Date.now().toString(36) + Math.random().toString(36).substring(2, 10);
                sessionStorage.setItem(TAB_ID_KEY, id);
            }
            return id;
        } catch (e) {
            return null;
        }
    }

    function withoutFragment(url) {
        var hashIndex = url.indexOf('#');
        return hashIndex === -1 ? url : url.substring(0, hashIndex);
    }

    /**
     * The original URL part of a playback URL, without the capture date as links are often
     * redirected to the nearest capture. Returns null if the URL is not a playback URL.
     */
    function playbackTarget(url) {
        var match = withoutFragment(url).match(/\/services\/web\/\d{14}\/(.*)$/);
        if (!match) {
            return null;
        }
        try {
            return decodeURI(match[1]);
        } catch (e) {
            return match[1];
        }
    }

    function readClicks() {
        try {
            var now = Date.now();
            var clicks = JSON.parse(localStorage.getItem(CLICKS_KEY)) || [];
            return clicks.filter(function(click) {
                return now - click.time < CLICK_MAX_AGE_MS;
            });
        } catch (e) {
            return [];
        }
    }

    function writeClicks(clicks) {
        try {
            localStorage.setItem(CLICKS_KEY, JSON.stringify(clicks));
        } catch (e) {
            // Storage unavailable, the backend then classifies from the referrer alone
        }
    }

    /**
     * Remember a click on a link to a playback page, so the page it opens knows it was opened by a click.
     */
    function rememberClick(event) {
        var link = event.target && event.target.closest ? event.target.closest('a[href], area[href]') : null;
        if (!link) {
            return;
        }
        var target = playbackTarget(link.href);
        if (target === null) {
            return;
        }
        var clicks = readClicks();
        clicks.push({
            source: link.closest('#tegModal') ? 'toolbar' : 'page link',
            from: withoutFragment(location.href),
            target: target,
            time: Date.now()
        });
        writeClicks(clicks.slice(-10));
    }

    /**
     * Find and remove the click on the referring page that opened this page.
     * Returns the source of the click ('toolbar' or 'page link') or null if there was none.
     */
    function takeClickSource(url) {
        var referrer = withoutFragment(document.referrer);
        var target = playbackTarget(url);
        var clicks = readClicks();
        for (var i = clicks.length - 1; i >= 0; i--) {
            if (clicks[i].from === referrer && clicks[i].target === target) {
                var source = clicks[i].source;
                clicks.splice(i, 1);
                writeClicks(clicks);
                return source;
            }
        }
        return null;
    }

    /**
     * The browser's navigation type for this page: 'navigate', 'reload' or 'back_forward'.
     */
    function navigationType() {
        try {
            var entries = performance.getEntriesByType('navigation');
            if (entries.length > 0) {
                return entries[0].type;
            }
        } catch (e) {
            // Navigation timing not supported
        }
        return null;
    }

    /**
     * Track a playback URL by extracting wayback date and original URL
     * and sending them to the query history tracking endpoint.
     */
    function trackPlayback(url, navigation) {
        // Extract wayback date and original URL from the playback URL format:
        // /services/web/YYYYMMDDHHmmss/http://example.com
        var match = url.match(/\/services\/web\/(\d{14})\/(.*?)(?:[?#]|$)/);
        if (!match) {
            return;
        }

        var data = {
            url: url,
            waybackDate: match[1],
            originalUrl: match[2],
            tabId: tabId
        };
        for (var key in navigation) {
            data[key] = navigation[key];
        }

        try {
            fetch(baseUrl + 'services/navigationhistory/track/playback', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin',
                body: JSON.stringify(data)
            });
        } catch (e) {
            console.error('Query history tracking error:', e);
        }
    }

    // Track the initial page load
    trackPlayback(location.href, {
        referrer: document.referrer,
        navigationType: navigationType(),
        clickSource: takeClickSource(location.href)
    });

    document.addEventListener('click', rememberClick, true);
    document.addEventListener('auxclick', rememberClick, true); // middle click

    // Pages restored from the back/forward cache do not run this script again
    window.addEventListener('pageshow', function(event) {
        if (event.persisted) {
            trackPlayback(location.href, { navigationType: 'back_forward' });
        }
    });

    // Monitor URL changes every 500ms to catch navigation within playback
    setInterval(function() {
        var currentUrl = location.href;
        if (currentUrl !== lastUrl) {
            trackPlayback(currentUrl, { sameDocument: true, referrer: lastUrl });
            lastUrl = currentUrl;
        }
    }, 500);
})();
