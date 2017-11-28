Vue.component('header-container', {
    props: ['url'],
    template: `
    <div id="header">
        <h1>Domain developement for: <span  v-if="url">{{ url }}</span></h1>
    </div>    
    `,
})

Vue.component('chart-container', {
    props: ["chartData","chartLabels"],
    template: `
    <div id="chart">
        <div>Chart data: {{ chartData }}</div>
        <div>Labels: {{ chartLabels }}</div>
        <canvas id="line-chart" width="800" height="450"></canvas>    
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
        url: '',
        spinner: false,
        chartData: [],
        chartLabels: [],
        incomingLinks: [],
        numberOfPages: []
    },
    methods: {
        getData: function(){
            this.url = this.$route.query.url
            if(this.url){
                this.showSpinner();
                this.url = this.url.replace(/http.*:\/\//i,""); //Get domain from URL, using replace and regex to trim domain
                if( this.url.slice(-1) === "/"){ // if trailing slash on url it's removed
                    this.url = this.url.slice(0, -1)
                }
                var searchUrl = 'http://' + location.host + '/solrwayback/services/solr/search?query=*:*&start=0&fq=domain%3A' + this.url;
                var domainGrowthUrl = 'http://' + location.host + '/solrwayback/services/statistics/domain?domain=' + this.url;
                //var domainGrowthUrl = http://localhost:8080/solrwayback/services/statistics/domain?domain=dr.dk

                this.$http.get(domainGrowthUrl).then((response) => {
                    this.hideSpinner();
                    this.errorMsg = "";
                    /*
                     var tempData = response.body.facet_counts.facet_fields.crawl_year;
                     for(var i = 0; i < tempData.length; i++){
                     if(i % 2 == 1){
                     this.chartData.push(tempData[i])
                     }else{
                     this.chartLabels.push(tempData[i])
                     }
                     }*/
                    var tempData = {
                        2006:{
                            links : 9,
                            size: 456,
                            pages: 28
                        },
                        2007:{
                            links : 57,
                            size: 1456,
                            pages: 72
                        },
                        2008:{
                            links : 107,
                            size: 2256,
                            pages: 73
                        },
                        2009:{
                            links : 729,
                            size: 4568,
                            pages: 87
                        }
                    };
                    for( key in tempData){
                        console.log(key)
                        this.chartLabels.push(key);
                        this.chartData.push(tempData[key].size);
                        this.incomingLinks.push(tempData[key].links);
                        this.numberOfPages.push(tempData[key].pages);
                    }
                    console.log('this.chartLabels',  this.chartLabels)
                    console.log('this.chartData',  this.chartData)

                    /* Calculating max values for y-axis
                     this.linksMax = Math.max(...this.incomingLinks);
                     this.linksMax = Math.ceil(this.linksMax / 100) * 100;
                     this.pagesMax = Math.max(...this.numberOfPages);
                     this.pagesMax = Math.ceil(this.pagesMax / 10) * 10;
                     console.log('this.linksMax: ', this.linksMax);
                     console.log('this.pagesMax: ', this.pagesMax);
                     **/


                    console.log('tempData: ', tempData);
                    console.log('response [Service delivered bogus data]: ', response);
                    console.log('this.chartData: ', this.chartData);
                    this.drawChart();
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
            console.log('document.getElementById("line-chart', document.getElementById("line-chart"));
            var domainGrowthChart = new Chart(document.getElementById("line-chart"), {
                type: 'bar',
                data: {
                    labels: this.chartLabels,
                    datasets: [
                        {
                            data: this.chartData,
                            label: "Number of crawls",
                            yAxisID: 'A',
                            borderColor: "#0066cc",
                            type: "line",
                            fill: false
                        },
                        {
                            data: this.numberOfPages,
                            label: "Number of pages",
                            yAxisID: 'C',
                            type: "line",
                            borderColor: "#cc0000",
                            fill: false
                        },
                        {
                            data: this.incomingLinks,
                            label: "Incoming links",
                            yAxisID: 'B',
                            type: "bar",
                            backgroundColor: "#009900"
                        }
                    ]
                },
                options: {
                    title: {
                        display: true,
                        text: 'Developement of domain: ' + this.url,
                    },
                    scales: {
                        xAxes: [{
                            barPercentage: 0.2
                        }],
                        yAxes: [
                            {
                                id: 'A',
                                scaleLabel: {
                                    display: true,
                                    labelString: 'Number of harvests',
                                    fontColor: "#0066cc",
                                },
                            },
                            {
                                id: 'C',
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
                                id: 'B',
                                scaleLabel: {
                                    display: true,
                                    labelString: 'Incoming links',
                                    fontColor: "#009900",
                                },
                                gridLines : {
                                    display : false
                                }
                            }
                        ]
                    },
                    legend: {
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

