Vue.filter('facetName', function(value) {
    if (!value) return '';
    var newValue = value.split('_').join(' ')
    newValue = newValue.replace(/"/g,"")
    return newValue;
})

Vue.filter('thousandsSeperator', function(value) {
    if (!value) return '';
    var newValue = value.toLocaleString();
    return newValue;
})

/* Component with search field, check boxes to decide searchtype and relevant links */
Vue.component('search-box', {
    props: ['setupSearch','myQuery','imageSearch','imageGeoSearch','urlSearch','clearSearch','grouping','setGrouping'],
    template: `
    <div>
        <div id="searchbox">
            <div>
                <input  id="queryInput"  v-on:keyup.enter="setupSearch('search',queryModel, urlSearchModel, imageSearchModel, imageGeoSearchModel);searchByFile = false" 
                v-model='queryModel' type="text" placeholder="search" autofocus />
                <button class="btn" 
                v-on:click="setupSearch('search', queryModel, urlSearchModel, urlSearchModel, imageSearchModel, imageGeoSearchModel);searchByFile = false">Search</button>
                <label class="groupingCheckLabel">
                <input class="groupingCheck" v-model="groupingModel" type="checkbox"
                     v-on:change="setGrouping(groupingModel);setupSearch('search', queryModel, urlSearchModel, urlSearchModel, imageSearchModel, imageGeoSearchModel);
                     searchByFile = false"> Grouping (slower response)
                </label>
                <span class="link clearSearchLink"  v-on:click="clearSearch();searchByFile = false">Clear search</span>
                <br>
                <label>
                    <input class="imageSearchCheck" v-model="imageSearchModel" type="checkbox"
                    v-on:change="urlSearchModel = false; setupSearch('search',queryModel, urlSearchModel, imageSearchModel);searchByFile = false"> Image search
                </label>
                <label v-if="imageSearch">
                    <input class="imageSearchCheck" v-model="imageGeoSearchModel" type="checkbox"
                     v-on:change="setupSearch('search',queryModel, urlSearchModel, imageSearchModel, imageGeoSearchModel);searchByFile = false"> Geo search
                </label>
                <label>
                    <input class="urlSearchCheck" v-model="urlSearchModel" type="checkbox"
                    v-on:change="searchUrl(urlSearchModel)"> URL search
                </label>
                <span class="link clearSearchLink"  v-on:click="clearSearch();searchByFile = !searchByFile">Search with uploaded file</span> 
                <span class="link clearSearchLink"><a href="./tags.html">Search for HTML-tags</a></span>
                <span class="link clearSearchLink"><a href="./domaingrowth.html">Domain stats</a></span>               
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
            urlSearchModel: this.urlSearch,
            imageGeoSearchModel: this.imageGeoSearch,
            groupingModel: this.grouping,
            searchByFile: false,
        };
    },
    watch: { // updating v-models in search box when vars are updated
        imageSearch: function () {
            this.imageSearchModel = this.imageSearch;
        },
        urlSearch: function () {
            this.urlSearchModel = this.urlSearch;
        },
        imageGeoSearch: function () {
            this.imageGeoSearchModel = this.imageGeoSearch;
        },
        myQuery: function () {
            this.queryModel = this.myQuery;
        }
    },
    methods:{
        /* Method that uses SOLRWayback hashes to perfom search*/
        searchHash: function(event){
            var file = event.target.files[0];
            var url = location.protocol +"//" + location.host + "/solrwayback/services/upload/gethash";
            var data = new FormData();
            data.append('file', file);
            this.$http.post(url,data).then((response) => {
                var sha1 = response.body;
                this.setupSearch('search', 'hash:"' + sha1 + '"');
            }, (response) => {
                console.log('error: ', response);
            });
        },

        /* Method to set up searchfield for dedicated URL search*/
        searchUrl: function(start){
            if(start){
                this.queryModel =  location.protocol +"//"; //helping user writing "http://" in search field
                this.imageSearchModel = false;
                this.imageGeoSearchModel = false;
            }else{
                this.queryModel = ""; //clearing searchfield when URL search is deactivatd
            }
        }
    }
})

/* Component shows Google map with results plugged in */
/* OSM */
Vue.component('map-box', {
    props: ['markerPosition',"placeMarker","doSearch","totalHits"],
    data: function() {
        return {
            markers: [],
            markerCircles: [],
            position: false,
            map: false,
            radiusModel: this.markerPosition.radius/1000,
        };
    },
    template: `
    <div id="googlemapBox">
        <div id="map"></div>
        <div id="infoGeoSearch">
            <div class="infoContainer">
                <h3>Adjust radius</h3> 
                <p>
                Radius in km: <input type="text" v-model="radiusModel" v-on:keyup.enter="placeMarker(position, map, markers, markerCircles, radiusModel)">
                </p>
            </div>
            <div class="infoContainer">
                <h3>Center for this search</h3> 
                <p>Latitude: {{ Math.round(markerPosition.lat * 10000) / 10000  }}</p>
                <p>Longitude: {{ Math.round(markerPosition.lng * 10000) / 10000  }}</p>
            </div>           
            <div class="infoContainer">
                <h3 v-if="totalHits > 0">Number of hits: {{this.totalHits | thousandsSeperator }}</h3>
            </div>      
        </div>
    </div>    
    `,

    /* OSM */
    mounted: function(){
        /* Initialising map when component is mounted, not when app is mounted */
        //latitude and longitude are loaded from the properties
    	    	
        //var center = {lat: 56.17, lng: 10.20};
    	var center = {lat:  parseFloat(app.$data.googleMapLatitude), lng: parseFloat(app.$data.googleMapLongitude)}; 	    	    	
    	var radius = parseInt(app.$data.googleMapRadius);
    	
    	this.map = L.map('map').setView(center, 5);

        L.tileLayer(app.leafletSource, {
        attribution: app.leafletAttribution
        }).addTo(this.map);

        //L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        //attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        //}).addTo(this.map);

    	
        var _this = this;
        
        this.position = center;
        //this.position = new google.maps.LatLng(center);
        
        this.placeMarker(this.position, this.map, this.markers, this.markerCircles, this.radiusModel)
        
         this.map.on('click',function(e){
            _this.position=e.latlng;
            _this.placeMarker(_this.position, _this.map, _this.markers, _this.markerCircles, _this.radiusModel);
        });

      
    }
})

/* Component shows selected facets and has method to deselect them one by one */
Vue.component('selected-facets-box', {
    props: ['setupSearch','facetFields','myQuery','clearFacets'],
    template: `
    <div id="selectedFacetsbox" class="box">
        <span>Selected facets:</span>
        <ul>
            <li v-for="(key, index) in facetFields">
                <span class="selectedFacetName" v-on:click="removeFacet(index)" v-for="(key, index) in facetFields[index]"><span class="capitalize">{{index | facetName}}</span>: <span class=bold>{{key | facetName}}</span> 
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

/* Component shows facets and has method to select them */
Vue.component('facet-box', {
    props: ['setupSearch','myQuery','myFacets'],
    template: `
    <div>
        <div id="facets">
            <h2>Limit results</h2>
            <div class="facet" v-for="(facets,key) in myFacets">
                <h3 class="capitalize">{{ key | facetName }}</h3>
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

/* Component shows hit count, pager and download menu. Has method to download search result */
Vue.component('pager-box', {
    props: ['setupSearch', 'totalHits', 'totalHitsDuplicates','start','isBottom','myQuery','filters','imageSearch','grouping'],
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
                <li><a v-if="$data.allowExportWarc" :href="exportResult('warc')" onclick="$('#downloadMenu,.downloadArrow').toggle()">Download as warc</a></li>                
                <li><a v-if="$data.allowExportWarc" :href="exportResult('warcExpanded')" onclick="$('#downloadMenu,.downloadArrow').toggle()">Download as warc with resources</a></li>
            </ul>           
        </div>      

        <div v-if="totalHits > 0 && !imageSearch" class="resultCount">
            <template v-if="grouping">
                <h3 v-if="parseInt(start) + 20 < totalHits" ><span title="Hit count with unique URLs">Showing  {{ parseInt(start) + 1 }}-{{ parseInt(start) + 20 }} of {{ totalHits | thousandsSeperator }}</span>  unique hits <span class="discrete" title="Hit count with duplicate URLs">(total hits: {{ totalHitsDuplicates | thousandsSeperator }}).</span></h3> 
                <h3  v-else><span title="Hit count with unique URLs">Showing {{ parseInt(start) + 1 }}-{{ totalHits }} of {{ totalHits | thousandsSeperator }}</span> unique hits  <span class="discrete" title="Hit count with duplicate URLs">  (total hits:{{ totalHitsDuplicates | thousandsSeperator }}).</span></h3>
            </template>
            <template v-else>
                <h3 v-if="parseInt(start) + 20 < totalHits" >Showing  {{ parseInt(start) + 1 }}-{{ parseInt(start) + 20 }} of {{ totalHits | thousandsSeperator }} hits</h3>
            <h3  v-else>Showing {{ parseInt(start) + 1 }}-{{ totalHits }} of {{ totalHits | thousandsSeperator }} hits</h3>
            </template>
        </div>

        <div class="pagerBox" v-if="totalHits > 21 && !imageSearch">
            <button :disabled="start == 0" class="pager prev" v-on:click="setupSearch('paging','','prev')">Previous</button>
            <button :disabled="parseInt(start) + 20 > this.totalHits" class="pager next" v-on:click="setupSearch('paging','','next')">Next</button>       
        </div>
    </div>
    `,
     data: function() {
        return {           
            allowExportWarc: app.$data.allowExportWarc,
        };
    },
    methods:{
        exportResult: function(downloadType){
            return  location.protocol +'//' + location.host + '/solrwayback/services/export/' + downloadType + '?query=' + this.myQuery + '&fq=' + this.filters;
        }
    },
})




/* Component shows search result when not image search*/
Vue.component('result-box', {
    props: ['searchResult','imageObjects','setupSearch','clearFacets','baseUrl','getFullpost','fullpost','openbaseUrl'],
    template: `
    <div class="searchResults">
        <div v-for="doc in searchResult" class="searchResultItem">
            <div class="item">
                <h3 title="Playback in SOLR Wayback">
                <a v-bind:href=" baseUrl + 'services/viewForward?source_file_path=' + doc.source_file_path + '&offset=' + doc.source_file_offset" target="_blank">
                    <span v-if="doc.title">{{ doc.title }}</span>
                    <span v-else>No title available</span>
                </a>
                </h3>
                <span v-if="openbaseUrl && (doc.content_type_norm === 'html' || doc.content_type_norm === 'text')">
                    <a v-bind:href="openbaseUrl  + doc.wayback_date + '/' + doc.url" target="_blank">
                    <img src="./images/newwindow.png" alt="Playback in Open Wayback"  title="Playback in Openwayback"/>
                    </a>
                </span>
            </div>
            <div v-if="doc.content_type" class="item">
                <div class="label">Content type:</div>
                <div class="text">{{ doc.content_type }}</div>
            </div>
            <div v-if="doc.domain" class="item">
                <div class="label">Domain:</div>
                <div class="text">{{ doc.domain }}</div>
            </div>
            <div v-if="doc.url" class="item">
                <div class="label">Url:</div>
                <div class="text">{{ doc.url }}</div>
            </div>
            <div v-if="doc.score" class="item">
                <div class="label">Score:</div>
                <div class="text">{{ doc.score }}</div>
            </div>
            <template v-if="doc.highlights">   
                <div v-if="Object.keys(doc.highlights).length > 0" class="item">
                     <div class="label">Highlighted content:</div>
                     <div class="text" v-html="doc.highlights.content[0]"></div>
                </div>
            </template>             
            
            <!-- Full post -->
            <div class="item" @click="getFullpost(doc.id);toggleFullpost(doc.id)">
                <div class="link fullPost" :id="'fullpostLink_' + doc.id" >Show full post</div>
            </div>
            <div class="fullpost" v-bind:id="doc.id">
                <div v-if="fullpost">
                    <h3>Click value to perform a field search</h3>
                    <template v-for="(value, key) in fullpost[0]">
                        <div class="item">
                            <div class="label">{{ key }}</div>
                            <div v-if="value.constructor !== Array" class="text link" v-on:click="setupSearch('search', key + ':&quot;' + value + '&quot;');clearFacets()" >{{ value }}</div>
                            <div v-else >
                                <div v-for="item in value" v-on:click="setupSearch('search', key + ':&quot;' + item + '&quot;');clearFacets()" class="text link">{{item}}</div>
                            </div>
                        </div>
                    </template> 
                </div> 
                <div v-else>
                    <p>Retrieving data...</p>
                </div>  
            </div> 
            
            <!-- Download PDF's, Word docs etc. -->
            <div v-if="doc.content_type_norm && doc.content_type_norm != 'html' && doc.content_type_norm != 'other' && doc.content_type_norm != 'image'" class="item">
                <div class="download">
                    <a v-bind:href=" baseUrl + 'services/downloadRaw?source_file_path=' + doc.source_file_path + '&offset=' + doc.source_file_offset"  target="_blank">
                       Download {{ doc.content_type_norm }}
                    </a>
                </div>  
            </div>
             
            <!-- Images -->    
            <div v-if="doc.content_type_norm == 'image'" class="item">
                <div class="image">
                    <a v-bind:href=" baseUrl + 'services/downloadRaw?source_file_path=' + doc.source_file_path + '&offset=' + doc.source_file_offset" target="_blank">
                        <img v-bind:src=" baseUrl + 'services/downloadRaw?source_file_path=' + doc.source_file_path + '&offset=' + doc.source_file_offset"/>
                    </a>
                </div> 
                <span class="link" v-on:click="setupSearch('search', 'links_images:&quot;' + doc.url_norm + '&quot;');clearFacets()">Pages linking to image</span>
            </div>
              
            <!-- Images in HTML pages, Twitter Tweets and Jodel posts  -->  
            <div v-if="(doc.content_type_norm && doc.content_type_norm == 'html') || 
            (doc.type && doc.type == 'Twitter Tweet') || (doc.type && doc.type == 'Jodel Post')" class="item">               
                 <template v-for="(image, index) in imageObjects" v-if="doc.id == image.imageID">
                     <div class="thumbs" v-if="imageObjects[index].imageUrls.length > 0">
                         <template  v-for="(imageUrl, index) in image.imageUrls" >
                             <div class="thumb thumbSearch" v-bind:class="{ 'show': index < 10, 'hide extra': index >9 }">
                                 <a :href="image.downloadUrls[index]" target="_blank">
                                      <span v-html="imageUrl"></span> 
                                 </a>
                                 <br/>  
                                 <span class="link" v-on:click="setupSearch('search', 'hash:&quot;' + image.hashes[index] + '&quot;');clearFacets()">Search for image</span><br>
                                 <span class="link" v-on:click="setupSearch('search', 'links_images:&quot;' + image.urlNorm[index] + '&quot;');clearFacets()">Pages linking to image</span>
                             </div>
                             <div class="link moreThumbs" v-if="index == 9 && image.imageUrls.length > 10" onclick="$(this).nextAll().toggleClass('hide');$(this).toggleClass('active')"> thumbs</div>
                         </template>
                     </div> 
                 </template>
            </div>                                            
        </div>
    </div>    
    `,
    methods: {
        toggleFullpost: function(id){
            //console.log('toggle full post ID', id);
            if(document.getElementById(id).style.display === "block"){
                document.getElementById(id).style.display = "none";
                document.getElementById("fullpostLink_" + id).innerHTML = "Show full post";
            }else{
                $('.fullpost').hide();
                $('.link.fullPost').html("Show full post");
                document.getElementById(id).style.display = "block";
                document.getElementById("fullpostLink_" + id).innerHTML = "Hide full post";
            }
        }
    }
})

/* Component shows search result for images */
Vue.component('result-box-images', {
    props: ['searchResult','setupSearch','clearFacets'],
    template: `
    <div class="searchResults images">
        <div v-for="doc in searchResult" class="searchResultItem">
             <div class="thumb thumbImageSearch"><a v-bind:href="doc.downloadUrl" target="_blank"><img v-if="doc.imageUrl" v-bind:src="doc.imageUrl + '&height=200&width=200'"/></a></div>
             <div class="link" v-if="doc.imageUrl" v-on:click="setupSearch('search', 'hash:&quot;' + doc.hash + '&quot;');clearFacets()">Search for image</div>
             <span class="link" v-on:click="setupSearch('search', 'links_images:&quot;' + doc.urlNorm + '&quot;');clearFacets()">Pages linking to image</span>        
        </div>
    </div>    
    `
})

/* Component shows zero hits info */
Vue.component('zerohits-box', {
    props: ['myQuery','imageSearch'],
    template: `
    <div class="box">
        <p>Your <span v-if="imageSearch">image </span> search for <span class="bold">{{ myQuery }}</span> gave 0 results.</p>
    </div>
    `
})

/* Component shows errors  */
Vue.component('error-box', {
    props: ['errorMsg', 'myQuery'],
    template: `
    <div id="errorbox" class="box">
        <p>Your search for:<br> <span class="bold">{{ myQuery }}</span><br><br> 
        Gave following error: <br><span class="bold">{{errorMsg}}</span></p>
    </div>
    `
})

/* Router used to make history/back button work */
var router = new VueRouter({
    mode: 'history',
    routes: []
});

var app = new Vue({
    router,
    el: '#app',
    data: {
        searchResult: null,
        fullpost: null,
        googleMapLatitude: 0.0,
        googleMapLongitude: 0.0,
        googleMapRadius: 0.0,        
        allowExportWarc: true,
        myFacets: '',
        myQuery: '',
        facetFields: [],
        filters: '',
        totalHits: 0,
        totalHitsDuplicates: 0,
        start: 0,
        grouping: false,
        imageSearch: false,
        imageGeoSearch: false,
        urlSearch: false,
        spinner: false,
        errorMsg: '',
        imageObjects: [],
        baseUrl: '',
        openbaseUrl: null,
        markerPosition: {radius: 0, lat: "", lng: ""},
        geoImageInfo : [],
        resultMarkers: [],
        map:{}
    },
    watch: { //updating when route is changing to make history/back button work
        '$route' () {
           this.getQueryparams();
           this.setupUrl();
        }
    },
    created: function() { // getting applications base URL on creation
            this.$http.get( location.protocol +"//" + location.host +  "/solrwayback/services/properties/solrwaybackweb").then((response) => {
            console.log('properties response',response);
            this.baseUrl = response.body['wayback.baseurl'];
            this.openbaseUrl = response.body['openwayback.baseurl'];
            this.allowExportWarc =  ('true' == response.body['allow.export.warc']); 
            this.googleMapLatitude = response.body['google.maps.latitude'];
            this.googleMapLongitude= response.body['google.maps.longitude'];
            this.markerPosition.radius =  parseInt(response.body['google.maps.radius']);                                                        
            this.leafletSource = response.body['leaflet.source'];
            this.leafletAttribution = response.body['leaflet.attribution'];
        }, (response) => {
            console.log('error: ', response);
            this.errorMsg = response.statusText;
            this.hideSpinner();
        });
        this.getQueryparams();
        this.setupUrl();
    },
    methods: {
        /* Method to toggle grouping. Quick fix instead of adding it to the already way too long list of parameters
         * to the setupSearch() method below.  */
        setGrouping: function(grouping){
            this.grouping = grouping;
        },

        /* Setting up search. Checking if it's an ordinary search, URL search, image search, paging, facet delimit */
        setupSearch: function(type, query, param3, param4, imagegeosearch) {
            if (type == "search") {
                this.filters = ''; //resetting filters on new search
                this.myQuery = query;
                this.start = 0;
                //console.log("type, query, param3, param4, imagegeosearch:", type, query, param3, param4, imagegeosearch)
                if (param3) {
                    this.urlSearch = true;
                    this.imageSearch = false; // deselecting image search when URL search
                    this.imageGeoSearch = false;
                }else if (param4) {
                    this.imageSearch = true;
                    this.urlSearch = false;
                    if (imagegeosearch) {
                        this.imageGeoSearch = imagegeosearch;
                    } else {
                        this.imageGeoSearch = false;
                    }
                }else {
                    this.imageSearch = false;
                    this.urlSearch = false;
                    this.imageGeoSearch = false;
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
                        // Quotes removed from filters to avoid double quotes before adding them again to handle spaces.
                        this.filters = this.filters + key + '%3A' + '"' + this.facetFields[i][key].replace(/"/g,"") + '"'
                    }
                }
                this.start = 0; //resetting pager
            }

            router.push({
                query: {
                    query: this.myQuery,
                    start: parseInt(this.start),
                    filter: this.filters,
                    imgsearch: this.imageSearch,
                    imggeosearch: this.imageGeoSearch,
                    grouping: this.grouping,
                    //urlsearch: this.urlSearch,
                }
            });
        },

        setupUrl: function() {
            this.imageObjects = []; //resetting imageObjecs on new search
            this.searchResult = []; //resetting search result on new search
            if (this.urlSearch && this.myQuery) {
                var tempUrl =  location.protocol +'//' + location.host + '/solrwayback/services/util/normalizeurl?url='
                    + encodeURI(encodeURI(this.myQuery.trim()));
                this.showSpinner();
                this.$http.get(tempUrl).then((response) => {
                    var url_norm = response.body.url;
                    this.searchUrl =  location.protocol +'//' + location.host + '/solrwayback/services/solr/search?query=url_norm:"' +
                        url_norm + '"&start=' + parseInt(this.start) + '&fq=' + this.filters;
                    this.doSearch();

                }, (response) => {
                    console.log('error: ', response);
                });
                return; // returning if URL search, because we're waiting for service to generate URL
            } else if (this.imageSearch && !this.imageGeoSearch) {
                this.searchUrl =  location.protocol +'//' + location.host + '/solrwayback/services/images/search?query=' + this.myQuery +
                    '&start=' + this.start + '&fq=' + this.filters;
            } else if (this.imageGeoSearch) {
                if (!this.markerPosition.lat || !this.markerPosition.lng) {
                    return //leaving search if latítude or longitude isn't set
                }
                this.searchUrl =  location.protocol +'//' + location.host + '/solrwayback/services/images/search/location?query=' + this.myQuery +
                    //'&latitude=' + this.latitude + '&longitude=' + this.longitude + '&d=' + this.markerPosition.radius / 1000;
                    '&latitude=' + this.markerPosition.lat + '&longitude=' + this.markerPosition.lng + '&d=' + this.markerPosition.radius / 1000;
            } else {
                this.searchUrl =  location.protocol +'//' + location.host + '/solrwayback/services/solr/search?query=' + this.myQuery +
                    '&start=' + parseInt(this.start) + '&fq=' + this.filters + "&grouping=" + this.grouping;
            }
            this.facetFields = []; //resetting facet fields before building them from query params
            if (this.filters) {
                var facetPairs = this.filters.split('%20AND%20');
                facetFieldsTemp = [];
                for (var i = 0; i < facetPairs.length; i++) {
                    facetFieldsTemp = facetPairs[i].split('%3A');
                    var tempObj = {[facetFieldsTemp[0]]: facetFieldsTemp[1]}; //Facet field and facet term saved in object and pushed to array
                    this.facetFields.push(tempObj);
                }
            }
            this.doSearch();
        },

        doSearch: function(){
            /* Starting search if there's a query using the search URL set up above */
            if(this.myQuery && this.myQuery.trim() != ''){
                this.showSpinner();
                //console.log('this.searchUrl: ', this.searchUrl);
                this.$http.get(this.searchUrl).then((response) => {
                    this.errorMsg = "";
                    console.log('response.body: ', response.body);
                    if(response.body.error){
                        this.errorMsg = response.body.error.msg;
                        this.hideSpinner();
                        return;
                    }
                    if(!this.imageSearch){
                        if(response.body.grouped){
                            this.searchResult = response.body.grouped.url.doclist.docs;
                            this.totalHits = response.body.stats.stats_fields.url.cardinality;
                            this.totalHitsDuplicates = response.body.grouped.url.matches;
                        }else{
                            this.searchResult = response.body.response.docs;
                            this.totalHits = response.body.response.numFound;
                        }
                        if(response.body.highlighting){
                            var highlights = response.body.highlighting;
                        }

                        /* Nyt objektet med image URL'er ved content type HTML, Twitter og Jodel */
                        for(var i=0; i<this.searchResult.length;i++){
                            if((this.searchResult[i].content_type_norm && this.searchResult[i].content_type_norm == 'html') ||
                                (this.searchResult[i].type && this.searchResult[i].type == 'Twitter Tweet') ||
                                (this.searchResult[i].type && this.searchResult[i].type == 'Jodel Post')){
                            	this.getImages(this.searchResult[i].id,this.searchResult[i].source_file_path, this.searchResult[i].source_file_offset);
                            }

                            /* Adding property highlight to search result object */
                            for (var key in highlights){
                                if(this.searchResult[i].id === key){
                                    this.searchResult[i].highlights = highlights[key];
                                }
                            }
                        }
                        this.myFacets=response.body.facet_counts.facet_fields;
                    }else{
                        this.geoImageInfo = []; // Resetting image positions array
                        this.searchResult = response.body;
                        this.totalHits = this.searchResult.length;
                        if(this.imageGeoSearch){
                            var _this = this
                            this.searchResult.forEach(function(item, index) {
                                var imageInfo = {
                                    lat: item.latitude,
                                    lng: item.longitude,
                                    downloadUrl: item.downloadUrl,
                                    resourceName: item.resourceName
                                };
                                _this.geoImageInfo.push(imageInfo);
                            });
                            this.setResultMarkers();
                        }
                    }
                    $("html, body").animate({ scrollTop: 0 }, "fast");
                    this.hideSpinner();
                }, (response) => {
                    console.log('error: ', response);
                    this.errorMsg = response.statusText;
                    this.hideSpinner();
                });
            }
        },

        getFullpost: function(id){
            var fullpostUrl =  location.protocol +'//'+ location.host + '/solrwayback/services/solr/idlookup?id=' + encodeURIComponent(id);
            this.$http.get(fullpostUrl).then((response) => {
                this.fullpost = response.body.response.docs;
            }, (response) => {
                console.log('error: ', response);
            });
        },

        getImages: function(id,source_file_path, offset){
            var imageInfoUrl =  location.protocol +"//" +location.host + "/solrwayback/services/images/htmlpage?source_file_path=" + source_file_path +"&offset="+offset;
            this.$http.get(imageInfoUrl).then((response) => {
                var imageUrl = ""; // Url in the netarchive
                var downloadUrl = ""; // Url in the netarchive
                var hash = "";
                var urlNorm = ""; // Url in real life
                var imageUrlArray = [];
                var downloadArray = [];
                var hashArray = [];
                var urlNormArray = [];
                for(var j=0;j<response.body.length;j++){
                    imageUrl = response.body[j].imageUrl;
                    downloadUrl = response.body[j].downloadUrl;
                    hash = response.body[j].hash;
                    urlNorm = response.body[j].urlNorm;
                    var imageHTML = '<img src="' + imageUrl + '&width=100&height=100">'
                    imageUrlArray.push(imageHTML);
                    downloadArray.push(downloadUrl);
                    hashArray.push(hash);
                    urlNormArray.push(urlNorm);
                }
                this.imageObjects.push({
                    imageID: id,
                    imageUrls: imageUrlArray,
                    downloadUrls: downloadArray,
                    hashes: hashArray,
                    urlNorm: urlNormArray
                });
            }, (response) => {
                console.log('error: ', response);
                this.hideSpinner();
            });
        },

        /* Method used on creation, reload and route change to get query parameters */
        getQueryparams:function(){
            this.myQuery = this.$route.query.query;
            this.start= this.$route.query.start;
            this.filters = this.$route.query.filter;
            this.imageSearch = this.$route.query.imgsearch;
            this.imageGeoSearch = this.$route.query.imggeosearch;
            this.grouping = this.$route.query.grouping;
            //converting possible string value from query param to boolean
            if(!this.imageSearch || this.imageSearch == 'false' ){
                this.imageSearch = false
            }else{
                this.imageSearch = true
            }
            if(!this.imageGeoSearch || this.imageGeoSearch == 'false' ){
                this.imageGeoSearch = false
            }else{
                this.imageGeoSearch = true
            }
            if(!this.grouping || this.grouping == 'false' ){
                this.grouping = false
            }else{
                this.grouping = true
            }
            if(!this.urlSearch || this.urlSearch == 'false' ){
                this.urlSearch = false
            }else{
                this.urlSearch = true
            }
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


 
        /* leaflet.js with openstreetmap */
        /* creates a marker and a marker circle on the screen and saves them */
        /* so later they are easier to remove */
        placeMarker: function(position, map, markers, markerCircles, radius){
            this.map = map;
            var position = position;
            for (var i = 0; i < markers.length; i++) { //deleting previous markers and circles
                markers[i].remove();
                markerCircles[i].remove();
            }
            
            
            var marker = L.popup().setLatLng(position).setContent("Center of your search").openOn(map);
                        
            var markerCircle = L.circle(position, {
                radius: radius*1000,
                color: '#00cc00',
                opacity: 0.8,
                weight: 2,
                fillColor: '#00cc00',
                fillOpacity: 0.35
                }).addTo(map);
             
           
            var _this = this;
            
            markerCircle.on('click',function(e){
                var newPosition = e.latLng;
                _this.placeMarker(newPosition, map, markers, markerCircles, radius);
                return;
            });
            
            
            markers.push(marker);
            markerCircles.push( markerCircle);
            /* Building marker info object */
            this.markerPosition = {
                radius: radius*1000,
                lat: marker.getLatLng().lat,
                lng: marker.getLatLng().lng,
            };
            
            //OSM works the same way as google map
            map.panTo(position);
            this.setupUrl();
        },
           
           /* a simple leaflet.js popup is really near in functionality */

        setResultMarkers: function(){
            for (var i = 0; i < this.resultMarkers.length; i++) { //deleting previous markers and circles
                this.resultMarkers[i].remove();
            }
            this.resultMarkers = [];
            for (var i = 0; i < this.geoImageInfo.length; i++) {
                var item = this.geoImageInfo[i]
                var latLng = [item.lat,item.lng];
                var _this = this;

                var marker = L.marker(latLng).addTo(_this.map);
                //marker.SWtitle = item.resourceName;
                marker.SWinfo = "<p class='imageNameHover' title='" + item.resourceName + "'>" + item.resourceName + "</p>" +
                            "<a href='"+ item.downloadUrl + "' target='_blank'>" +
                                "<img class='mapsHoverImage' src='" + item.downloadUrl + "'>" +
                            "</a>";

                marker.SWdownloadUrl=item.downloadUrl;
                
                marker.on('click', function() {
                    window.open(this.SWdownloadUrl, '_blank');
                });
                
                marker.on('mouseover',function(){
                    //L.popup().setLatLng(this.getLatLng()).setContent(this.SWinfo).openOn(map);
                    marker.bindPopup(this.SWinfo).openPopup(this.getLatLng());
                });
                
                
                this.resultMarkers.push(marker);
            }

        }
    }
})

