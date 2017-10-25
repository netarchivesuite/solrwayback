Vue.filter('toLocaleTime', function(value) {
    if (!value) return '';
    var days = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday']
    var date = new Date(value);
    var newValue = days[date.getDay()] + ', ' + date.getDate() + '-' + (date.getMonth() + 1) + '-' + date.getFullYear() +
        ', ' + date.getHours() + ':' + ('0' + (date.getMinutes().toString())).slice(-2);
    return newValue;
})



Vue.component('page-resources', {
    props: ["resourceObj"],
    data: function(){
        return{
            sortParam: "",
            sortOrder: false,
        }
    },
    computed: {
        sortedData: function(){
            if(this.sortOrder){
                return _.orderBy(this.resourceObj.resources,this.sortParam).reverse();
            }else{
                return _.orderBy(this.resourceObj.resources,this.sortParam);
            }

        }
    },
    template: `
    <div id="pageResources">
        <h2>{{resourceObj.pageUrl}} - {{resourceObj.pageCrawlDate | toLocaleTime }}</h2>
        <div class="previewContainer" >
            <div class="previewImg">              
                <h3>Preview</h3>
                <a :href="resourceObj.pagePreviewUrl">           
                     <img class="preview" :src="resourceObj.pagePreviewUrl" alt="webpage preview">
                </a>
            </div>    
            <div class="notHarvested" v-if="resourceObj.notHarvested && resourceObj.notHarvested.length > 0">
                 <h3>Not harvested resources</h3>
                 <ol class="notHarvested">
                    <li v-for="item in resourceObj.notHarvested">{{ item }}</li>
                 </ol>
            </div>
        </div>
        <div class="tableContainer">
            <h3>Harvested resources</h3>
            <table id="resourcesTable">
                <thead>
                    <tr>
                        <th v-on:click="sortParam = 'url';sortOrder =! sortOrder" class="clickable">Resource URL</th>
                        <th v-on:click="sortParam = 'contentType';sortOrder =! sortOrder" class="clickable">Content type</th>
                        <th v-on:click="sortParam = 'crawlTime';sortOrder =! sortOrder" class="clickable">Time diff.</th>
                        <th>See/download</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="resource in sortedData">
                        <td>{{resource.url}}</td>
                        <td>{{resource.contentType}}</td>
                        <td>{{resource.timeDifference}}</td>
                        <td v-if="resource.contentType==='image'"><a :href="resource.downloadUrl"><img :src="resource.downloadUrl"></a></td> 
                        <td v-else><a :href="resource.downloadUrl">Download</a></td> 
                    </tr>
                </tbody>
            </table> 
        </div>
    </div>    
    `
})

var router = new VueRouter({
    mode: 'history',
    routes: []
})

var app = new Vue({
    router,
    el: '#app',
    data: {
        resourceObj : {},
        source_file_path: "",
        offset: ""
    },
    methods: {
        getQueryparams:function(){
            this.source_file_path = this.$route.query.source_file_path;
            this.offset = this.$route.query.offset;
            this.getTimestamps();
        },
        getTimestamps: function(){
            this.showSpinner();
            this.resourceUrl = "http://" + location.host + "/solrwayback/services/frontend/timestampsforpage?source_file_path=" + this.source_file_path + '&offset=' + this.offset;
            this.$http.get(this.resourceUrl).then((response) => {
                this.resourceObj = response.body;
                console.log('response.body: ', response.body);
                this.hideSpinner();
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
        this.getQueryparams()
        /*this.spinner = true;*/
    }
})

