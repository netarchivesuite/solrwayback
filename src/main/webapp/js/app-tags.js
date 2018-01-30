Vue.filter('thousandsSeperator', function(value) {
    if (value === 0) return 0; // to keep zero's in table
    if (!value) return '';
    var newValue = value.toLocaleString();
    return newValue;
})

Vue.component('header-container', {
    props: ['getData'],
    template: `
    <div id="headerTags">
        <h1>Search for HTML tags</h1>
        <search-box :get-data="getData"></search-box>
    </div>    
    `,
})

Vue.component('search-box', {
    props: ["getData"],
    data: function(){
        return{
            tagModel: '',
        }
    },
    template: `
    <div id="tagSearch">
        <input  v-model="tagModel" @keyup.enter="getData(tagModel)" type="text">
        <button  @click="getData(tagModel)">Go</button>  
    </div>    
    `,
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
    props: ["rawData"],
    template: `
    <div id="domainGrowthTableContainer">
        <table id="domainGrowthTable">
            <thead>
                <tr>
                    <th></th>
                    <th v-for="item in rawData" v-if="item.total > 0">{{ item.year }}</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>Count</td>
                    <td v-for="item in rawData" v-if="item.total > 0">{{ item.count | thousandsSeperator }}</td>
                </tr>
                <tr>
                    <td>Total pages</td>
                    <td v-for="item in rawData" v-if="item.total > 0">{{item.total | thousandsSeperator }}</td>
                </tr>
            </tbody>
        </table>
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
        rawData: [],
        tags: [],
        chartLabels: [],
        tag1: [],
        numberOfPages: [],
        hasResults: true,
    },
    methods: {
        getData: function(tag){
            this.showSpinner();

            this.tags.push(tag);
            if(this.tags.length > 2){
                this.tags.shift();
            }
            console.log('this.tags', this.tags);
            var promises = [];
            for( var i = 0; i < this.tags.length; i++ ){
                var tagsUrl = 'http://' + location.host + '/solrwayback/services/smurf/tags?tag=' + this.tags[i];
                promises.push(this.$http.get(tagsUrl));
                console.log('tagsUrl', tagsUrl)
            }
            console.log('PROMISES   ', promises)
            Promise.all(promises).then((response) => {
                    this.errorMsg = "";
                    this.chartLabels = []; // Resetting data arrays
                    this.tag1 = []; // Resetting data arrays
                    this.tag2 = []; // Resetting data arrays
                    this.rawData = response[0].body;
                    if(response[1]){
                        this.rawData2 = response[1].body;
                        for(var i = 0; i < this.rawData2.yearCountPercent.length; i++){
                            this.tag2.push(this.rawData2.yearCountPercent[i] * 100);
                        }
                    }


                    for(var i = 0; i < this.rawData.yearCountsTotal.length; i++){
                        this.chartLabels.push(this.rawData.yearCountsTotal[i].year);
                       /*this.ingoingLinks.push(this.rawData[i].ingoingLinks);
                        this.numberOfPages.push(this.rawData[i].totalPages);*/
                    }
                    for(var i = 0; i < this.rawData.yearCountPercent.length; i++){
                        this.tag1.push(this.rawData.yearCountPercent[i] * 100);
                    }
                    console.log('response: ', response);
                    console.log('this.rawData: ', this.rawData);
                    this.drawChart();
                    this.hideSpinner();
                    /*if(response.body.error){
                        this.errorMsg = response.body.error.msg;
                        return;
                    }*/
                }, (response) => {
                    console.log('error: ', response);
                    this.hideSpinner();
                });

            /*for( var i = 0; i < this.tags.length; i++ ){
                var tagsUrl = 'http://' + location.host + '/solrwayback/services/smurf/tags?tag=' + this.tags[i];
                console.log('tagsUrl', tagsUrl)

                this.$http.get(tagsUrl).then((response) => {
                    this.errorMsg = "";
                    this.chartLabels = []; // Resetting data arrays
                    this.tag1 = []; // Resetting data arrays
                    this.tag2 = []; // Resetting data arrays
                    this.rawData = response.body;
                    for(var i = 0; i < this.rawData.yearCountsTotal.length; i++){
                        this.chartLabels.push(this.rawData.yearCountsTotal[i].year);
                    }
                    for(var i = 0; i < this.rawData.yearCountPercent.length; i++){
                        this.tag1.push(this.rawData.yearCountPercent[i] * 100);
                    }
                    console.log('response.body: ', response.body);
                    console.log('this.rawData: ', this.rawData);
                    this.drawChart();
                    this.hideSpinner();
                    if(response.body.error){
                        this.errorMsg = response.body.error.msg;
                        return;
                    }
                }, (response) => {
                    console.log('error: ', response);
                    this.hideSpinner();
                });
            }*/
        },

        drawChart: function(){
            var domainGrowthChart = new Chart(document.getElementById("line-chart"), {
                type: 'line',
                data: {
                    labels: this.chartLabels,
                    datasets: [
                        {
                            data: this.tag1,
                            label: this.tags[0],
                            borderColor: "#0066cc",
                            fill: false,
                        },
                        {
                            data: this.tag2,
                            label: this.tags[1],
                            borderColor: "#00cc66",
                            fill: false,
                        }
                    ]
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
                            domainGrowthChart.data.datasets[index].hidden = !domainGrowthChart.data.datasets[index].hidden;
                            //toggle the related labels' visibility
                            domainGrowthChart.options.scales.yAxes[index].display = !domainGrowthChart.options.scales.yAxes[index].display;
                            domainGrowthChart.update();
                        }
                    }
                }
            });
        },

        showSpinner: function(){
            this.spinner = true;
        },

        hideSpinner: function(){
            this.spinner = false;
        },
    }
})


