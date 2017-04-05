import {doForEachWeekAndDayInDatesObject} from './iterators';

/**
 * Plugin to add the property activityLevel to every month in the data object.
 * Takes a transformation function to choose the way the activity level is calculated.
 * 
 * @param {Object} datesObject The final object of years, months and days to add data to.
 * @param {Function} transformationFunction The function to calculate activity level from transformation-functions.js
 */
export function addActivityLevelToWeeks(datesObject, transformationFunction) {

    let {maximumYear, maximumWeek, maximumHarvests, maximumDay} = getMaximumHarvestCount(datesObject);

    console.log(maximumYear, maximumWeek, maximumDay, maximumHarvests);

    // Loop through each month and assign the activityLevel
    doForEachWeekAndDayInDatesObject(datesObject, (year, week, day) => {
        datesObject[year]['weeks'][week][day].activityLevel = transformationFunction(datesObject[year]['weeks'][week][day].numberOfHarvests, maximumHarvests);
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
    let maximumWeek = null;
    let maximumDay = null;
    let maximumHarvests = 0;

    // Loop through each month in the data object, check if it beats the record for numberOfHarvests...
    doForEachWeekAndDayInDatesObject(datesObject, (year, week, day) => {
        if (datesObject[year]['weeks'][week][day].numberOfHarvests >= maximumHarvests) {
            maximumHarvests = datesObject[year]['weeks'][week][day].numberOfHarvests;
            maximumYear = year;
            maximumWeek = week;
            maximumDay = day;
        }
    });

    return {
        maximumYear: maximumYear, 
        maximumWeek: maximumWeek, 
        maximumDay: maximumDay,
        maximumHarvests: maximumHarvests
    };
}
