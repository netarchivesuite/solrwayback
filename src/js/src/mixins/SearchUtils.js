import HistoryRoutingUtils from './HistoryRoutingUtils'
import { mapStores, mapActions } from 'pinia'
import { useSearchStore } from '../store/search.store'
import { useNotifierStore } from '../store/notifier.store'
import { requestService } from '../services/RequestService'

export default {
  mixins: [HistoryRoutingUtils],
  computed: {
    // ...mapState({
    //   query: state => state.Search.query,
    //   searchAppliedFacets: state => state.Search.searchAppliedFacets,
    //   solrSettings: state => state.Search.solrSettings,
    // }),
    ...mapStores(useSearchStore)
  },
  methods: {
    ...mapActions(useSearchStore, {
      updateQuery:'updateQuery',
      updateSolrSettingGrouping:'updateSolrSettingGrouping',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
      updateSolrSettingUrlSearch:'updateSolrSettingUrlSearch',
      updateSolrSettingOffset:'updateSolrSettingOffset',
      updatePreNormalizedQuery:'updatePreNormalizedQuery',
      updateNormalizedQuery:'updateNormalizedQuery',
      clearResults:'clearResults',
      clearFacets:'clearFacets',
      requestSearch:'requestSearch',
      requestImageSearch:'requestImageSearch',
      requestUrlSearch:'requestUrlSearch',
      requestNormalizedFacets:'requestNormalizedFacets',
      requestFacets:'requestFacets'

    }),
    ...mapActions(useNotifierStore, {
      setNotification: 'setNotification'
    }),
    $_validateUrlSearchPrefix(testString) {
      return testString.substring(0,7) === 'http://' || 
             testString.substring(0,8) === 'https://' || 
             testString.substring(0,10) === 'url_norm:"'
    },
    //Deliver a normal search
    deliverSearchRequest(futureQuery, updateHistory, pagnation) {
      this.requestSearch({query:futureQuery, facets:this.searchStore.searchAppliedFacets, options:this.searchStore.solrSettings})
      !pagnation ? this.requestFacets({query:futureQuery, facets:this.searchStore.searchAppliedFacets, options:this.searchStore.solrSettings}) : null
      updateHistory ? this.$_pushSearchHistory('Search', futureQuery, this.searchStore.searchAppliedFacets, this.searchStore.solrSettings) : null
    },
    //Deliver an URL search
     async deliverUrlSearchRequest(futureQuery, updateHistory) {
      this.updatePreNormalizedQuery(futureQuery)
      if(this.$_validateUrlSearchPrefix(this.disectQueryForNewUrlSearch(futureQuery))) {
        let normalizedURL = await requestService.getNormalizedURL(this.disectQueryForNewUrlSearch(futureQuery))
        this.updateNormalizedQuery(normalizedURL)
        this.requestUrlSearch({query:normalizedURL, facets:this.searchStore.searchAppliedFacets, options:this.searchStore.solrSettings, preNormalizedQuery:this.disectQueryForNewUrlSearch(futureQuery)})
        this.requestNormalizedFacets({query:normalizedURL, facets:this.searchStore.searchAppliedFacets, options:this.searchStore.solrSettings, preNormalizedQuery:this.disectQueryForNewUrlSearch(futureQuery)})
        updateHistory ? this.$_pushSearchHistory('Search', normalizedURL, this.searchStore.searchAppliedFacets, this.searchStore.solrSettings) : null
      }
      else {
        this.setNotification({
          title: 'We are so sorry!',
          text: 'This URL is not valid. the url must start with \'http://\' or \'https://\'',
          type: 'error',
          timeout: false
        })
      }
    },
    // Deliver an image search
    deliverImgSearchRequest(futureQuery, updateHistory) {
      this.requestImageSearch({query:futureQuery})
      updateHistory ? this.$_pushSearchHistory('Search', futureQuery, this.searchStore.searchAppliedFacets, this.searchStore.solrSettings) : null
    },
    // Check if there has been any changes to the query
    queryHasChanged(query) {
      return query !== this.searchStore.query
    },
    // Prepare for a new search
    prepareStateForNewSearch(futureQuery, pagnation) {
      this.updatePreNormalizedQuery(null)
      this.updateNormalizedQuery(null)
      this.clearResults()
      !pagnation ? this.clearFacets() : null
      this.updateQuery(futureQuery)
    },
    // Disect the query for URL searching
    disectQueryForNewUrlSearch(futureQuery) {
      let queryString = ''
          if(futureQuery.substring(0,10) === 'url_norm:"') {
            queryString = futureQuery.replace('url_norm:"', '')
            queryString.substring(queryString.length-1, queryString.length) === '"' ? queryString = queryString.slice(0,-1) : null
          }
          else {
            queryString = futureQuery
          }
          return queryString
    },
    // Method to fire off a search (and deciding which kind it is)
    $_determineNewSearch(futureQuery, updateHistory, pagnation) {
      //console.log('we\'ve accessed the searchfunction with ',futureQuery)
      //console.log('we have these solrsettings: ', this.solrSettings)
      //console.log('and these facets', this.searchAppliedFacets)
      this.prepareStateForNewSearch(futureQuery, pagnation)
      if(this.searchStore.solrSettings.imgSearch) {
        this.deliverImgSearchRequest(futureQuery ,updateHistory)
      }
      else if(this.searchStore.solrSettings.urlSearch) {
        this.deliverUrlSearchRequest(futureQuery , updateHistory)
      }
      else {
        this.deliverSearchRequest(futureQuery, updateHistory, pagnation)
      }
    },

    /*
    * Regex query checker
    * Kudos to tokee
    * https://github.com/netarchivesuite/solrwayback/issues/41#issuecomment-724571414
    * https://jsfiddle.net/bsc6zkxy/12/   
    **/  
    $_checkQueryForBadSyntax(q) {
        q = q.replaceAll('\n', ' ')
        let responses = []
        if (((q.includes(' AND ')  || q.includes(' && ')) &&
            (q.includes(' OR ') || q.includes(' || '))) && 
            !q.includes('(')) {
          responses.push('Ambiguous AND/OR. Consider adding clarifying parentheses')
        }
        
        if (q.match(/ (and|or|not) /)) {
          responses.push('Possible faulty boolean. Booleans must be uppercase - ' +  q.replace(' and ', ' AND ').replace(' or ', ' OR ').replace(' not ', ' NOT '))
        }

        if (q.match(/[`‘’''„“‟”❝❞]/)) {
          responses.push('Smart quotes. When quoting, use simple quote signs " - ' +  q.replace(/[`‘’'„“‟”❝❞]/ + '"'))
        }

        // Remove all 'f:[something TO somethingelse] to avoid colon warnings for crawl_date:[2023-12-99T12:34:56Z TO *]
        let qfold = q.replace(/[^\\]: *\[ *[^ ]+ * TO  *[^ ]+ *]/g, '')
        if (qfold.match(/[^:"]+:[^:" ]*[^\\]:[^" ]( ?.*)$/)) {
          responses.push('Two colons without quote signs. When a qualified search is performed, consider quoting the value - ' + q.replace(/([^:"]*:)([^:" ]*[^\\]:[^" ]*)( ?.*)$/, '$1"$2"$3'))
        }

        if (q.match(/^https?:\/\/[^ ]*$/)) {
          responses.push('Standalone URL. Consider using URL-search for URLs')
        }

        if (q.match(/(^|\s)[*]($|\s)/)) {
          responses.push('Slow single star searches. These searches are very slow, use *:* to match everything try ' + q.replace(/(^|\s)([*])($|\s)/, '$1*:*$3'))
        }

        //Old regex discontinued due to no support for 'look behind' in Safari and IE11
        //let quoteMatches = q.replace('\\\\', '').match(/(?<!\\)"/g)
        let quoteMatches = q.replace('\\\\', '').replace('\\"', '').match(/"/g)
        if (quoteMatches && quoteMatches.length % 2 != 0) {
          responses.push('Unbalanced quotes. Make sure to write both a start- and an end-quote "')
        }
        
        let parenBalance = 0
        // Remove escapes, then iterate the tokens quotes_string, left_paren and right_paren
        q.replace(/\\\\/g, '').replace(/\\["()]/g, '').replace(/("[^"]*")|\(|\)/g, function(value, index, array) {
          // We only care about the side effect (adjusting paren balance), so there's no return value
          parenBalance += ('(' == value ? 1 : (')' == value ? -1 : 0))
        })
        
        if (parenBalance < 0) {
          responses.push(-parenBalance + ' missing start parenthes' + (parenBalance == -1 ? 'is' : 'es') + '. Make sure to balance parentheses')
        } else if (parenBalance > 0) {
          responses.push(parenBalance + ' missing end parenthes' + (parenBalance == 1 ? 'is' : 'es') + '. Make sure to balance parentheses')
        }
        
        return responses
     }
  }
}