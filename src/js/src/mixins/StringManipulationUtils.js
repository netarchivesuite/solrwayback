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
    $_checkDate(date) {
      // Matches format YYYY-MM-DD
      return date.match(/^([12]\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01]))$/)
    },
  }
}
