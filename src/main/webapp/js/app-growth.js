Vue.filter('thousandsSeperator', function(value) {
    if (value === 0) return 0;
    if (!value) return '';
    var newValue = value.toLocaleString();
    return newValue;
})

Vue.component('header-container', {
    props: ['domain','getData','hasResults'],
    template: `
    <div id="headerDomainGrowth">
        <h1>Domain developement for: <span  v-if="domain">{{ domain }}</span></h1>
        <search-box :domain="domain" :get-data="getData"></search-box>
        <div id="nohitsDomain" v-if="!hasResults" class="box">
            <p>No results in the Netarchive for domain: <strong>{{ domain }}</strong></strong></p>
        </div>
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
        <table id="domainGrowthTable" v-if="rawData.length > 0">
            <thead>
                <tr>
                    <th></th>
                    <th v-for="item in rawData">{{ item['year'] }}</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>Ingoing links</td>
                    <td v-for="item in rawData">{{ item['ingoingLinks'] | thousandsSeperator }}</td>
                </tr>
                <tr>
                    <td>Total pages</td>
                    <td v-for="item in rawData">{{item['totalPages'] | thousandsSeperator }}</td>
                </tr>
                <tr>
                    <td>Size in KB</td>
                    <td v-for="item in rawData">{{ item['sizeInKb'] | thousandsSeperator }}</td>
                </tr>
            </tbody>
        </table>
    </div>    
    `,
})

Vue.component('search-box', {
    props: ["domain", "getData"],
    data: function(){
        return{
            domainModel: this.domain,
        }
    },
    template: `
    <div id="domainSearch">
        <input id="domainGrowth" v-model="domainModel" @keyup.enter="startSearch()" type="text">
        <button id="domainGrowth" @click="startSearch()">Check domain</button>  
    </div>    
    `,
    methods: {
        startSearch: function() {
            router.push({
                query: {
                    domain: this.domainModel
                }
            });
            this.getData();
        }
    }
})

var router = new VueRouter({
    mode: 'history',
    routes: []
});

var app = new Vue({
    router,
    el: '#app',
    data: {
        domain: '',
        spinner: false,
        rawData: [],
        sizeInKb: [],
        chartLabels: [],
        ingoingLinks: [],
        numberOfPages: [],
        hasResults: true,
    },
    methods: {
        getData: function(){
            this.domain = this.$route.query.domain;
            if(this.domain){
                this.showSpinner();
                this.hasResults = true;
                this.domain = this.domain.replace(/http.*:\/\//i,""); //Get domain from URL, using replace and regex to trim domain
                if( this.domain.slice(-1) === "/"){ // if trailing slash on domain it's removed
                    this.domain = this.domain.slice(0, -1)
                }

                var domainGrowthUrl = 'http://' + location.host + '/solrwayback/services/statistics/domain?domain=' + this.domain;

                this.$http.get(domainGrowthUrl).then((response) => {
                    this.errorMsg = "";
                    this.chartLabels = []; // Resetting data arrays
                    this.sizeInKb = [];
                    this.ingoingLinks = [];
                    this.numberOfPages = [];
                    this.rawData = response.body;
                    for(var i = 0; i < this.rawData.length; i++){
                        this.chartLabels.push(this.rawData[i].year);
                        this.sizeInKb.push(this.rawData[i].sizeInKb);
                        this.ingoingLinks.push(this.rawData[i].ingoingLinks);
                        this.numberOfPages.push(this.rawData[i].totalPages);
                    }
                    // local vars used to check if there's results on the chosen domain
                    var sumSize = 0, sumLinks = 0, sumPages = 0;
                    for(var i in this.sizeInKb) { sumSize += this.sizeInKb[i]; }
                    for(var i in this.ingoingLinks) { sumLinks += this.ingoingLinks[i]; }
                    for(var i in this.numberOfPages) { sumPages += this.numberOfPages[i]; }
                    if(sumSize == 0 && sumLinks == 0 && sumPages == 0){
                        this.hasResults = false;
                        $("#line-chart, #domainGrowthTableContainer").hide();
                    }else{
                        this.hasResults = true;
                        $("#line-chart, #domainGrowthTableContainer").show();
                    }
                    console.log('response.body: ', response.body);
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
            }
        },

        drawChart: function(){
            var domainGrowthChart = new Chart(document.getElementById("line-chart"), {
                type: 'line',
                data: {
                    labels: this.chartLabels,
                    datasets: [
                        {
                            data: this.sizeInKb,
                            label: "Size in Kilobytes",
                            yAxisID: 'kilobytes',
                            borderColor: "#0066cc",
                            fill: false,
                        },
                        {
                            data: this.numberOfPages,
                            label: "Number of pages",
                            yAxisID: 'totalpages',
                            borderColor: "#cc0000",
                            fill: false
                        },
                        {
                            data: this.ingoingLinks,
                            label: "Incoming links",
                            yAxisID: 'links',
                            borderColor: "#009900",
                            fill: false
                        }
                    ]
                },
                options: {
                    title: {
                        display: true,
                    },
                    scales: {
                        xAxes: [{
                            barPercentage: 0.2
                        }],
                        yAxes: [
                            {
                                id: 'kilobytes',
                                scaleLabel: {
                                    display: true,
                                    labelString: 'Size in kilobytes',
                                    fontColor: "#0066cc",
                                },
                            },
                            {
                                id: 'totalpages',
                                scaleLabel: {
                                    display: true,
                                    labelString: 'Pages',
                                    fontColor: "#cc0000",
                                },
                                gridLines : {
                                    display : false
                                }
                            },
                            {
                                id: 'links',
                                scaleLabel: {
                                    display: true,
                                    labelString: 'Ingoing links',
                                    fontColor: "#009900",
                                },
                                gridLines : {
                                    display : false
                                }
                            }
                        ]
                    },
                    legend: {
                        labels: {
                            //usePointStyle: true,
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
    },
    created: function() {
        this.getData();
    }
})

/* Some resources for the project:
 *  Simple startup: http://tobiasahlin.com/blog/chartjs-charts-to-get-you-started/
 *  Hide y-axis and dataset: https://stackoverflow.com/questions/40006970/hide-y-axis-labels-when-data-is-not-displayed-in-chart-js
 *
 *
 *
 *
 *
 *
 *
 * */

