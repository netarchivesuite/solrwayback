export default {
  computed: {
  },
  methods: {
    seperateFacets(facets) {
      let dividedFacets = facets.split('&fq=').filter(Boolean)
      return dividedFacets
    },
    displayFacetName(facet) {
      return facet.split(":")[0] + ": "
    },
    displayFacetValue(facet) {
      return facet.split(":")[1].replace(/"/g,'')
    },
  }
}
