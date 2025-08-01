import HistoryRoutingUtils from './HistoryRoutingUtils'
import { mapStores, mapActions } from 'pinia'
import { useSearchStore } from '../store/search.store'

export default {
  mixins: [HistoryRoutingUtils],
  computed: {
    // ...mapState({
    //   searchAppliedFacets: state => state.Search.searchAppliedFacets,
    //   solrSettings: state => state.Search.solrSettings,
    // }),
    ...mapStores(useSearchStore)
  },
  methods: {
    ...mapActions(useSearchStore, {
      updateSolrSettingImgSearch:'updateSolrSettingImgSearch',
    }),
    $_startPageSearchFromImage(searchItem) {
      return '/search?query=' + 'links_images:"' + encodeURIComponent(searchItem) + '"' + '&offset=0&grouping=' + this.searchStore.solrSettings.grouping + '&imgSearch=false&urlSearch=false&facets='
    },
    $_startImageSearchFromImage(searchItem) {
      return '/search?query=' + 'hash:"' + encodeURIComponent(searchItem) + '"' + '&offset=0&grouping=' + this.searchStore.solrSettings.grouping + '&imgSearch=false&urlSearch=false&facets='
    },
  }
}
