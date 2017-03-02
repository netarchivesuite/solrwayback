import {getArrayOfMonthValues, getHarvestsForMonth, getDaysInMonth} from './date'
import {sortDatesDescending} from './util'

/**
 * Generate the monthObject for a given year and parsedHarvestDates.
 * 
 * Output is:
 * {
 *     1: {
 *         dates: [ ... ],
           numberOfHarvests: 5023
 *     },
 *     ...
 * }
 */
export function buildMonthObject(year, parsedHarvestDates) {

    const monthObject = {};

    for (let month of getArrayOfMonthValues()) {
        const allHarvestDatesInMonth = getHarvestsForMonth(year, month, parsedHarvestDates);

        monthObject[month] = {
            days: buildDayObject(allHarvestDatesInMonth),
            numberOfHarvests: allHarvestDatesInMonth.length
        }
    }

    return monthObject;
}



/**
 * Build an object of harvest dates for each day in the month.
 */
function buildDayObject(allHarvestDatesInMonth) {
    if (allHarvestDatesInMonth.length === 0) {
        return {};
    }

    // Sort the harvest date objects by time ascending.
    sortDatesDescending(allHarvestDatesInMonth);

    const daysInMonth = getDaysInMonth(allHarvestDatesInMonth[0]);
    const arrayOfDays = [...Array(daysInMonth).keys()].map(day => day + 1);     // [1, 2, ..., 31]

    // Initialise the object with days as key:
    const daysObject = {};

    for (let day of arrayOfDays) {
        daysObject[day] = [];
    }

    // Populate the daysObject with the harvestDates:
    for (let harvestDate of allHarvestDatesInMonth) {
        daysObject[harvestDate.getDate()].push(harvestDate);                    // daysObject[31] = Date()
    }

    return daysObject;
}





// /**
//  * Calculates and adds an activityLevel property to each month,
//  * this is used to color each table cell.
//  * 
//  * It takes a transformationFunction from activity-level.js. 
//  * This enables easy changing of the transformation types.
//  */
// function addActivityLevelToDataObject(harvestDataObject, transformationFunction) {

//     let {maximumYear, maximumMonth, maximumHarvests} = getMaximumHarvestCount(harvestDataObject);

//     // Loop through each month and assign the activityLevel
//     doForEachMonthInHarvestDataObject(harvestDataObject, (year, month) => {
//         harvestDataObject[year][month].activityLevel = transformationFunction(harvestDataObject[year][month].numberOfHarvests, maximumHarvests);
//     });

//     return harvestDataObject;
// }


// /**
//  * Loops through the data object, returns an object with 3 values: maximumYear, maximumMonth, maximumHarvests.
//  */
// function getMaximumHarvestCount(harvestDataObject) {

//     let maximumYear = null;
//     let maximumMonth = null;
//     let maximumHarvests = 0;

//     // Loop through each month in the data object, check if it beats the record for numberOfHarvests...
//     doForEachMonthInHarvestDataObject(harvestDataObject, (year, month) => {
//         if (harvestDataObject[year][month].numberOfHarvests >= maximumHarvests) {
//             maximumHarvests = harvestDataObject[year][month].numberOfHarvests;
//             maximumYear = year;
//             maximumMonth = month;
//         }
//     });

//     return {
//         maximumYear: maximumYear, 
//         maximumMonth: maximumMonth, 
//         maximumHarvests: maximumHarvests
//     };
// }


// /**
//  * Higher-order function that loops through the harvestDataObject, calling a callback for each month.
//  */
// function doForEachMonthInHarvestDataObject(harvestDataObject, actionFunction) {

//     for (let year of Object.keys(harvestDataObject)) {
//         for (let month of Object.keys(harvestDataObject[year])) {
//             actionFunction(year, month);
//         }
//     }
// }


