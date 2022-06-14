process.env.VUE_APP_VERSION = process.env.NODE_ENV === 'production' ? require('./package.json').version : 'DEVELOPMENT BUILD'
module.exports = {
  devServer: {
    proxy: {
    '^/services/frontend/solr/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/solr/',
        pathRewrite: { '^/services/frontend/solr/': '' },
        changeOrigin: true
      },
    '^/services/frontend/images/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/images/',
        pathRewrite: { '^/services/frontend/images/': '' },
        changeOrigin: true
      },
    '^/services/frontend/images/htmlpage/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/images/htmlpage/',
        pathRewrite: { '^/services/frontend/images/htmlpage/': '' },
        changeOrigin: true
      },
    '^/services/frontend/properties/solrwaybackweb/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/properties/solrwaybackweb',
        pathRewrite: { '^/services/frontend/properties/solrwaybackweb/': '' },
        changeOrigin: true
      },
    '^/services/downloadRaw/': {
        target: 'http://localhost:8080/solrwayback/services/downloadRaw/',
        pathRewrite: { '^/services/downloadRaw/': '' },
        changeOrigin: true
      },
    '^/services/viewForward/': {
        target: 'http://localhost:8080/solrwayback/services/viewForward/',
        pathRewrite: { '^services/viewForward/': '' },
        changeOrigin: true
      },
    '^/services/frontend/upload/gethash/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/upload/gethash',
        pathRewrite: { '^/services/frontend/upload/gethash/': '' },
        changeOrigin: true
      },
    '^/services/frontend/util/normalizeurl/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/util/normalizeurl/',
        pathRewrite: { '^/services/frontend/util/normalizeurl/': '' },
        changeOrigin: true
      },
    '^/services/export/warc/': {
        target: 'http://localhost:8080/solrwayback/services/export/warc',
        pathRewrite: { '^/services/export/warc/': '' },
        changeOrigin: true
      },
    '^/services/export/warcExpanded/': {
        target: 'http://localhost:8080/solrwayback/services/export/warcExpanded',
        pathRewrite: { '^/services/export/warcExpanded/': '' },
        changeOrigin: true
      },
    '^/services/export/csv/': {
        target: 'http://localhost:8080/solrwayback/services/export/csv',
        pathRewrite: { '^/services/export/csv/': '' },
        changeOrigin: true
      },
   '^/services/frontend/harvestDates/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/harvestDates/',
        pathRewrite: { '^/services/frontend/harvestDates/': '' },
        changeOrigin: true
      },
    '^/services/frontend/help/about/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/help/about/',
        pathRewrite: { '^/services/frontend/help/about/': '' },
        changeOrigin: true
      },
    '^/services/timestampsforpage/': {
      target: 'http://localhost:8080/solrwayback/services/timestampsforpage/',
      pathRewrite: { '^/services/timestampsforpage/': '' },
      changeOrigin: true
      },
    '^/services/frontend/image/pagepreview/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/image/pagepreview/',
        pathRewrite: { '^/services/frontend/image/pagepreview/': '' },
        changeOrigin: true
      },
    '^/services/frontend/help/search/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/help/search/',
        pathRewrite: { '^/services/frontend/help/search/': '' },
        changeOrigin: true
      },
    '^/services/frontend/graph/domain_result/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/graph/domain_result/',
        pathRewrite: { '^/services/frontend/graph/domain_result/': '' },
        changeOrigin: true
      },
    '^/services/frontend/wordcloud/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/wordcloud/',
        pathRewrite: { '^/services/frontend/wordcloud/': '' },
        changeOrigin: true
      },
    '^/services/frontend/wordcloud/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/wordcloud/',
        pathRewrite: { '^/services/frontend/wordcloud/': '' },
        changeOrigin: true
      },
      '^/services/statistics/domain/': {
        target: 'http://localhost:8080/solrwayback/services/statistics/domain/',
        pathRewrite: { '^/services/statistics/domain/': '' },
        changeOrigin: true
      },
      '^/services/frontend/smurf/text/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/smurf/text/',
        pathRewrite: { '^/services/frontend/smurf/text/': '' },
        changeOrigin: true
      },
      '^/services/frontend/smurf/tags/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/smurf/tags/',
        pathRewrite: { '^/services/frontend/smurf/tags/': '' },
        changeOrigin: true
      },
      '^/services/frontend/images/search/location/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/images/search/location/',
        pathRewrite: { '^/services/frontend/images/search/location/': '' },
        changeOrigin: true
      },
      '^/services/generatepwid/': {
        target: 'http://localhost:8080/solrwayback/services/generatepwid/',
        pathRewrite: { '^/services/generatepwid/': '' },
        changeOrigin: true
      },
      '^/services/warc/header/': {
        target: 'http://localhost:8080/solrwayback/services/warc/header/',
        pathRewrite: { '^/services/warc/header/': '' },
        changeOrigin: true
      },
      '^/services/frontend/tools/linkgraph/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/tools/linkgraph/',
        pathRewrite: { '^/services/frontend/tools/linkgraph/': '' },
        changeOrigin: true
      },
      '^/services/export/linkgraph/': {
        target: 'http://localhost:8080/solrwayback/services/export/linkgraph',
        pathRewrite: { '^/services/export/linkgraph/': '' },
        changeOrigin: true
      },
      'services/frontend/solr/search/facets/loadmore/' : {
        target: 'http://localhost:8080/solrwayback/services/frontend/solr/search/facets/loadmore/',
        pathRewrite: { '^/services/frontend/solr/search/facets/loadmore/': '' },
        changeOrigin: true
      }
    }
  },
  publicPath: './',
  assetsDir: 'static',
  indexPath: 'solrwayback_index_page.html',
  chainWebpack: config => {
    config
        .plugin('html')
        .tap(args => {
            args[0].template = './public/solrwayback_index_page.html'
            args[0].title = 'SolrWayback'
            return args
        })
} 
}