import {doForEachMonthInDatesObject} from './iterators';

/**
 * Plugin to add the property activityLevel to every month in the data object.
 * Takes a transformation function to choose the way the activity level is calculated.
 * 
 * @param {Object} datesObject The final object of years, months and days to add data to.
 * @param {Function} transformationFunction The function to calculate activity level from transformation-functions.js
 */
export function addActivityLevelToMonths(datesObject, transformationFunction) {

    let {maximumYear, maximumMonth, maximumHarvests} = getMaximumHarvestCount(datesObject);

    // Loop through each month and assign the activityLevel
    doForEachMonthInDatesObject(datesObject, (year, month) => {
        datesObject[year]['months'][month].activityLevel = transformationFunction(datesObject[year]['months'][month].numberOfHarvests, maximumHarvests);
    });

    return datesObject;
}


/**
 * Loops through the data object, returns an object with 3 values: maximumYear, maximumMonth, maximumHarvests.
 * 
 * @param {Object} datesObject The final object of years, months and days to add data to.
 */
function getMaximumHarvestCount(datesObject) {

    let maximumYear = null;
    let maximumMonth = null;
    let maximumHarvests = 0;

    // Loop through each month in the data object, check if it beats the record for numberOfHarvests...
    doForEachMonthInDatesObject(datesObject, (year, month) => {
        if (datesObject[year]['months'][month].numberOfHarvests >= maximumHarvests) {
            maximumHarvests = datesObject[year]['months'][month].numberOfHarvests;
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
