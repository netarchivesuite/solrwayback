export default {
  methods: {

    displayFacetName(facet) {
      return facet.replace('&fq=','').split(':')[0] + ': '
    },
    displayFacetValue(facet) {
      return facet.split(':')[1].replace(/"/g,'')
    },
    checkDomain(domain) {
      // Matches at least 1 dot in the string, and no spaces. 
      return domain.match(/^[^\s]+\.[^\s]+$/)
    },
  }
}
