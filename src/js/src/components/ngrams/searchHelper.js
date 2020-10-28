export default {
  methods: {
    $_handleSearch(chartInstance, evt) {
      const activeElement = chartInstance.getElementAtEvent(evt)
      if (activeElement.length > 0) {
        const yearFromClick = activeElement[0]._xScale.ticks[activeElement[0]._index]
        const queryFromClick = chartInstance.config.data.datasets[activeElement[0]._datasetIndex].label
        const url = `/search?query=${encodeURIComponent(queryFromClick)}&grouping=false&imgSearch=false&offset=0&urlSearch=false&facets=%26fq%3Dcrawl_year%3A"${yearFromClick}"` 
        window.open(url, '_blank')
        }
    },
  }
}