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
    // Build a link that triggers a 'find similar images' (PDQ hash) search for the image with this Solr document id.
    $_startPdqImageSearchFromImage(id) {
      return '/search?query=' + encodeURIComponent(id) + '&offset=0&grouping=' + this.searchStore.solrSettings.grouping + '&imgSearch=false&urlSearch=false&pdqSearch=true&facets='
    },
  }
}
