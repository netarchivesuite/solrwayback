import HistoryRoutingUtils from './HistoryRoutingUtils'
import { mapState, mapActions } from 'vuex'

export default {
  mixins: [HistoryRoutingUtils],
  computed: {
    ...mapState({
      searchAppliedFacets: state => state.Search.searchAppliedFacets,
      solrSettings: state => state.Search.solrSettings,
    }),
  },
  methods: {
    ...mapActions('Search', {
      updateSearchAppliedFacets:'updateSearchAppliedFacets',
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
    }),
    $_startPageSearchFromImage(searchItem) {
      return '/?q=' + 'links_images:"' + encodeURIComponent(searchItem) + '"' + '&offset=' + this.solrSettings.offset + '&grouping=' + this.solrSettings.grouping + '&imgSearch=false&urlSearch=false'
    },
    $_startImageSearchFromImage(searchItem) {
      return '/?q=' + 'hash:"' + encodeURIComponent(searchItem) + '"' + '&offset=' + this.solrSettings.offset + '&grouping=' + this.solrSettings.grouping + '&imgSearch=false&urlSearch=false'
    },
    $_addHistory(field, searchItem) {
      console.log('adding history with', field)
      let query
      if(field === 'hash') {
        query = 'hash:"' + encodeURIComponent(searchItem) + '"'
      } 
      else if(field === 'links_images') {
        query = 'links_images:"' + encodeURIComponent(searchItem) + '"'
      }  
      this.updateSearchAppliedFacets('')
      this.updateSolrSettingImgSearch(false)
      this.$_pushSearchHistory('SolrWayback', query, this.searchAppliedFacets, this.solrSettings)
    },
  }
}
