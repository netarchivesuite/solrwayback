function groupHarvestDatesByYearAndMonth(harvestDates) {

    const maxDate = new Date(_.max(harvestDates));
    const minDate = new Date(_.min(harvestDates));

    const yearRangeArray = buildYearRangeArray(minDate, maxDate);
    let yearAndMonthArray = addMonthsToYearRangeArray(yearRangeArray);
    
    for(date of harvestDates) {
        yearAndMonthArray = addDateToYearAndMonthArray(date, yearAndMonthArray);
    }

    yearAndMonthArray = [
        {   
            'year': 2007,
            'months': [
                {
                    'month': 1,
                    'monthName': 'January',
                    'harvestDates': [
                        [1,2,3,4,5,6,7,8]
                    ]
                },
                {
                    'month': 2,
                    'monthName': 'February',
                    'harvestDates': [
                        [1,2,3,4,5,6,7,8]
                    ]
                }
            ]
        },
        {   
            'year': 2008,
            'months': [
                {
                    'month': 1,
                    'monthName': 'January',
                    'harvestDates': [
                        [1,2,3,4,5,6,7,8]
                    ]
                },
                {
                    'month': 2,
                    'monthName': 'February',
                    'harvestDates': [
                        [1,2,3,4,5,6,7,8]
                    ]
                }
            ]
        }
    ];

    return {
        minDate: minDate,
        maxDate: maxDate,
        dates: yearAndMonthArray,
        numberOfHarvests: harvestDates.length
    }
}


/**
 * Build an array of years from the minDate year to the maxDate year. E.g. [2007, 2008, 2009, 2010, 2011, ...]
 * minDate, maxDate are Date instances
 */
function buildYearRangeArray(minDate, maxDate) {
    return yearRangeArray = [...Array(maxDate.getFullYear() - minDate.getFullYear() + 1).keys()]     // [0, 1, 2, ...]
        .map(year => year + minDate.getFullYear());                                                  // [2007, 2008, 2009, ...]
}

/**
 * Add months to the year range array.
 */
function addMonthsToYearRangeArray(yearRangeArray) {
    const arrayWithYearsAsKey = [];

    for (year of yearRangeArray) {
        arrayWithYearsAsKey[year] = getArrayWithMonthsAsKey();
    }

    return arrayWithYearsAsKey;
}

function getArrayWithMonthsAsKey() {
    const arrayOfMonths = [...Array(12).keys()].map(n => n + 1)       // [1, 2, 3, ..., 12]
    const arrayWithMonthAsKey = [];

    for (month of arrayOfMonths) {
        arrayWithMonthAsKey[month] = [];
    }

    return arrayWithMonthAsKey;
}

function addDateToYearAndMonthArray(harvestDate, yearAndMonthArray) {
    const date = new Date(harvestDate);
    const month = date.getMonth() + 1;
    const year = date.getFullYear();

    yearAndMonthArray[year][month] = [...yearAndMonthArray[year][month], date];

    return yearAndMonthArray;
}



let harvestDateComponent = Vue.component('harvest-date', {
    props: ['message'],
    data: () => {
        return {
            harvestData: null,
        }
    },
    template: `
        <div v-if="harvestData">
            <p>Harvests: {{ harvestData.numberOfHarvests }}</p>
            <ol>
                <li v-for="year in harvestData.dates">
                    {{ year.year }}
                    <ul>
                        <li v-for="month in year.months">
                            {{ month.monthName }}
                        </li>
                    </ul>
                </li>
            </ol>
        </div>
        <div v-else>
            <p>Fetching harvests</p>
        </div>
    `,
    created() {
        this.$http.get("/solrwayback/services/harvestDates?url=" + encodeURIComponent(window.solrWaybackConfig.url))
        .then(response => {
            this.harvestData = groupHarvestDatesByYearAndMonth(response.data.dates);
        });
    }
});


let app = new Vue({
    el: "#app"
});