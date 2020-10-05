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
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
    }),
    $_startPageSearchFromImage(searchItem) {
      return '/?query=' + 'links_images:"' + encodeURIComponent(searchItem) + '"' + '&offset=0&grouping=' + this.solrSettings.grouping + '&imgSearch=false&urlSearch=false&facets='
    },
    $_startImageSearchFromImage(searchItem) {
      return '/?query=' + 'hash:"' + encodeURIComponent(searchItem) + '"' + '&offset=0&grouping=' + this.solrSettings.grouping + '&imgSearch=false&urlSearch=false&facets='
    },
  }
}
