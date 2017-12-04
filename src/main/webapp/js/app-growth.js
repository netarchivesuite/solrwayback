Vue.component('header-container', {
    props: ['domain'],
    template: `
    <div id="header">
        <h1>Domain developement for: <span  v-if="domain">{{ domain }}</span></h1>
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
        sizeInKb: [],
        chartLabels: [],
        ingoingLinks: [],
        numberOfPages: []
    },
    methods: {
        getData: function(){
            this.domain = this.$route.query.domain
            if(this.domain){
                this.showSpinner();
                this.domain = this.domain.replace(/http.*:\/\//i,""); //Get domain from URL, using replace and regex to trim domain
                if( this.domain.slice(-1) === "/"){ // if trailing slash on domain it's removed
                    this.domain = this.domain.slice(0, -1)
                }

                var domainGrowthUrl = 'http://' + location.host + '/solrwayback/services/statistics/domain?domain=' + this.domain;

                this.$http.get(domainGrowthUrl).then((response) => {
                    this.hideSpinner();
                    this.errorMsg = "";
                    var tempData = response.body;
                    for(var i = 0; i < tempData.length; i++){
                        this.chartLabels.push(tempData[i].year);
                        this.sizeInKb.push(tempData[i].sizeInKb);
                        this.ingoingLinks.push(tempData[i].ingoingLinks);
                        this.numberOfPages.push(tempData[i].totalPages);
                    }

                    console.log('response.body: ', response.body);

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
                            fill: false
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
                        //text: 'Developement of domain: ' + this.domain,
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

