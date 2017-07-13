Vue.filter('toLocaleTime', function(value) {
    if (!value) return '';
    var days = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday']
    var date = new Date(value);
    var newValue = days[date.getDay()] + ', ' + date.getDate() + '-' + (date.getMonth() + 1) + '-' + date.getFullYear() +
        ', ' + date.getHours() + ':' + ('0' + (date.getMinutes().toString())).slice(-2);
    return newValue;
})

Vue.component('header-container', {
    props: ['harvestData','url'],
    template: `
    <div id="header">
        <h2 v-if="url">Url: {{ this.url }}</h2>
    </div>    
    `,
})

Vue.component('harvestinfo-container', {
    props: ['harvestData'],
    template: `
    <div id="harvestinfoContainer">
        <p v-if="harvestData.length > 0 " class="harvestInfo"> Number of harvests: {{ this.harvestData.length }}.<br>
        <span >First harvest: {{ this.harvestData[0].crawlDate | toLocaleTime}}<br>
        Last harvest: {{ this.harvestData[this.harvestData.length - 1].crawlDate | toLocaleTime}}.
        </span>
        </p>
    </div>    
    `,
})

Vue.component('slider-container', {
    props: ['harvestData','showPreview'],
    template: `
    <div id="sliderContainer" class="container" v-if="harvestData">
        <p>Date: {{ this.sliderValue | toLocaleTime }}</p>
        <div class="slider"></div>
        <p>Selected date:  {{ this.selectedValue | toLocaleTime }}</p>
    </div>  
    `,
    data: function() {
        return {
            selectedValue: '',
            sliderValue: '',
        };
    },
    watch: { // updating when harvestData are updated
        harvestData: function () {
            /* Setting up jQueryUI slider when harvest data is loaded */
            var min = this.harvestData[0].crawlDate;
            var max = this.harvestData[this.harvestData.length - 1].crawlDate;
            this.sliderValue = min;
            var vm = this; // making var to point to 'this' inside anonymous function
            $( ".slider" ).slider({
                value: min,
                min: min,
                max: max,
                slide: function( event, ui ) {
                    vm.sliderValue = ui.value;
                },
                change: function( event, ui ) {
                    vm.selectedValue =  ui.value;
                    vm.showPreview(ui.value)
                }
            });
            $( ".slider" ).datepicker( "show" );
        },
    }
})

Vue.component('datepicker-container', {
    props: ['harvestData','showPreview'],
    template: `
    <div id="calendarContainer" class="container">
        <a href="#" onclick="$('.datepicker').datepicker('show');return false">Choose preview from calendar</a>
        <input class="datepicker" type="text" readonly style="border:0; color:white">
    </div>
    `,
    watch: { // updating when harvestData are updated
        harvestData: function () {
            /* setting up Datepicker when harvest data is loaded */
            var vm = this; // making var to point to 'this' inside anonymous function
            $('.datepicker').datepicker({
                inline: true,
                showOtherMonths: true,
                dayNamesMin: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
                firstDay: 1,
                changeYear: true,
                yearRange: "c-50:c",
                changeMonth: true,
                dateFormat: "yy-mm-dd",
                onSelect: function(dateText) {
                    var date = new Date(dateText);
                    var time = date.getTime();
                    vm.showPreview(time)
                },
             });
        },
    },
})

Vue.component('harvests-container', {
    props: ['harvestData','showPreview'],
    template: `
    <div>  
        <div id="harvestListContainer" class="container">
            <a href="#" onclick="$('#harvestList').toggle();return false">Choose preview from list of harvests</a>
            <ul id="harvestList">
                <li v-for="(item, index) in harvestData" class="link" onclick="$('#harvestList').toggle()"
                v-on:click="showPreview(item.crawlDate)">{{ index }} - {{item.crawlDate | toLocaleTime }}</li>
            </ul>
        </div>
    </div>    
    `,
})

Vue.component('preview-container', {
    props: ['previewData'],
    template: `
    <div id="previewContainer" class="container">
        <ul class="previews">
            <li v-for="item in previewData" >
                <span>{{ item.previewDate | toLocaleTime}}</span><br>
                <a :href="item.previewurl" target="_blank"><img :src="item.previewurl" class="webPageThumb"/></a><br>
                <a :href="item.solrwaybackurl" target="_blank">Go to harvested page</a>
            </li>
        </ul>
    </div>    
    `,
})



var app = new Vue({
    el: '#app',
    data: {
        harvestData:[],
        previewData:[],
    },
    methods: {
        showPreview: function(time){
            this.previewData = []; //resetting previewdata
            console.log('time', time);
            var j = 0
            for (var i = 0; i < this.harvestData.length; i++) {
                if(time <= this.harvestData[i].crawlDate && j < 4){
                    var tempObject = {
                        previewurl: this.harvestData[i].pagePreviewUrl,
                        solrwaybackurl:this.harvestData[i].solrWaybackUrl,
                        previewDate: this.harvestData[i].crawlDate,
                    };
                    this.previewData.push(tempObject);
                    console.log(this.previewData);
                    j++;
                }
            }
        },

        showSpinner: function(){
            this.spinner = true;
        },

        hideSpinner: function(){
            this.spinner = false;
        },
    },
    created: function() {
        var serviceUrl = "http://localhost:8080/solrwayback/services/pagepreviews?url=" + url;
        this.url = url;
        this.$http.get(serviceUrl).then((response) => {
            this.harvestData = response.body;
            console.log('this.harvestData: ', this.harvestData);
        }, (response) => {
            console.log('error: ', response);
        });
    },
})

