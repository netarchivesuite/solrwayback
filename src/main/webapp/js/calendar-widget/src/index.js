/**
 * This is the main vue component for the graph.
 */

import Vue from 'vue'
import VueResource from 'vue-resource'

import {groupHarvestDatesByYearAndMonth} from './transformer';
import {calculateLinearActivityLevel, calculateLogarithmicActivityLevel} from './transformers/plugins/transformation-functions'
import VTooltip from 'v-tooltip'

Vue.use(VueResource);
Vue.use(VTooltip);


function toHumanDate(value) {
    const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

    if (value instanceof Date) {
        return `${months[value.getMonth()]} ${value.getDay()}, ${value.getFullYear()}`;
    } 
    
    return value;
}

Vue.filter('human-date', function (value) {
    return toHumanDate(value);
});

Vue.filter('formatted-number', function (value) {
    if (!isNaN(value)) {
        return value.toLocaleString();
    }

    return value;
});

Vue.component('harvest-title', {
    props: ['url'],
    template: `<h1>Harvests for {{ url }}</h1>`
});

Vue.component('harvest-date', {
    props: ['url'],
    data: () => {
        return {
            harvestData: null,
            showDetails: false,
        }
    },
    template: `
        <div>
            <div v-if="harvestData" class="tableContainer">
                <p>
                    First harvest: <strong>{{ harvestData.fromDate | human-date }}</strong><br>
                    Latest harvest: <strong>{{ harvestData.toDate | human-date }}</strong>
                </p>
                <p>Total harvests: <strong>{{ harvestData.numberOfHarvests | formatted-number }}</strong></p>
                
                <transition name="slideLeft">
                    <div v-if="!showDetails" class="yearTables">
                        <table class="monthLabels">
                            <tr><td class="empty">&nbsp;</td></tr>
                            <tr><td>January</td></tr>
                            <tr><td>February</td></tr>
                            <tr><td>March</td></tr>
                            <tr><td>April</td></tr>
                            <tr><td>May</td></tr>
                            <tr><td>June</td></tr>
                            <tr><td>July</td></tr>
                            <tr><td>August</td></tr>
                            <tr><td>September</td></tr>
                            <tr><td>October</td></tr>
                            <tr><td>November</td></tr>
                            <tr><td>December</td></tr>
                        </table>
                        <table v-for="(yearData, year) in harvestData.dates">
                            <thead>
                                <tr>
                                    <th>{{ year }}</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(data, month) in yearData.months">
                                    <td v-on:click="showDays(year, month)" v-tooltip.top-center="'Harvests: ' + data.numberOfHarvests.toLocaleString()" v-bind:class="{activityLevel4: data.activityLevel === 4, activityLevel3: data.activityLevel === 3, activityLevel2: data.activityLevel === 2, activityLevel1: data.activityLevel === 1}">&nbsp;</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>                
                </transition>        
                <transition name="slideRight">
                <div v-if="showDetails" class="detailsContainer">
                    <div id="details">
                        <div v-on:click="showDetails = false" class="hideDetails">Hide details</div>
                        <h3>Details for {{ year }}</h3>
                        <table v-for="(week, weekNumber) in harvestData.dates[year]['weeks']">
                            <thead>
                                <tr>
                                    <th>{{ weekNumber }}</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(data, dayNumber) in week">
                                    <td v-tooltip.top-center="data.date.toLocaleString() + '<br>Harvests: ' + data.numberOfHarvests.toLocaleString()" v-bind:class="{activityLevel4: data.activityLevel === 4, activityLevel3: data.activityLevel === 3, activityLevel2: data.activityLevel === 2, activityLevel1: data.activityLevel === 1}">&nbsp;</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                 </div>               
                </transition> 
                <transition name="slideLeft">  
                    <div v-if="!showDetails" id="legends">
                        Less <div class="legend legend0"></div>
                        <div class="legend legend1"></div>
                        <div class="legend legend2"></div>
                        <div class="legend legend3"></div>
                        <div class="legend legend4"></div> More
                    </div>
                </transition>   
            </div>
            <template v-else>
                <div id="spinner">
                    <p class="spinnerText">Fetching harvests</p>
                </div>
                <div id="overlay"></div>
            </template>
        </div>
    `,
    created() {
        this.$http.get("/solrwayback/services/harvestDates?url=" + encodeURIComponent(this.url))
        .then(response => {
            this.harvestData = groupHarvestDatesByYearAndMonth(response.data.dates, calculateLinearActivityLevel);
        });
    },
    methods: {
        showDays(year, month) {
            this.showDetails = true;
            this.year = year;
            this.month = month;
        }
    }
});


let app = new Vue({
    el: "#app",
    data: {
        url: window.solrWaybackConfig.url
    }
});