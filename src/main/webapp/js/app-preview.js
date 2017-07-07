Vue.filter('toLocaleTime', function(value) {
    if (!value) return '';
    var days = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday']
    var date = new Date(value);
    var newValue = days[date.getDay()] + ', ' + date.getDate() + '-' + (date.getMonth() + 1) + '-' + date.getFullYear() +
        ', ' + date.getHours() + ':' + ('0' + (date.getMinutes().toString())).slice(-2);
    return newValue;
    //return date;
})

Vue.component('header-container', {
    props: ['harvestData','url'],
    template: `
    <div id="header">
        <h3 v-if="this.harvestData">Url: {{ this.url }} - Number of harvests: {{ this.harvestData.length }}.</h3>
    </div>    
    `,
    data: function() {
        return {
        };
    },

})

Vue.component('harvests', {
    props: ['harvestData'],
    template: `
    <div>
        <div id="calendarContainer" class="container">
            <label>Show 10 previews after:
                 <input class="datepicker" type="text" readonly v-on:blur="showPreview('getDatepickerDate')">
            </label>
        </div>
        
        <div id="sliderContainer" class="container">
            <input type="text" id="year" readonly style="border:0;">
            <div class="slider"></div>
        </div>
        
        <div id="harvestListContainer" class="container">
            <a href="#" onclick="$('#harvestList').toggle();return false">Choose preview from list og harvests</a>
            <ul id="harvestList" style="display: none">
                <li v-for="(item, index) in harvestData" v-on:click="showPreview('harvestDate',index)">{{ index }} - {{item.crawlDate | toLocaleTime }}</li>
            </ul>
        </div>
        
        <ul class="previews">
            <li v-for="item in previewData" ><img :src="item.previewurl" class="webPageThumb"/></li>
        </ul>
    </div>    
    `,
    data: function() {
        return {
            previewData: [],
        };
    },
    methods:{

        showPreview: function(type,param2){
            this.previewData = []; //resetting previewdata
            if(type='getDatepickerDate'){
                this.timer = setTimeout(function () { this.getDatepickerDate() }.bind(this), 500); //needs timeout because onblur fires before datepicker populate input field
            }

            if(type='datepickerDate'){
                var time = param2;
                console.log('time', time);
                var j = 0
                for (var i = 0; i < this.harvestData.length; i++) {
                    if(time < this.harvestData[i].crawlDate && j < 4){
                        var tempObject = {previewurl: this.harvestData[i].pagePreviewUrl};
                        this.previewData.push(tempObject);
                        console.log(this.previewData);
                        j++;
                    }

                }
            }

            if(type='harvetsDate') {
                var index = param2;
                for (var i = index; i < index + 10 && i < this.harvestData.length; i++) {
                    var tempObject = {previewurl: this.harvestData[i].pagePreviewUrl};
                    this.previewData.push(tempObject);
                    console.log(this.harvestData[i]);
                }
                console.log('this.previewData:', this.previewData);
            }
        },


        getDatepickerDate: function(){
            if($( ".datepicker" ).datepicker( "getDate" )) {
                var date = $(".datepicker").datepicker("getDate");
                var time = date.getTime();
                this.showPreview('datepickerDate',time);
                clearTimeout(this.timer);
            }
        },
    }
})



var app = new Vue({
    el: '#app',
    data: {
        harvestData:[],
    },
    methods: {
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

