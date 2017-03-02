/**
 * Plugin to add the property activityLevel to every month in the data object.
 * Takes a transformation function to choose the way the activity level is calculated.
 * 
 * @param {Object} datesObject The final data object to aggregate.
 * @param {Function} transformationFunction The function to calculate activity level from transformation-functions.js
 */
export function addActivityLevelToMonths(datesObject, transformationFunction) {

    let {maximumYear, maximumMonth, maximumHarvests} = getMaximumHarvestCount(datesObject);

    // Loop through each month and assign the activityLevel
    doForEachMonthInHarvestDataObject(datesObject, (year, month) => {
        datesObject[year]['months'][month].activityLevel = transformationFunction(datesObject[year]['months'][month].numberOfHarvests, maximumHarvests);
    });

    return datesObject;
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
        if (harvestDataObject[year]['months'][month].numberOfHarvests >= maximumHarvests) {
            maximumHarvests = harvestDataObject[year]['months'][month].numberOfHarvests;
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
        for (let month of Object.keys(harvestDataObject[year]['months'])) {
            actionFunction(year, month);
        }
    }
}
