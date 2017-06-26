
Vue.filter('facetName', function(value) {
    if (!value) return '';
    //var valueArr = value.toString().split('_');
    //var newValue = valueArr[valueArr.length-1];
    var newValue = value.split('_').join(' ')
    return newValue;
})

Vue.component('search-box', {
    props: ['doSearch','myQuery','imageSearch'],
    template: `
    <div id="searchbox">
        <div>
            <input  id="queryInput"  v-on:keyup.enter="doSearch('search',queryModel, imageSearchModel)" 
            v-model='queryModel' type="text" placeholder="search" autofocus />
            <button class="btn" 
            v-on:click="doSearch('search', queryModel, imageSearchModel)">Search</button><br>
            <label>
                <input class="imageSearchCheck" v-model="imageSearchModel" type="checkbox"
                v-on:change="doSearch('search',queryModel, imageSearchModel)"> Image search
            </label>
        </div>
    </div>
    `,
    data: function() {
        return {
            queryModel: this.myQuery,
            imageSearchModel: this.imageSearch,
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
})

Vue.component('selected-facets-box', {
    props: ['doSearch','facetFields','myQuery','clearFacets'],
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
            this.doSearch('facet',this.myQuery);
        }
    }
})

Vue.component('facet-box', {
    props: ['doSearch','myQuery','myFacets'],
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
            this.doSearch('facet',this.mySearch,facetField,facetTerm);
        }
    }
})

Vue.component('pager-box', {
    props: ['doSearch', 'totalHits', 'start','disabledPrev','disabledNext','isBottom'],
    template: `
    <div :class="{bottom : isBottom}">
        <div v-if="totalHits > 0" class="resultCount">
            <h3 v-if="start + 20 < totalHits" >Showing  {{ start + 1 }}-{{ start + 20 }} of {{ totalHits }} hits</h3>
            <h3  v-else>Showing {{ start + 1 }}-{{ totalHits }} of {{ totalHits }} hits</h3>
        </div>

        <div class="pagerBox" v-if="totalHits > 21">
            <button :disabled="disabledPrev" class="pager prev" v-on:click="doSearch('paging','','prev')">Previous</button>
            <button :disabled="disabledNext" class="pager next" v-on:click="doSearch('paging','','next')">Next</button>
        </div>
    </div>
    `
})

Vue.component('result-box', {
    props: ['searchResult'],
    template: `
    <div class="searchResults">
        <div v-for="doc in searchResult" class="searchResultItem">
            <div class="item"><h3><a>{{ doc.id }}</a></h3></div>
            <div v-if="doc.title" class="item">
                <div class="label">Title:</div>
                <div class="text">{{ doc.title }}</div>
            </div>
            <div v-if="doc.domain" class="item">
                <div class="label">Domain:</div>
                <div class="text"><a v-bind:href="'http://' + doc.domain"  target="_blank">{{ doc.domain }}</a></div>
            </div>
            <div v-if="doc.url" class="item">
                <div class="label">Url:</div>
                <div class="text"><a v-bind:href="doc.url" target="_blank">{{ doc.url }}</a></div>
            </div> 
            
            <div v-if="doc.content_type_norm && doc.content_type_norm === 'html'" class="item">
                <div class="label">SOLR Wayback:</div> 
                <div class="text">
                    <a v-bind:href="'http://belinda:9721/solrwayback/services/view?arcFilePath=' + doc.arc_full + '&offset=' + (doc.source_file_s).split('@')[1]"  
                    target="_blank">See Page in SOLR Wayback</a>
                    <!--   + doc.arc_full + '&offset=' + (doc.source_file_s).split('@')[1]"  -->
                </div>
                
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
            <div v-if="doc.content" class="item">
                <div class="label">Content:</div>
                <div class="text"></div>
                <div v-if="doc.content[0].length > 130" class="text long clickable" onclick="$(this).toggleClass('active')"> {{ doc.content[0] }}</div>
                <div v-else class="text long"> {{ doc.content[0] }}</div>
            </div>
              
            <div v-if="doc.content_type_norm && doc.content_type_norm != 'html' && doc.content_type_norm != 'other' && doc.content_type_norm != 'image'" class="item">
                <div class="image">
                    <a v-bind:href="'http://belinda:9721/solrwayback/services/downloadRaw?arcFilePath=' + doc.arc_full + '&offset=' + (doc.source_file_s).split('@')[1]"  target="_blank">
                       Download
                    </a>
                </div>  
            </div>   
            <div v-if="doc.content_type_norm && doc.content_type_norm == 'image'" class="item">
                <div class="image">
                    <a v-bind:href="'http://belinda:9721/solrwayback/services/downloadRaw?arcFilePath=' + doc.arc_full + '&offset=' + (doc.source_file_s).split('@')[1]" target="_blank">
                        <img v-bind:src="'http://belinda:9721/solrwayback/services/downloadRaw?arcFilePath=' + doc.arc_full + '&offset=' + (doc.source_file_s).split('@')[1]"/>
                    </a>
                </div>  
            </div>  
            <div v-if="doc.content_type && doc.content_type[0] == 'text/html'" class="item">
                <div class="label">Thumbnail:</div>
                <div class="text">Thumbnail ID: {{ doc.source_file_s }}</div>
                <!--<div class="thumb" v-html="getImage(doc.source_file_s)"></div>-->   
            </div> 
        </div>
    </div>    
    `,
    methods:{
        getImage: function(source) {
            this.imageUrl = "";
            var imageInfoUrl = "http://" + location.host + "/solrwayback/services/images/htmlpage?source_file_s=" + source + '&test=true';
            //var imageInfoUrl = "http://" + location.host + "/solrwayback/services/images/htmlpage?source_file_s=" + source;
            this.$http.get(imageInfoUrl).then((response) => {
                this.imageUrl = response.body[0].imageUrl;
                return '<img src="' + this.imageUrl + '&width=100&height=100">';
                //this.getImageHtml();
            }, (response) => {
                console.log('error: ', response);
            });
            return '<img src="' + this.imageUrl + '&width=100&height=100">';
        }/*,
        getImageHtml: function(source) {
            return '<img src="' + this.imageUrl + '&width=100&height=100">';
        }*/

    }
})


Vue.component('result-box-images', {
    props: ['searchResult','doSearch'],
    template: `
    <div class="searchResults images">
        <div v-for="doc in searchResult" class="searchResultItem">
             <div class="thumb"><a v-bind:href="doc.downloadUrl" target="_blank"><img v-if="doc.imageUrl" v-bind:src="doc.imageUrl + '&height=200&width=200'"/></a></div>
             <div class="link" v-on:click="doSearch('search', 'hash:' + doc.hash)">Search for image</div>
        </div>
    </div>    
    `
})

Vue.component('zerohits-box', {
    props: ['myQuery'],
    template: `
    <div class="box">
        <p>Your search for <span class="bold">{{ myQuery }}</span> gave 0 results.</p>
    </div>
    `
})

var app = new Vue({
    el: '#app',
    data: {
        searchResult: '',
        myFacets: '',
        myQuery: '',
        facetFields: [],
        filters: '',
        totalHits: 0,
        start: 0,
        imageSearch: false,
        disabledPrev: false,
        disabledNext: false,
        spinner: false,
        errorMsg: '',
    },
    methods: {

        doSearch: function(type, query, param3, param4){
            if(type == "search") {
                this.searchType = type;
                this.myQuery = query;
                this.start = 0;
                if(param3){
                    this.imageSearch = param3;
                }else{
                    this.imageSearch = false;
                }
            }
            if(type == "paging"){
                if(param3 === "prev"){
                    if(this.start >= 20){
                        this.start = this.start - 20;
                    }
                }else if(param3 === "next"){
                    if(this.start + 20 < this.totalHits){
                        this.start = this.start + 20;
                    }
                }
                else{
                    this.start = 0;
                    this.disabledPrev = true;
                }
            }
            if(type == "facet"){
                this.searchType = type;
                if(param3){
                    var tempObj = {[param3] : param4}; //Facet field and facet term saved in object and pushed to array
                    this.facetFields.push(tempObj);
                }
                this.filters = ''; //Setting up filters string for search URL
                for(var i=0; i < this.facetFields.length; i++){
                    if(i > 0){
                        this.filters = this.filters + '%20AND%20'
                    }
                    for (var key in this.facetFields[i]) {
                        this.filters = this.filters + key + '%3A' + this.facetFields[i][key]
                    }
                }
                this.start = 0; //resetting pager
            }

            if(!this.imageSearch){
                var searchUrl = 'http://' + location.host + '/solrwayback/services/solr/search?query=' + this.myQuery +
                    '&start=' + this.start + '&fq=' + this.filters;
            }else{
                var searchUrl = 'http://' + location.host + '/solrwayback/services/images/search?query=' + this.myQuery +
                   '&start=' + this.start + '&fq=' + this.filters;
            }

            /* Starting search if there's a query*/
            if(this.myQuery && this.myQuery.trim() != ''){
                this.showSpinner();
                this.$http.get(searchUrl).then((response) => {
                    this.errorMsg = "";
                    console.log('response: ', response);
                    /*
                    if(response.body.error){
                        this.errorMsg = response.body.error.msg;
                        this.hideSpinner();
                        return;
                    }*/
                    if(!this.imageSearch){
                        this.searchResult = response.body.response.docs;
                        console.log('this.searchResult: ', this.searchResult);
                        /* Eksperiment med at berige objektet med imagae URL'er ved content type HTML */
                        for(var i=0; i<this.searchResult.length;i++){
                            if(this.searchResult[i].content_type && this.searchResult[i].content_type[0] == 'text/html'){
                                this.imageUrl = "";
                                var imageInfoUrl = "http://" + location.host + "/solrwayback/services/images/htmlpage?source_file_s=" + this.searchResult[i].source_file_s + '&test=true';
                                //var imageInfoUrl = "http://" + location.host + "/solrwayback/services/images/htmlpage?source_file_s=" + source;
                                this.$http.get(imageInfoUrl).then((response) => {
                                    for(var j=0;j<response.body.length;j++){
                                        this.imageUrl = response.body[j].imageUrl;
                                        var minImageUrl = '<img src="' + this.imageUrl + '&width=100&height=100">'
                                        console.log(minImageUrl);
                                    }

                                }, (response) => {
                                    console.log('error: ', response);
                                });
                            }
                        }
                        this.myFacets=response.body.facet_counts.facet_fields;
                        this.totalHits = response.body.response.numFound;
                    }else{
                        this.searchResult = response.body;
                    }
                    this.disabledPrev = false; // resetting paging buttons
                    this.disabledNext = false;
                    if(this.start + 20 > this.totalHits){
                        this.disabledNext = true;
                    }
                    if(this.start == 0){
                        this.disabledPrev = true;
                    }
                    $("html, body").animate({ scrollTop: 0 }, "fast");
                    this.hideSpinner();
                }, (response) => {
                    console.log('error: ', response);
                });
            }
        },

        clearFacets: function(){
            this.facetFields = [];
            this.filters = "";
            this.doSearch('search',this.myQuery);
        },

        showSpinner: function(){
            this.spinner = true;
        },

        hideSpinner: function(){
            this.spinner = false;
        },
    }
})

