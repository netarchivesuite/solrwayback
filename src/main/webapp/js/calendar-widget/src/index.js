/**
 * This is the main vue component for the graph.
 */

import Vue from 'vue'
import VueResource from 'vue-resource'

import {groupHarvestDatesByYearAndMonth} from './transformer';
import {calculateLinearActivityLevel, calculateLogarithmicActivityLevel} from './activity-level'
import VTooltip from 'v-tooltip'

Vue.use(VueResource);
Vue.use(VTooltip);

Vue.component('harvest-date', {
    data: () => {
        return {
            harvestData: null,
        }
    },
    template: `
        <div v-if="harvestData" class="tableContainer">
            <p>Harvests: {{ harvestData.numberOfHarvests }}</p>
            <table>
                <tr><td>&nbsp;</td></tr>
                <tr><td>Januar</td></tr>
                <tr><td>Februar</td></tr>
                <tr><td>Marts</td></tr>
                <tr><td>April</td></tr>
                <tr><td>Maj</td></tr>
                <tr><td>Juni</td></tr>
                <tr><td>Juli</td></tr>
                <tr><td>August</td></tr>
                <tr><td>September</td></tr>
                <tr><td>Oktober</td></tr>
                <tr><td>November</td></tr>
                <tr><td>December</td></tr>
            </table>
            <table v-for="(months, year) in harvestData.dates">
                <thead>
                    <tr>
                        <th>{{ year }}</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="(data, month) in months">
                        <td v-tooltip.top-center="'Antal høstninger: ' + data.numberOfHarvests" v-bind:class="{activityLevel4: data.activityLevel === 4, activityLevel3: data.activityLevel === 3, activityLevel2: data.activityLevel === 2, activityLevel1: data.activityLevel === 1}">&nbsp;</td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div v-else>
            <p>Fetching harvests</p>
        </div>
    `,
    created() {
        this.$http.get("/solrwayback/services/harvestDates?url=" + encodeURIComponent(window.solrWaybackConfig.url))
        .then(response => {
            this.harvestData = groupHarvestDatesByYearAndMonth(response.data.dates, calculateLinearActivityLevel);
        });
    }
});


let app = new Vue({
    el: "#app"
});