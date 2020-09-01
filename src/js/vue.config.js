process.env.VUE_APP_VERSION = process.env.NODE_ENV === 'production' ? require('./package.json').version : 'DEVELOPMENT BUILD'
module.exports = {
  devServer: {
    proxy: {
    '^/services/frontend/solr/': {
      target: 'http://localhost:8080/solrwayback/services/frontend/solr/',
      pathRewrite: { '^/services/frontend/solr/': '' },
      changeOrigin: true
      },
    '^/services/frontend/images/htmlpage/': {
      target: 'http://localhost:8080/solrwayback/services/frontend/images/htmlpage/',
      pathRewrite: { '^/services/frontend/images/htmlpage/': '' },
      changeOrigin: true
      },
      '^/services/viewForward/': {
        target: 'http://localhost:8080/solrwayback/services/viewForward/',
        pathRewrite: { '^services/viewForward/': '' },
        changeOrigin: true
        },
    '^/services/frontend/properties/solrwaybackweb/': {
        target: 'http://localhost:8080/solrwayback/services/frontend/properties/solrwaybackweb',
        pathRewrite: { '^/services/frontend/properties/solrwaybackweb/': '' },
        changeOrigin: true
        },
       
    '^/services/frontend/upload/gethash/': {
          target: 'http://localhost:8080/solrwayback/services/frontend/upload/gethash',
          pathRewrite: { '^/services/frontend/upload/gethash/': '' },
          changeOrigin: true
          },
    }
  },
publicPath: process.env.NODE_ENV === 'production'
? '/solrwayback/'
: '/',
assetsDir: 'static',

indexPath: 'solrwayback_index_page.html' 

}