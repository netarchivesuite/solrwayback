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

                this.$http.get(searchUrl).then((response) => {
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
                            links : 425,
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

                    console.log('tempData: ', tempData);
                    //console.log('response: ', response);
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
            new Chart(document.getElementById("line-chart"), {
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
                        }/*,
                        {
                            data: this.chartData,
                            label: "Number of crawls",
                            yAxisID: 'A',
                            type: "bar",
                            backgroundColor: "#0066cc"
                        }*/,
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
                                id: 'B',
                                scaleLabel: {
                                    display: true,
                                    labelString: 'Incoming links',
                                    fontColor: "#009900",
                                },
                                ticks: {
                                    max: 200,
                                    min: 0
                                }
                            },
                            {
                                id: 'C',
                                scaleLabel: {
                                    display: true,
                                    labelString: 'Incoming links',
                                    fontColor: "#cc0000",
                                },
                                ticks: {
                                    max: 1000,
                                    min: 0
                                }
                            }
                        ]
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
 * http://tobiasahlin.com/blog/chartjs-charts-to-get-you-started/
 *
 *
 *
 *
 *
 *
 *
 *
 * */

