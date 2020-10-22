export default {
  methods: {

    $_displayFacetName(facet) {
      return facet.replace('&fq=','').split(':')[0] + ': '
    },
    $_displayFacetValue(facet) {
      return facet.split(':')[1].replace(/"/g,'')
    },
    $_checkDomain(domain) {
      // Matches at least 1 dot in the string, and no spaces. 
      return domain.match(/^[^\s]+\.[^\s]+$/)
    },
  }
}
