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
    },
    methods: {
        getData: function(){
            this.showSpinner();
            this.url = this.$route.query.url.replace(/http.*:\/\//i,""); //Get domain from URL, using replace and regex to trim domain
            console.log('this.url', this.url);
            var searchUrl = 'http://' + location.host + '/solrwayback/services/solr/search?query=*:*&start=0&fq=domain%3A' + this.url;

            this.$http.get(searchUrl).then((response) => {
                this.hideSpinner();
                this.errorMsg = "";
                console.log('response: ', response);
                if(response.body.error){
                    this.errorMsg = response.body.error.msg;
                    return;
                }
            }, (response) => {
                console.log('error: ', response);
                this.hideSpinner();
            });
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

