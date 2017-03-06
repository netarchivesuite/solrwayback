import {buildMonthObject} from './transformers/month-transformer'
import {buildWeekObject} from './transformers/week-transformer'
import {addActivityLevelToMonths} from './transformers/plugins/add-month-activity-level'
import {addActivityLevelToWeeks} from './transformers/plugins/add-weekday-activity-level'

/**
 * Transforms an array of harvest dates as epoch times with ms into a suitable format for rendering a graph with iteration.
 * This is used to transform the API response into a usable format for VueJS.
 * 
 * TODO: Handle empty response.
 */
export function groupHarvestDatesByYearAndMonth(harvestDates, transformationFunction) {

    const fromDate = new Date(Math.min(...harvestDates));
    const toDate = new Date(Math.max(...harvestDates));

    // Parse the harvest dates into an array of Date objects.
    // Validate that the dates are integers.
    const parsedHarvestDates = harvestDates
        .filter(date => parseInt(date) !== NaN)
        .map(date => new Date(date));

    // Populate the dates from parsedHarvestDates.
    let datesObject = buildDatesObject(fromDate, toDate, parsedHarvestDates);

    // Add activity level to the dates object.
    // Add any other plugins here:
    datesObject = addActivityLevelToMonths(datesObject, transformationFunction);
    datesObject = addActivityLevelToWeeks(datesObject, transformationFunction);

    console.log(datesObject);

    return {
        fromDate: fromDate,
        toDate: toDate,
        dates: datesObject,
        numberOfHarvests: harvestDates.length
    }
}


/**
 * Build an object with keys as the years, and the data within each year.
 * {
 *     2007: {
 *         'weeks': {...},
 *         'months': {...} 
 *     },
 *     ...
 * }
 */
function buildDatesObject(fromDate, toDate, parsedHarvestDates) {
    const yearRangeArray = buildYearRangeArray(fromDate, toDate);
    const yearRangeObject = {};

    for (let year of yearRangeArray) {
        yearRangeObject[year] = {};
        yearRangeObject[year]['months'] = buildMonthObject(year, parsedHarvestDates);
        yearRangeObject[year]['weeks'] = buildWeekObject(year, parsedHarvestDates);
    }  

    return yearRangeObject;
}


/**
 * Build an array of years from the minDate year to the maxDate year. E.g. [2007, 2008, 2009, 2010, 2011, ..., 2017]
 * minDate, maxDate are Date instances
 */
function buildYearRangeArray(fromDate, toDate) {
    return [...Array(toDate.getFullYear() - fromDate.getFullYear() + 1).keys()]             // e.g. [0, 1, 2, ..., 10]
        .map(year => year + fromDate.getFullYear());                                        // e.g. [2007, 2008, 2009, ..., 2017]
}
