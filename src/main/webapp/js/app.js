
Vue.filter('facetName', function(value) {
    if (!value) return '';
    var newValue = value.split('_').join(' ')
    return newValue;
})

Vue.component('search-box', {
    props: ['setupSearch','myQuery','imageSearch','clearSearch'],
    template: `
    <div>
        <div id="searchbox">
            <div>
                <input  id="queryInput"  v-on:keyup.enter="setupSearch('search',queryModel, imageSearchModel);searchByFile = false" 
                v-model='queryModel' type="text" placeholder="search" autofocus />
                <button class="btn" 
                v-on:click="setupSearch('search', queryModel, imageSearchModel);searchByFile = false">Search</button>
                <span class="link clearSearchLink"  v-on:click="clearSearch();searchByFile = false">Clear search</span>
                <br>
                <label>
                    <input class="imageSearchCheck" v-model="imageSearchModel" type="checkbox"
                    v-on:change="setupSearch('search',queryModel, imageSearchModel);searchByFile = false"> Image search
                </label>
                <span class="link clearSearchLink"  v-on:click="clearSearch();searchByFile = !searchByFile">Search with uploaded file</span>               
            </div>
        </div>
        <div v-if="searchByFile" id="uploadfilesContainer" class="box">
             <label>Upload file: <input  v-on:change="searchHash($event)" type="file" id="uploadfiles"  name="uploadfiles"/></label>
        </div>
    </div>    
    `,
    data: function() {
        return {
            queryModel: this.myQuery,
            imageSearchModel: this.imageSearch,
            searchByFile: false,
        };
    },
    watch: { // updating v-model when vars are updated
        imageSearch: function () {
            this.imageSearchModel = this.imageSearch;
        },
        myQuery: function () {
            this.queryModel = this.myQuery;
        }
    },
    methods:{
        searchHash: function(event){
            var file = event.target.files[0];
            var url = "http://" + location.host + "/solrwayback/services/upload/gethash";
            var data = new FormData();
            data.append('file', file);
            this.$http.post(url,data).then((response) => {
                var sha1 = response.body;
                this.setupSearch('search', 'hash:"' + sha1 + '"');
            }, (response) => {
                console.log('error: ', response);
            });
        },
    }
})

Vue.component('selected-facets-box', {
    props: ['setupSearch','facetFields','myQuery','clearFacets'],
    template: `
    <div id="selectedFacetsbox" class="box">
        <span>Selected facets:</span>
        <ul>
            <li v-for="(key, index) in facetFields">
                <span class="selectedFacetName" v-on:click="removeFacet(index)" v-for="(key, index) in facetFields[index]">{{index | facetName}}: <span class=bold>{{key}}</span> 
                    <span class="deleteIcon"></span>
                </span>
            </li>
        </ul>
        <a v-if="facetFields.length > 1" v-on:click="clearFacets()">Clear all</a>
    </div>
    `,
    methods:{
        removeFacet: function(facetField){
            for(var i = 0; i < this.facetFields.length; i++){ //looping through facetField filters object
                if(this.facetFields[i].hasOwnProperty(facetField)) { //removing object with clicked facet field if exits
                    this.facetFields.splice(i, 1);
                }
            }
            this.setupSearch('facet',this.myQuery);
        }
    }
})

Vue.component('facet-box', {
    props: ['setupSearch','myQuery','myFacets'],
    template: `
    <div>
        <div id="facets">
            <h2>Limit results</h2>
            <div class="facet" v-for="(facets,key) in myFacets">
                <h3>{{ key | facetName }}</h3>
                <ul v-for="(index, facet) in facets.length / 2">
                    <li v-if="facets[2*(index-1)+1] > 0">
                        <a v-on:click="facetClicked(key,facet)"> {{ facets[2*(index-1)] }}  : {{ facets[2*(index-1)+1] }}</a>
                    </li>
                </ul>
            </div>  
        </div>
    </div>
    `,
    methods:{
        facetClicked: function(facetField,index){
            var facetTerm = this.myFacets[facetField][2*(index)];
            this.setupSearch('facet',this.mySearch,facetField,facetTerm);
        }
    }
})

Vue.component('pager-box', {
    props: ['setupSearch', 'totalHits', 'start','isBottom','myQuery','filters','showSpinner','hideSpinner','imageSearch'],
    template: `
    <div class="counterBox" :class="{bottom : isBottom}" v-if="totalHits > 0">
        <div class="selectDownload" v-if="!isBottom">
            <span onclick="$('#downloadMenu,.downloadArrow').toggle()" class="downloadMenuLink">Download result as CSV 
                <span class="downloadArrow">&#9660;</span>
                <span class="downloadArrow" style="display:none;">&#9650;</span>
            </span>
            <ul id="downloadMenu">
                <li><a :href="exportResult('brief')" onclick="$('#downloadMenu,.downloadArrow').toggle()">Download brief result</a></li>
                <li><a :href="exportResult('full')" onclick="$('#downloadMenu,.downloadArrow').toggle()">Download full result</a></li>
            </ul>           
        </div>      

        <div v-if="totalHits > 0 && !imageSearch" class="resultCount">
            <h3 v-if="parseInt(start) + 20 < totalHits" >Showing  {{ parseInt(start) + 1 }}-{{ parseInt(start) + 20 }} of {{ totalHits }} hits</h3>
            <h3  v-else>Showing {{ parseInt(start) + 1 }}-{{ totalHits }} of {{ totalHits }} hits</h3>
        </div>

        <div class="pagerBox" v-if="totalHits > 21 && !imageSearch">
            <button :disabled="start == 0" class="pager prev" v-on:click="setupSearch('paging','','prev')">Previous</button>
            <button :disabled="parseInt(start) + 20 > this.totalHits" class="pager next" v-on:click="setupSearch('paging','','next')">Next</button>       
        </div>
    </div>
    `,
    data: function() {
        return {
            downloadBriefUrl: '',
        };
    },
    methods:{
        exportResult: function(downloadType){
            return 'http://' + location.host + '/solrwayback/services/export/' + downloadType + '?query=' + this.myQuery + '&fq=' + this.filters;
        }
    },
})

Vue.component('result-box', {
    props: ['searchResult','imageObjects','setupSearch','getOffset','clearFacets','baseUrl'],
    template: `
    <div class="searchResults">
        <div v-for="doc in searchResult" class="searchResultItem">
            <div class="item">
                <h3>
                <a v-bind:href=" baseUrl + 'services/view?arcFilePath=' + doc.arc_full + '&offset=' + getOffset(doc)" target="_blank">
                    <span v-if="doc.title">{{ doc.title }}</span>
                    <span v-else>No title available</span>
                </a>
                </h3>
            </div>
            <div v-if="doc.arc_harvesttime" class="item">
                <div class="label">Harvest time:</div>
                <div class="text">{{ doc.arc_harvesttime }}</div>
            </div>  
            <div v-if="doc.last_modified" class="item">
                <div class="label">Last modified:</div>
                <div class="text">{{ doc.last_modified }}</div>
            </div>
            <div v-if="doc.content_type" class="item">
                <div class="label">Content type:</div>
                <div class="text">{{ doc.content_type[0] }}</div>
            </div>
            <div v-if="doc.domain" class="item">
                <div class="label">Domain:</div>
                <div class="text"><a v-bind:href="'http://' + doc.domain"  target="_blank">{{ doc.domain }}</a></div>
            </div>
            <div v-if="doc.url" class="item">
                <div class="label">Url:</div>
                <div class="text"><a v-bind:href="doc.url" target="_blank">{{ doc.url }}</a></div>
            </div>
            <div v-if="doc.score" class="item">
                <div class="label">Score:</div>
                <div class="text">{{ doc.score }}</div>
            </div> 
            <div v-if="doc.content" class="item">
                <div class="label">Content:</div>
                <div class="text"></div>
                <div v-if="doc.content[0].length > 120" class="text long clickable" onclick="$(this).toggleClass('active')"> {{ doc.content[0] }}</div>
                <div v-else class="text long"> {{ doc.content[0] }}</div>
            </div>              
            
            <!-- Full post -->
            <div class="item" onclick="$(this).next().toggle();$(this).toggleClass('active')">
                <div class="link fullPost" > full post</div>
            </div>
            <div class="fullpost">
                <div v-for="(value, key) in doc" class="item">
                    <template v-if="key !== 'content'">
                        <div class="label">{{ key | facetName }}</div>
                        <div class="text">{{ value }}</div>
                    </template>    
                </div>
            </div> 
            
            <!-- Download PDF's, Word docs etc. -->
            <div v-if="doc.content_type_norm && doc.content_type_norm != 'html' && doc.content_type_norm != 'other' && doc.content_type_norm != 'image'" class="item">
                <div class="download">
                    <a v-bind:href=" baseUrl + 'services/downloadRaw?arcFilePath=' + doc.arc_full + '&offset=' + getOffset(doc)"  target="_blank">
                       Download {{ doc.content_type_norm }}
                    </a>
                </div>  
            </div>
             
            <!-- Images -->    
            <div v-if="doc.content_type_norm && doc.content_type_norm == 'image'" class="item">
                <div class="image">
                    <a v-bind:href=" baseUrl + 'services/downloadRaw?arcFilePath=' + doc.arc_full + '&offset=' + getOffset(doc)" target="_blank">
                        <img v-bind:src=" baseUrl + 'services/downloadRaw?arcFilePath=' + doc.arc_full + '&offset=' + getOffset(doc)"/>
                    </a>
                </div>  
            </div>
              
             <!-- Images in HTML pages -->  
            <div v-if="doc.content_type && doc.content_type[0] == 'text/html'" class="item">
                <div class="thumbs">
                    <template v-for="image in imageObjects" v-if="doc.id == image.imageID">
                        <div class="thumb" v-for="(imageUrl, index) in image.imageUrls">
                            <a :href="image.downloadUrls[index]" target="_blank">
                                <span v-html="imageUrl"></span>                  
                            </a>
                            <br/>  
                            <span class="link" v-on:click="setupSearch('search', 'hash:&quot;' + image.hashes[index] + '&quot;');clearFacets()">Search for image</span>
                        </div>
                    </template>
                </div>   
            </div>
            
        </div>
    </div>    
    `
})


Vue.component('result-box-images', {
    props: ['searchResult','setupSearch','clearFacets'],
    template: `
    <div class="searchResults images">
        <div v-for="doc in searchResult" class="searchResultItem">
             <div class="thumb"><a v-bind:href="doc.downloadUrl" target="_blank"><img v-if="doc.imageUrl" v-bind:src="doc.imageUrl + '&height=200&width=200'"/></a></div>
             <div class="link" v-if="doc.imageUrl" v-on:click="setupSearch('search', 'hash:&quot;' + doc.hash + '&quot;');clearFacets()">Search for image</div>
        </div>
    </div>    
    `
})

Vue.component('zerohits-box', {
    props: ['myQuery','imageSearch'],
    template: `
    <div class="box">
        <p>Your <span v-if="imageSearch">image </span> search for <span class="bold">{{ myQuery }}</span> gave 0 results.</p>
    </div>
    `
})

Vue.component('error-box', {
    props: ['errorMsg', 'myQuery'],
    template: `
    <div class="box">
        <p>Your search for <span class="bold">{{ myQuery }}</span> gave following error: <span class="bold">{{errorMsg}}</span></p>
    </div>
    `
})

var router = new VueRouter({
    mode: 'history',
    routes: []
});

var app = new Vue({
    router,
    el: '#app',
    data: {
        searchResult: null,
        myFacets: '',
        myQuery: '',
        facetFields: [],
        filters: '',
        totalHits: 0,
        start: 0,
        imageSearch: false,
        spinner: false,
        errorMsg: '',
        imageObjects: [],
        baseUrl: '',
    },
    watch: { //updating when route is changing
        '$route' () {
           this.getQueryparams();
           this.doSearch();
        }
    },
    created: function() {
        this.$http.get( "http://" + location.host +  "/solrwayback/services/properties/solrwaybackweb").then((response) => {
            this.baseUrl = response.body['wayback.baseurl'];
        }, (response) => {
            console.log('error: ', response);
        });
        this.getQueryparams();
        this.doSearch();
    },
    methods: {
        setupSearch: function(type, query, param3, param4) {
            this.imageObjects = []; //resetting imageObjecs on new search
            if (type == "search") {
                this.filters = ''; ////resetting filters on new search
                this.myQuery = query;
                this.start = 0;
                if (param3) {
                    this.imageSearch = param3;
                } else {
                    this.imageSearch = false;
                }
            }
            if (type == "paging") {
                if (param3 === "prev") {
                    if (this.start >= 20) {
                        this.start = this.start - 20;
                    }
                } else if (param3 === "next") {
                    if (this.start + 20 < this.totalHits) {
                        this.start = this.start + 20;
                    }
                }
                else {
                    this.start = 0;
                }
            }
            if (type == "facet") {
                this.filters = ''; //resetting filters
                if (param3) {
                    var tempObj = {[param3]: param4}; //Facet field and facet term saved in object and pushed to array
                    this.facetFields.push(tempObj);
                }
                this.filters = ''; //Setting up filters string for search URL
                for (var i = 0; i < this.facetFields.length; i++) {
                    if (i > 0) {
                        this.filters = this.filters + '%20AND%20'
                    }
                    for (var key in this.facetFields[i]) {
                        this.filters = this.filters + key + '%3A' + this.facetFields[i][key]
                    }
                }
                this.start = 0; //resetting pager
            }

            router.push({
                query: {
                    query: this.myQuery,
                    start: parseInt(this.start),
                    filter: this.filters,
                    imgsearch: this.imageSearch
                }
            });
        },

        doSearch: function(){
            if(!this.imageSearch || this.imageSearch == 'false' ){ //converting possible string value from query param to boolean
                this.imageSearch = false
            }else{
                this.imageSearch = true
            }
            if (!this.imageSearch) {
                this.searchUrl = 'http://' + location.host + '/solrwayback/services/solr/search?query=' + this.myQuery +
                    '&start=' + parseInt(this.start) + '&fq=' + this.filters;
            } else {
                this.searchUrl = 'http://' + location.host + '/solrwayback/services/images/search?query=' + this.myQuery +
                    '&start=' + this.start + '&fq=' + this.filters;
            }
            //console.log('this.searchUrl',this.searchUrl);
            this.facetFields = []; //resetting facet fields before building them from query params
            if(this.filters){
                var facetPairs = this.filters.split('%20AND%20');
                facetFieldsTemp = [];
                for( var i = 0; i < facetPairs.length; i++ ){
                    facetFieldsTemp = facetPairs[i].split('%3A');
                    var tempObj = {[facetFieldsTemp[0]]: facetFieldsTemp[1]}; //Facet field and facet term saved in object and pushed to array
                    this.facetFields.push(tempObj);
                }
            }
            /* Starting search if there's a query*/
            if(this.myQuery && this.myQuery.trim() != ''){
                this.showSpinner();
                this.$http.get(this.searchUrl).then((response) => {
                    this.errorMsg = "";
                    console.log('response.body: ', response.body);
                    if(response.body.error){
                        this.errorMsg = response.body.error.msg;
                        this.hideSpinner();
                        return;
                    }
                    if(!this.imageSearch){
                        this.searchResult = response.body.response.docs;
                        /* Nyt objektet med image URL'er ved content type HTML */
                        for(var i=0; i<this.searchResult.length;i++){
                            if(this.searchResult[i].content_type && this.searchResult[i].content_type[0] == 'text/html'){
                            	this.getImages(this.searchResult[i].id,this.searchResult[i].arc_full,this.getOffset(this.searchResult[i]));
                            }
                        }
                        this.myFacets=response.body.facet_counts.facet_fields;
                        this.totalHits = response.body.response.numFound;
                    }else{
                        this.searchResult = response.body;
                        this.totalHits = this.searchResult.length;
                    }
                    $("html, body").animate({ scrollTop: 0 }, "fast");
                    this.hideSpinner();
                }, (response) => {
                    console.log('error: ', response);
                });
            }
        },

        getImages: function(id,arc_full, offset){
            var imageInfoUrl = "http://" + location.host + "/solrwayback/services/images/htmlpage?arc_full=" + arc_full +"&offset="+offset;

            /* ImageInfoUrl for local developement*/
            if(location.host==='localhost:8080'){
                var imageInfoUrl = "http://" + location.host + "/solrwayback/services/images/htmlpage?arc_full=" + arc_full +"&offset="+offset+'&test=true'
            }
            this.$http.get(imageInfoUrl).then((response) => {
                var imageUrl = "";
                var downloadUrl = "";
                var hash = "";
                var imageUrlArray = [];
                var downloadArray = [];
                var hashArray = [];
                for(var j=0;j<response.body.length;j++){
                    imageUrl = response.body[j].imageUrl;
                    downloadUrl = response.body[j].downloadUrl;
                    hash = response.body[j].hash;
                    var imageHTML = '<img src="' + imageUrl + '&width=100&height=100">'
                    imageUrlArray.push(imageHTML);
                    downloadArray.push(downloadUrl);
                    hashArray.push(hash);
                }
                this.imageObjects.push({imageID: id, imageUrls: imageUrlArray, downloadUrls: downloadArray, hashes: hashArray});
            }, (response) => {
                console.log('error: ', response);
            });
        },

        getOffset: function(doc) {
            if (doc.source_file_offset) {
                return doc.source_file_offset
            } else {
                return doc.source_file_s.split('@')[1]
            }
        },

        getQueryparams:function(){
            this.myQuery = this.$route.query.query;
            this.start= this.$route.query.start;
            this.filters = this.$route.query.filter;
            this.imageSearch = this.$route.query.imgsearch;
        },

        clearFacets: function(){
            this.facetFields = [];
            this.filters = "";
            this.setupSearch('search',this.myQuery);
        },

        clearSearch: function(){
            this.facetFields = [];
            this.filters = "";
            this.myQuery = "";
            this.searchResult = "";
        },

        showSpinner: function(){
            this.spinner = true;
        },

        hideSpinner: function(){
            this.spinner = false;
        },
    }
})

