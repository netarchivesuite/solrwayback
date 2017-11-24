Vue.component('header-container', {
    props: ['url'],
    template: `
    <div id="header">
        <h2 v-if="url">Domain: {{ this.url }}</h2>
    </div>    
    `,
})

var router = new VueRouter({
    mode: 'history',
    routes: []
});

var app = new Vue({
    router,
    el: '#app',
    data: {
        spinner: false,
        data: [],
    },
    methods: {
        getData: function(){
            this.showSpinner();
            this.url = this.$route.query.url.replace(/http.*:\/\//i,""); //Get domain from URL, using replace and regex to trim domain
            if( this.url.slice(-1) === "/"){ // if trailing slash on url it's removed
                this.url = this.url.slice(0, -1)
            }
            var searchUrl = 'http://' + location.host + '/solrwayback/services/solr/search?query=*:*&start=0&fq=domain%3A' + this.url;

            this.$http.get(searchUrl).then((response) => {
                this.hideSpinner();
                this.errorMsg = "";
                var tempData = response.body.facet_counts.facet_fields.crawl_year;
                for(var i = 0; i < tempData.length; i++){
                    if(i % 2 == 1){
                        this.data.push(tempData[i])
                    }
                }
                console.log('tempData: ', tempData);
                console.log('response: ', response);
                this.drawChart();
                if(response.body.error){
                    this.errorMsg = response.body.error.msg;
                    return;
                }
            }, (response) => {
                console.log('error: ', response);
                this.hideSpinner();
            });
        },

        drawChart: function(){
            d3.select(".chart")
                .selectAll("div")
                .data(this.data)
                .enter()
                .append("div")
                .style("width", function(d) { return d / 200 + "px" ; })
                .text(function(d) { return d; });
        },

        showSpinner: function(){
            this.spinner = true;
        },

        hideSpinner: function(){
            this.spinner = false;
        },
    },
    created: function() {
        this.getData();
    }
})

