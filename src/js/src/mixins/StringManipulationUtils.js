export default {
  methods: {

    $_displayFacetName(facet) {
      return facet.replace('&fq=','').split(':')[0] + ': '
    },
    $_displayFacetValue(facet) {
      let s = facet.split(':')
      return s.slice(1, s.length).join(' ').replace(/"/g,'')
    },
    $_checkDomain(domain) {
      // Matches at least 1 dot in the string, and no spaces. 
      return domain.match(/^[^\s]+\.[^\s]+$/)
    },
    $_checkDate(date) {
      // Matches format YYYY-MM-DD
      return date.match(/^([12]\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01]))$/)
    },
    $_displayDate(date, timeScale){
      // Display only the scale
      let end = 0
      if (date.length > 0) {
      switch (timeScale) {
        case 'YEAR':
        case 'null':
          end = 4
          break
        case 'MONTH':
          end = 7
          break
        case 'WEEK':
        case 'DAY':
        default:
          end = 10
      }}
      return date.slice(0, end)
    }
  }
}
