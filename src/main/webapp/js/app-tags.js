Vue.filter('thousandsSeperator', function(value) {
    if (value === 0) return 0; // to keep zero's in table
    if (!value) return '';
    var newValue = value.toLocaleString();
    return newValue;
})

Vue.component('header-container', {
    props: ['addTag','tags',"removeTag","errorMsg"],
    template: `
    <div id="headerTags">
        <a class="backToSearch" href="./">Back to SOLR Wayback</a>
        <h1>Search the Netarchive for HTML tags</h1>
        <div id="tagSearchBox">
            <search-box :add-tag="addTag"></search-box>
            <tags-box :tags="tags" :remove-tag="removeTag"></tags-box>
        </div>
        <error-box v-if="errorMsg" :error-msg="errorMsg"></error-box>
    </div>    
    `,
})

Vue.component('search-box', {
    props: ["addTag"],
    data: function(){
        return{
            tagModel: '',
        }
    },
    template: `
    <div id="tagSearch">
        <input  v-model="tagModel" @keyup.enter="addTag(tagModel)" placeholder="eg. h1;h2;h3;h4" type="text">
        <button  @click="addTag(tagModel)">Search</button>
        <p>Search for up to 4 tags seperated by semi colon.</p>  
    </div>    
    `,
})

Vue.component('tags-box', {
    props: ["tags","removeTag"],
    template: `
    <div id="tagsList" v-if="tags.length > 0">
        <ul class="removeTags"> 
            <li class="removeTags">Searched tags:</li>
            <li v-for="tag in tags" @click="removeTag(tag)" class="removeTags"><span class="link">{{ tag }}</span>,</li> 
            <li class="removeTags">(Click tag to remove it from your search)</li>
        </ul> 
    </div>    
    `,
})

Vue.component('error-box', {
    props: ['errorMsg'],
    template: `
    <div id="errorbox" class="box">
        <p>Your search gave an error: <span class="bold">{{errorMsg}}</span></p>
    </div>
    `
})

Vue.component('chart-container', {
    props: ["sizeInKb","chartLabels"],
    template: `
    <div id="chart">
        <canvas id="line-chart" width="800" height="450"></canvas>    
    </div>    
    `,
})

Vue.component('table-container', {
    props: ["dataArrays"],
    template: `
    <div id="tagTableContainer" v-if="dataArrays.length > 0">
        <h2>Results in raw numbers</h2>
        <template v-for="dataset in dataArrays">
        <h3>Tag: {{ dataset.searchedTag }}</h3>
        <table class="tagTable" >
            <thead>
                <tr>
                    <th></th>
                    <th v-for="item in dataset.yearCountsTotal ">{{ item.year }}</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>Percent</td>
                    <td v-for="item in dataset.yearCountPercent" class="data">{{ Math.round( item * 10000)/100 }}%</td>
                </tr>
                <tr>
                    <td>Count</td>
                    <td v-for="item in dataset.yearCountsTotal" class="data">{{ item.count | thousandsSeperator  }}</td>
                </tr>
                <tr>
                    <td>Total pages</td>
                    <td v-for="item in dataset.yearCountsTotal" class="data">{{item.total | thousandsSeperator }}</td>
                </tr>
            </tbody>
        </table>
        </template>
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
        dataArrays: [],
        tags: [],
        chartLabels: [],
        errorMsg: "",
    },
    methods: {
        addTag: function(tag) {
            //var tagArray = tag.split(";").join('').split(''); //removing empty items if user is sloppy with the seperators
            var tagArray = tag.split(";");
            if(tagArray.length > 4){
                tagArray.length = 4;
            }
            if (this.tags.length + tagArray.length > 4) {
                this.tags.length = 4 - tagArray.length;
            }
            for( var i=0;i<tagArray.length; i++){
                tagArray[i] = tagArray[i].replace("<", "").replace(">", "").toLowerCase().trim();
                if(tagArray[i] != ''){
                    this.tags.push(tagArray[i]);
                }
            }
            if(this.tags.length > 0){
                this.getData();
            }
        },

        getData: function(){
            this.showSpinner();
            var promises = [];
            for( var i = 0; i < this.tags.length; i++ ){
                var tagsUrl = 'http://' + location.host + '/solrwayback/services/smurf/tags?tag=' + this.tags[i] + "&startyear=2006";
                promises.push(this.$http.get(tagsUrl));
            }
            Promise.all(promises).then((response) => {
                    this.errorMsg = "";
                    this.chartLabels = []; // Resetting data arrays
                    this.dataArrays = []; // Resetting data arrays
                    for(var i = 0; i < response.length; i++){
                        var tempPercents = []; // Resetting temp array
                        this.dataArrays.push(response[i].body);
                        for(var j = 0; j < this.dataArrays[i].yearCountPercent.length; j++){
                            tempPercents.push(Math.round(this.dataArrays[i].yearCountPercent[j] * 10000)/100); // recalculating to percents
                        }
                        this.dataArrays[i].yearPercent = tempPercents; // Real percents are added to data objects
                        this.dataArrays[i].searchedTag = this.tags[i];
                    }

                    // Setting chart labels (years in chart) based on the first search
                    for(var i = 0; i < this.dataArrays[0].yearCountsTotal.length; i++){
                        this.chartLabels.push(this.dataArrays[0].yearCountsTotal[i].year);
                    }
                    console.log('response: ', response);
                    this.drawChart();
                    this.hideSpinner();
                }, (response) => {
                    console.log('error: ', response);
                    this.errorMsg = response.body;
                    this.hideSpinner();
                });
        },

        drawChart: function(){
            var chartData = {
                type: 'line',
                data: {
                    labels: this.chartLabels,
                },
                options: {
                    title: {
                        display: true,
                    },
                    scales: {
                        yAxes: [
                            {
                                scaleLabel: {
                                    display: true,
                                    labelString: 'Use in percentage',
                                    fontColor: "#0066cc",
                                }
                            }
                        ]
                    },
                    legend: {
                        labels: {
                            fontColor: 'black',
                        },
                        onClick: function(event, legendItem) {
                            var index = legendItem.datasetIndex;
                            //toggle the datasets visibility
                            tagsChart.data.datasets[index].hidden = !tagsChart.data.datasets[index].hidden;
                            //toggle the related labels' visibility
                            //tagsChart.options.scales.yAxes[index].display = !tagsChart.options.scales.yAxes[index].display;
                            tagsChart.update();
                        }
                    }
                }
            };
            var datasets =  [];
            var borderColors = ["#0066cc","#00cc66","#cc0066","#cc6600"];
            for (var i = 0; i < this.dataArrays.length; i++){
                var datasetTemp = {
                    data: this.dataArrays[i].yearPercent,
                    label: this.tags[i],
                    borderColor: borderColors[i],
                    fill: false,
                }
                datasets.push(datasetTemp)
                console.log('datasets', datasets);
            }
            chartData.data.datasets = datasets;

            var tagsChart = new Chart(document.getElementById("line-chart"), chartData);
        },

        removeTag: function(tag){
            var index = this.tags.indexOf(tag);
            if (index > -1) {
                this.tags.splice(index, 1);
            }
            if(this.tags.length >0 ){
                this.getData() //get data if tags in array
            }else{
                var canvas = '<canvas id="line-chart" width="800" height="450"></canvas>'
                $("#chart").html(canvas);//insert clean canvas if tags is empty
                this.errorMsg = "";
            }

        },

        showSpinner: function(){
            this.spinner = true;
        },

        hideSpinner: function(){
            this.spinner = false;
        },
    }
})


