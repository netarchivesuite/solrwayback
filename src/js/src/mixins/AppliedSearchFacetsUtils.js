export default {
  methods: {

    displayFacetName(facet) {
      return facet.replace('&fq=','').split(':')[0] + ': '
    },
    displayFacetValue(facet) {
      return facet.split(':')[1].replace(/"/g,'')
    },
  }
}
