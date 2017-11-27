Vue.component('header-container', {
    props: ['url'],
    template: `
    <div id="header">
        <h2 v-if="url">Domain: {{ url }}</h2>
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
        spinner: false,
        chartData: [],
        chartLabels: [],
    },
    methods: {
        getData: function(){
            this.showSpinner();
            this.url = this.$route.query.url.replace(/http.*:\/\//i,""); //Get domain from URL, using replace and regex to trim domain
            if( this.url.slice(-1) === "/"){ // if trailing slash on url it's removed
                this.url = this.url.slice(0, -1)
            }
            var searchUrl = 'http://' + location.host + '/solrwayback/services/solr/search?query=*:*&start=0&fq=domain%3A' + this.url;

            this.$http.get(searchUrl).then((response) => {
                this.hideSpinner();
                this.errorMsg = "";
                var tempData = response.body.facet_counts.facet_fields.crawl_year;
                for(var i = 0; i < tempData.length; i++){
                    if(i % 2 == 1){
                        this.chartData.push(tempData[i])
                    }else{
                        this.chartLabels.push(tempData[i])
                    }
                }
                console.log('tempData: ', tempData);
                console.log('response: ', response);
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
        },

        drawChart: function(){
            console.log('document.getElementById("line-chart', document.getElementById("line-chart"));
            new Chart(document.getElementById("line-chart"), {
                type: 'line',
                data: {
                    labels: this.chartLabels,
                    datasets: [{
                        data: this.chartData,
                        label: "Number of harvests",
                        borderColor: "#3e95cd",
                        fill: false
                    }/*, {
                        data: [282,350,411,502,635,809,947,1402,3700,5267],
                        label: "Asia",
                        borderColor: "#8e5ea2",
                        fill: false
                    }, {
                        data: [168,170,178,190,203,276,408,547,675,734],
                        label: "Europe",
                        borderColor: "#3cba9f",
                        fill: false
                    }, {
                        data: [40,20,10,16,24,38,74,167,508,784],
                        label: "Latin America",
                        borderColor: "#e8c3b9",
                        fill: false
                    }, {
                        data: [6,3,2,2,7,26,82,172,312,433],
                        label: "North America",
                        borderColor: "#c45850",
                        fill: false
                    }*/
                    ]
                },
                options: {
                    title: {
                        display: true,
                        text: 'Developement of domain: ' + this.url,
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

