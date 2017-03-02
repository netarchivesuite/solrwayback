import {getArrayOfMonths, getHarvestsForMonth, getDaysInMonth} from './date'
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
 * 
 * @param {number} year 
 * @param {Array<Date>} parsedHarvestDates 
 */
export function buildMonthObject(year, parsedHarvestDates) {

    const monthObject = {};

    for (let month of getArrayOfMonths()) {
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
 * 
 * @param {Array<Date>} allHarvestDatesInMonth 
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
