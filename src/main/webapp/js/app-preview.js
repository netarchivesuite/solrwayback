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
    props: ['harvestData','selectedDate'],
    template: `
    <div id="harvestinfoContainer">
        <p v-if="harvestData.length > 0 " class="harvestInfo">
        <span class="label">Number of harvests:</span> {{ this.harvestData.length }}<br>
        <span class="label">First harvest:</span> {{ this.harvestData[0].crawlDate | toLocaleTime}}<br>
        <span class="label">Last harvest:</span> {{ this.harvestData[this.harvestData.length - 1].crawlDate | toLocaleTime}}<br>
        <span class="label">Preview date:</span> <strong>{{ this.selectedDate | toLocaleTime }}</strong>
        </span>
        </p>
    </div>    
    `,
})

Vue.component('slider-container', {
    props: ['harvestData','showPreview','hideSpinner'],
    template: `
    <div id="sliderContainer" class="container" v-if="harvestData">
        <h3>Use slider to choose previews</h3>
        <p>Date: {{ this.sliderValue | toLocaleTime }}</p>
        <div class="slider"></div>
    </div>  
    `,
    data: function() {
        return {
            sliderValue: '',
        };
    },
    watch: { // updating when harvestData are updated
        harvestData: function () {
            this.hideSpinner();
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
        <a href="#" v-on:click="toggleDatepicker()">Choose preview from calendar</a>
        <input class="datepicker" type="hidden" readonly>
        <div></div>
    </div>
    `,
    methods: {
        toggleDatepicker: function(){
            if($('.datepicker').datepicker( "widget" ).is(":visible")){
                $('.datepicker').datepicker('hide');
            }else{
                $('.datepicker').datepicker('show');
            }
        }
    },
    watch: { // updating when harvestData are updated
        harvestData: function () {
            /* setting up Datepicker when harvest data is loaded */
            var min = this.harvestData[0].crawlDate;
            var max = this.harvestData[this.harvestData.length - 1].crawlDate;
            var vm = this; // making var to point to 'this' inside anonymous function
            $('.datepicker').datepicker({
                minDate: new Date(min),
                maxDate: new Date(max),
                inline: true,
                showOtherMonths: true,
                dayNamesMin: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
                firstDay: 1,
                changeYear: true,
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
                v-on:click="showPreview(item.crawlDate)">{{ index +1 }} - {{item.crawlDate | toLocaleTime }}</li>
            </ul>
        </div>
    </div>    
    `,
})

Vue.component('preview-container', {
    props: ['previewData','selectedDate'],
    template: `
    <div id="previewContainer" class="container">
        <h3>Selected date for previews: {{ this.selectedDate | toLocaleTime }}</h3>
        <ul class="previews">
            <li v-for="item in previewData" >
                <span>{{ item.previewDate | toLocaleTime}}</span><br>
                <a :href="item.previewurl" target="_blank"><img :src="item.previewurl" :key="item.previewurl" class="webPageThumb"/></a><br>
                <a :href="item.solrwaybackurl" target="_blank">Go to harvested page</a> | 
                <a :href="item.previewurl" target="_blank">Go to preview image</a>
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
        spinner: false,
        selectedDate: '',
    },
    methods: {
        showPreview: function(time){
            this.previewData = []; //resetting previewdata
            this.selectedDate = time;
            var j = 0
            for (var i = 0; i < this.harvestData.length; i++) {
                if(time <= this.harvestData[i].crawlDate && j < 4){
                    var tempObject = {
                        previewurl: this.harvestData[i].pagePreviewUrl,
                        solrwaybackurl:this.harvestData[i].solrWaybackUrl,
                        previewDate: this.harvestData[i].crawlDate,
                    };
                    this.previewData.push(tempObject);
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
        this.spinner = true;
        var serviceUrl = 'http://' + location.host + '/solrwayback/services/pagepreviews?url=' + url;
        this.url = url;
        this.$http.get(serviceUrl).then((response) => {
            this.harvestData = response.body;
            console.log('this.harvestData: ', this.harvestData);
        }, (response) => {
            console.log('error: ', response);
        });
    }
})

