Vue.component('header-container', {
    props: [],
    template: `
    <div id="header">
        <h2>SOLR Wayback page resources</h2>
    </div>    
    `,
})

var router = new VueRouter({
    mode: 'history',
    routes: []
})

var app = new Vue({
    router,
    el: '#app',
    data: {
        source_file_path: "",
    },
    methods: {
        getQueryparams:function(){
            this.source_file_path = this.$route.query.source_file_path;
            this.offset = this.$route.query.offset;
            this.getTimestamps();
            console.log('this.source_file_path', this.source_file_path);
            console.log('this.offset', this.offset);
        },
        getTimestamps: function(){
            this.$http.get("http://localhost:8080/solrwayback/services/frontend/timestampsforpage?source_file_path=/media/teg/1200GB_SSD/netarkiv/0105/filedir/272018-267-20170310091550256-00008-kb-prod-har-003.kb.dk.warc.gz&offset=70062443").then((response) => {
                console.log('response.body: ', response.body);
                //this.hideSpinner();
            }, (response) => {
                console.log('error: ', response);
            });
        }
        /*showSpinner: function(){
            this.spinner = true;
        },
        hideSpinner: function(){
            this.spinner = false;
        },*/
    },
    created: function() {
        this.getQueryparams()
        /*this.spinner = true;*/
    }
})

