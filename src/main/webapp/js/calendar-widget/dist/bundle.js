/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};

/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {

/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId])
/******/ 			return installedModules[moduleId].exports;

/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};

/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);

/******/ 		// Flag the module as loaded
/******/ 		module.l = true;

/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}


/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;

/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;

/******/ 	// identity function for calling harmony imports with the correct context
/******/ 	__webpack_require__.i = function(value) { return value; };

/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, {
/******/ 				configurable: false,
/******/ 				enumerable: true,
/******/ 				get: getter
/******/ 			});
/******/ 		}
/******/ 	};

/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};

/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };

/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";

/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = 2);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = calculateLinearActivityLevel;
/* unused harmony export calculateLogarithmicActivityLevel */
/**
 * These are functions to calculate the activitylevel for a given month.
 */


/**
 * Calculate activity level linearly between 0 and 4.
 * 0 is no activity level at all, 4 is the max level.
 */
function calculateLinearActivityLevel(harvestsInMonth, maximumHarvests) {
    if (harvestsInMonth > maximumHarvests * 0.75 && harvestsInMonth <= maximumHarvests) {
        return 4;
    } else if (harvestsInMonth > maximumHarvests * 0.50 && harvestsInMonth <= maximumHarvests * 0.75) {
        return 3;
    } else if (harvestsInMonth > maximumHarvests * 0.25 && harvestsInMonth <= maximumHarvests * 0.50) {
        return 2;
    } else if (harvestsInMonth > 0 && harvestsInMonth <= maximumHarvests * 0.25) {
        return 1;
    } 

    return 0;
}


/**
 * Calculate activity level logarithmically.
 */
function calculateLogarithmicActivityLevel(harvestsInMonth, maximumHarvests) {

    const logarithmicResult = getBaseLog(maximumHarvests, harvestsInMonth);

    if (logarithmicResult > 0.75 && logarithmicResult <= 1) {
        return 4;
    } else if (logarithmicResult > 0.50 && logarithmicResult <= 0.75) {
        return 3;
    } else if (logarithmicResult > 0.25 && logarithmicResult <= 0.50) {
        return 2;
    } else if (logarithmicResult > 0 && logarithmicResult <= 0.25) {
        return 1;
    } 

    return 0;
}


/**
 * The following function returns the logarithm of y with base x, ie. logx(y):
 */
function getBaseLog(x, y) {
    return Math.log(y) / Math.log(x);
}



/***/ }),
/* 1 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (immutable) */ __webpack_exports__["a"] = groupHarvestDatesByYearAndMonth;
/**
 * Transforms an array of harvest dates as epoch times into a suitable format for rendering a graph with iteration.
 * This is used to transform the API response into a usable format for VueJS.
 */
function groupHarvestDatesByYearAndMonth(harvestDates, transformationFunction) {

    const fromDate = new Date(Math.min(...harvestDates));
    const toDate = new Date(Math.max(...harvestDates));

    // Parse the harvest dates into an array of Date objects.
    // Validate that the dates are integers.
    const parsedHarvestDates = harvestDates
        .filter(date => parseInt(date) !== NaN)
        .map(date => new Date(date));

    // Build an object with keys as the years.
    const yearRangeObject = buildYearRangeObject(fromDate, toDate);

    // Build Harvest Data Object.
    const harvestDataObject = addActivityLevelToDataObject(
        buildHarvestDataObject(yearRangeObject, parsedHarvestDates),
        transformationFunction
    );

    return {
        fromDate: fromDate,
        toDate: toDate,
        dates: harvestDataObject,
        numberOfHarvests: harvestDates.length
    }
}


/**
 * Build an object with keys as the years, e.g.
 * {
 *     2007: {},
 *     2008: {},
 *     ...
 * }
 */
function buildYearRangeObject(fromDate, toDate) {
    const yearRangeArray = buildYearRangeArray(fromDate, toDate);
    const yearRangeObject = {};

    for (let year of yearRangeArray) {
        yearRangeObject[year] = [];
    }  

    return yearRangeObject;
}


/**
 * Build an array of years from the minDate year to the maxDate year. E.g. [2007, 2008, 2009, 2010, 2011, ..., 2017]
 * minDate, maxDate are Date instances
 */
function buildYearRangeArray(minDate, maxDate) {
    return [...Array(maxDate.getFullYear() - minDate.getFullYear() + 1).keys()]     // e.g. [0, 1, 2, ..., 10]
        .map(year => year + minDate.getFullYear());                                                  // e.g. [2007, 2008, 2009, ..., 2017]
}


/**
 * Add months to the year range object.
 * Output is:
 * {
 *     2007: {
 *         1: {
 *             dates: [ ... ],
               numberOfHarvests: 5023
 *         },
 *         ...
 *     },
 *     ...
 * }
 */
function buildHarvestDataObject(yearRangeObject, parsedHarvestDates) {

    const arrayOfMonthValues = [...Array(12).keys()];       // [0, 1, 2, ..., 11]

    // Iterate over all years in the yearRangeObject
    for (let yearAsString of Object.keys(yearRangeObject)) {
        const year = parseInt(yearAsString);        // Since Object.keys() returns an array of strings, we need to convert years to a number.

        // Iterate over all months (0-11)
        for (let month of arrayOfMonthValues) {
            const allHarvestDatesInMonth = getHarvestsForMonth(year, month, parsedHarvestDates);

            yearRangeObject[year][month] = {
                dates: allHarvestDatesInMonth,
                numberOfHarvests: allHarvestDatesInMonth.length
            }
        }
    }

    return yearRangeObject;
}


/**
 * Return an array of all the parsedHarvestDates in the given year and month.
 */
function getHarvestsForMonth(year, month, parsedHarvestDates) {
    return parsedHarvestDates
        .filter(date => date.getMonth() === month && date.getFullYear() === year)
}


/**
 * Calculates and adds an activityLevel property to each month,
 * this is used to color each table cell.
 * 
 * It takes a transformationFunction from activity-level.js. 
 * This enables easy changing of the transformation types.
 */
function addActivityLevelToDataObject(harvestDataObject, transformationFunction) {

    let {maximumYear, maximumMonth, maximumHarvests} = getMaximumHarvestCount(harvestDataObject);

    // Loop through each month and assign the activityLevel
    doForEachMonthInHarvestDataObject(harvestDataObject, (year, month) => {
        harvestDataObject[year][month].activityLevel = transformationFunction(harvestDataObject[year][month].numberOfHarvests, maximumHarvests);
    });

    return harvestDataObject;
}


/**
 * Loops through the data object, returns an object with 3 values: maximumYear, maximumMonth, maximumHarvests.
 */
function getMaximumHarvestCount(harvestDataObject) {

    let maximumYear = null;
    let maximumMonth = null;
    let maximumHarvests = 0;

    // Loop through each month in the data object, check if it beats the record for numberOfHarvests...
    doForEachMonthInHarvestDataObject(harvestDataObject, (year, month) => {
        if (harvestDataObject[year][month].numberOfHarvests >= maximumHarvests) {
            maximumHarvests = harvestDataObject[year][month].numberOfHarvests;
            maximumYear = year;
            maximumMonth = month;
        }
    });

    return {
        maximumYear: maximumYear, 
        maximumMonth: maximumMonth, 
        maximumHarvests: maximumHarvests
    };
}


/**
 * Higher-order function that loops through the harvestDataObject, calling a callback for each month.
 */
function doForEachMonthInHarvestDataObject(harvestDataObject, actionFunction) {

    for (let year of Object.keys(harvestDataObject)) {
        for (let month of Object.keys(harvestDataObject[year])) {
            actionFunction(year, month);
        }
    }
}



/***/ }),
/* 2 */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__transformer__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__activity_level__ = __webpack_require__(0);
/**
 * This is the main vue component for the graph.
 */




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
                        <td :title="'Antal høstninger: ' + data.numberOfHarvests" v-bind:class="{activityLevel4: data.activityLevel === 4, activityLevel3: data.activityLevel === 3, activityLevel2: data.activityLevel === 2, activityLevel1: data.activityLevel === 1}">&nbsp;</td>
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
            this.harvestData = __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__transformer__["a" /* groupHarvestDatesByYearAndMonth */])(response.data.dates, __WEBPACK_IMPORTED_MODULE_1__activity_level__["a" /* calculateLinearActivityLevel */]);
        });
    }
});


let app = new Vue({
    el: "#app"
});

/***/ })
/******/ ]);