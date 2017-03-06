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

/**
 * Transform a Javascript Date to a human readable string.
 * 
 * @param {Date} date 
 */
function toHumanDate(date) {
    const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

    if (date instanceof Date) {
        return `${months[date.getMonth()]} ${date.getDate()}, ${date.getFullYear()}`;
    } 
    
    return date;
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
            view: 'year-month',
            year: null
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
                <br>
                <p>
                    Show: 
                    <span class="pointer" :class="{active: this.view === 'year-month'}" @click="showYearMonth">Months</span> - 
                    <span class="pointer" :class="{active: this.view === 'all-years'}" @click="showAllYears">Days</span>
                </p>

                <transition name="slideLeft">
                    <year-month-graph v-if="view === 'year-month'" :harvest-data="harvestData" :show-year-details="showYearWeek"></year-month-graph>
                </transition>        
                <transition name="slideRight">
                    <week-graph v-if="view === 'year-week'" :year="year" :harvest-data="harvestData" :show-all="showYearMonth" class="detailsContainer"></week-graph>
                </transition> 
                <transition name="slideRight">
                    <all-years-graph v-if="view === 'all-years'" :harvest-data="harvestData" class="detailsContainer"></all-years-graph>
                </transition> 
                <transition name="slideLeft">  
                    <color-legend></color-legend>
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
        showYearWeek(year) {
            this.year = year;
            this.view = 'year-week';
        },
        showYearMonth() {
            this.year = null;
            this.view = 'year-month';
        },
        showAllYears() {
            this.year = null;
            this.view = 'all-years';
        }
    }
});


Vue.component('year-month-graph', {
    props: ['harvestData', 'showYearDetails'],    // showWeeks is a callback function.
    template: `
        <div class="yearTables">
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
                        <td v-on:click="showYearDetails(year)" v-tooltip.top-center="formatHarvestDate(data)" v-bind:class="mapActivityLevel(data)">&nbsp;</td>
                    </tr>
                </tbody>
            </table>
        </div>                 
    `,
    methods: {
        formatHarvestDate(data) {
            return 'Harvests: ' + data.numberOfHarvests.toLocaleString();
        },
        mapActivityLevel(data) {
            return {
                activityLevel4: data.activityLevel === 4, 
                activityLevel3: data.activityLevel === 3, 
                activityLevel2: data.activityLevel === 2, 
                activityLevel1: data.activityLevel === 1
            };
        }
    }
})


Vue.component('week-graph', {
    props: ['harvestData', 'year', 'showAll'],
    template: `
    <div id="details">
        <div v-on:click="showAll()" class="hideDetails">Hide details</div>
        <p>Harvests in {{ year }}</p>
        <table v-for="(week, weekNumber) in harvestData.dates[year]['weeks']">
            <tbody>
                <tr v-for="(data, dayNumber) in week">
                    <td class="weekday" v-tooltip.top-center="formatHarvestDate(data)" v-bind:class="mapActivityLevel(data)"></td>
                </tr>
            </tbody>
        </table>
    </div>
    `,
    methods: {
        formatHarvestDate(data) {
            return toHumanDate(data.date) + '<br>' +
                'Harvests: ' + data.numberOfHarvests.toLocaleString()
        },
        mapActivityLevel(data) {
            return {
                activityLevel4: data.activityLevel === 4, 
                activityLevel3: data.activityLevel === 3, 
                activityLevel2: data.activityLevel === 2, 
                activityLevel1: data.activityLevel === 1
            };
        }
    }
});




Vue.component('all-years-graph', {
    props: ['harvestData', 'showAll'],
    template: `
    <div id="details">
        <div v-for="(_, year) in harvestData.dates">
            <p>{{ year }}</p>
            <table v-for="(week, weekNumber) in harvestData.dates[year]['weeks']">
                <tbody>
                    <tr v-for="(data, dayNumber) in week">
                        <td class="weekday" v-tooltip.top-center="formatHarvestDate(data)" v-bind:class="mapActivityLevel(data)"></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
    `,
    methods: {
        formatHarvestDate(data) {
            return toHumanDate(data.date) + '<br>' +
                'Harvests: ' + data.numberOfHarvests.toLocaleString()
        },
        mapActivityLevel(data) {
            return {
                activityLevel4: data.activityLevel === 4, 
                activityLevel3: data.activityLevel === 3, 
                activityLevel2: data.activityLevel === 2, 
                activityLevel1: data.activityLevel === 1
            };
        }
    }
});


Vue.component('color-legend', {
    template: `
        <div id="legends">
            Less <div class="legend legend0"></div>
            <div class="legend legend1"></div>
            <div class="legend legend2"></div>
            <div class="legend legend3"></div>
            <div class="legend legend4"></div> More
        </div>
    `
});

let app = new Vue({
    el: "#app",
    data: {
        url: window.solrWaybackConfig.url
    }
});