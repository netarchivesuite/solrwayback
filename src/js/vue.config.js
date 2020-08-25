process.env.VUE_APP_VERSION = process.env.NODE_ENV === 'production' ? require('./package.json').version : 'DEVELOPMENT BUILD'
module.exports = {
  devServer: {
    proxy: {
    '/services/frontend/solr/': {
      target: 'http://localhost:8080/solrwayback/services/frontend/solr/',
      pathRewrite: { '^/services/frontend/solr/': '' },
      changeOrigin: true
      },
    '/services/frontend/images/htmlpage/': {
      target: 'http://localhost:8080/solrwayback/services/frontend/images/htmlpage/',
      pathRewrite: { '^/services/frontend/images/htmlpage/': '' },
      changeOrigin: true
      },
    }
  },
publicPath: process.env.NODE_ENV === 'production'
? '/solrwayback/'
: '/',
assetsDir: 'static',


chainWebpack: (config) => {
  if (process.env.NODE_ENV === 'production') {
      config.plugin('html').tap((opts) => {
          opts[0].filename = './solrwayback_index_page.html'
          return opts
      })
  }
},
}