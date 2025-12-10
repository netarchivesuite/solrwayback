/**
 * Query History Tracker Utility
 * 
 * Manages tracking of search queries and playback URLs in session storage
 * for the current browser session only.
 */

const HISTORY_KEY = 'solrwayback_query_history'

/**
 * Add a URL entry to the query history
 * @param {string} url - The URL to track
 */
export function addToQueryHistory(url) {
  try {
    if (typeof sessionStorage === 'undefined' || typeof window === 'undefined') {
      return
    }
    
    const history = getQueryHistory()
    
    // Add new entry with timestamp
    const entry = {
      url: url,
      timestamp: new Date().toISOString()
    }
    
    history.push(entry)
    
    // Save back to session storage
    sessionStorage.setItem(HISTORY_KEY, JSON.stringify(history))
    
    // Dispatch custom event to notify listeners
    window.dispatchEvent(new CustomEvent('queryHistoryUpdated', { 
      detail: { count: history.length } 
    }))
  } catch (error) {
    console.error('An error occurred when adding latest action to interaction history:', error)
  }
}

/**
 * Get the current query history from session storage
 * @returns {Array} Array of history entries
 */
export function getQueryHistory() {
  try {
    if (typeof sessionStorage === 'undefined') {
      return []
    }
    const historyJson = sessionStorage.getItem(HISTORY_KEY)
    if (!historyJson) {
      return []
    }
    return JSON.parse(historyJson)
  } catch (error) {
    console.error('The following error occurred when getting interaction history from current session:', error)
    return []
  }
}

/**
 * Clear the query history from session storage
 */
export function clearQueryHistory() {
  try {
    sessionStorage.removeItem(HISTORY_KEY)
    window.dispatchEvent(new CustomEvent('queryHistoryUpdated', { 
      detail: { count: 0 } 
    }))
  } catch (error) {
    console.error('The following error occurred when clearing interaction history from current session:', error)
  }
}

/**
 * Get the count of history entries
 * @returns {number} Number of entries in history
 */
export function getQueryHistoryCount() {
  return getQueryHistory().length
}

/**
 * Track a search URL
 * @param {string} query - Search query
 * @param {Array} appliedFacets - Applied facets
 * @param {Object} solrSettings - Solr settings
 * @param {string} baseUrl - Base URL for SolrWayback instance
 */
export function trackSearch(query, appliedFacets, solrSettings, baseUrl = '') {
  // Construct the search URL similar to how the router does it
  const params = new URLSearchParams()
  params.set('query', query)
  params.set('grouping', solrSettings.grouping)
  params.set('imgSearch', solrSettings.imgSearch)
  params.set('urlSearch', solrSettings.urlSearch)
  params.set('offset', solrSettings.offset || 0)
  
  if (appliedFacets && appliedFacets.length > 0) {
    params.set('facets', appliedFacets.join(''))
  }
  
  if (solrSettings.sort) {
    params.set('sort', solrSettings.sort)
  }
  
  const url = `${baseUrl}/solrwayback/search?${params.toString()}`
  addToQueryHistory(url)
}

/**
 * Track a playback URL (when user clicks on archived content)
 * @param {string} playbackUrl - The full playback URL
 */
export function trackPlayback(playbackUrl) {
  addToQueryHistory(playbackUrl)
}

/**
 * Parse URL parameters from a SolrWayback search URL
 * @param {string} url - The search URL to parse
 * @returns {object} Object containing query, facets, and filter queries
 */
function parseSolrWaybackParams(url) {
  try {
    const decoded = decodeURIComponent(url.trim())
    const urlObj = new URL(decoded)
    const params = new URLSearchParams(urlObj.search)
    
    const query = params.get('query')
    const facets = params.get('facets')
    const fqs = params.getAll('fq')
    
    return { query, facets, fqs }
  } catch (error) {
    console.error('Error parsing URL:', error)
    return { query: null, facets: null, fqs: [] }
  }
}

/**
 * Parse archived URL to extract date and original URL
 * @param {string} decodedUrl - The playback URL
 * @returns {object|null} Object containing date and url, or null if invalid
 */
function parseArchivedUrl(decodedUrl) {
  const prefix = '/solrwayback/services/web/'
  
  if (!decodedUrl.includes(prefix)) {
    return null
  }
  
  const idx = decodedUrl.indexOf(prefix)
  const remainder = decodedUrl.substring(idx + prefix.length)
  const parts = remainder.split('/', 2)
  
  if (parts.length !== 2) {
    return null
  }
  
  return {
    date: parts[0],
    url: parts[1]
  }
}

/**
 * Format a search entry for output
 * @param {object} params - Parsed parameters
 * @param {number} counter - Entry counter
 * @returns {string} Formatted string
 */
function formatSearchEntry(params, counter) {
  let output = `Action Number: ${counter}\n`
  output += `SolrWayback Query changed.\n`
  output += `Query: ${params.query || 'N/A'}\n`
  
  if (params.facets) {
    output += `Facets: ${params.facets}\n`
  }
  
  if (params.fqs && params.fqs.length > 0) {
    params.fqs.forEach((fq, idx) => {
      output += `Filter Query ${idx + 1}: ${fq}\n`
    })
  }
  
  output += '-----------------------\n'
  return output
}

/**
 * Format a playback entry for output
 * @param {object} archived - Parsed archived URL info
 * @param {number} counter - Entry counter
 * @param {boolean} lastWasClick - Whether previous action was a click
 * @returns {string} Formatted string
 */
function formatPlaybackEntry(archived, counter, lastWasClick) {
  let output = `Action Number: ${counter}\n`
  
  if (lastWasClick) {
    output += 'Clicked on a link from a playback page.\n'
  } else {
    output += 'Found interesting search result and clicked it from search results:\n'
  }
  
  output += 'SolrWayback Playback URL clicked.\n'
  output += `Archive Date: ${archived.date}\n`
  output += `Original URL: ${archived.url}\n`
  output += '-----------------------\n'
  
  return output
}

/**
 * Parse all history entries and generate formatted output
 * @returns {string} Formatted history text
 */
export function parseHistory() {
  const history = getQueryHistory()
  
  if (history.length === 0) {
    return 'No search history available for this session.\n'
  }
  
  let output = 'SolrWayback Query History\n'
  output += '=========================\n\n'
  
  let counter = 0
  let lastWasClick = false
  
  history.forEach(entry => {
    counter++
    const decoded = decodeURIComponent(entry.url)
    
    // Check if it's a playback URL
    if (decoded.includes('/solrwayback/services/web/')) {
      const archived = parseArchivedUrl(decoded)
      if (archived) {
        output += formatPlaybackEntry(archived, counter, lastWasClick)
        lastWasClick = true
        return
      }
    }
    
    // Check if it's a search URL
    if (decoded.includes('/solrwayback/search?query=')) {
      const params = parseSolrWaybackParams(entry.url)
      output += formatSearchEntry(params, counter)
      lastWasClick = false
    }
  })
  
  return output
}

/**
 * Generate and download history as a text file
 */
export function downloadHistory() {
  const historyText = parseHistory()
  const blob = new Blob([historyText], { type: 'text/plain;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-')
  
  link.href = url
  link.download = `solrwayback-query-history-${timestamp}.txt`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
