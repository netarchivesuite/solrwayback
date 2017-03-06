import {getArrayOfWeeks, getArrayOfWeekDays, getHarvestsForWeek} from './date'
import {sortDatesDescending} from './util'
import startOfISOYear from 'date-fns/start_of_iso_year'
import getISODay from 'date-fns/get_iso_day'

/**
 * Returns the data object of ISO weeks:
 * 
 * {
 *     1: {
 *         0: {
 *             harvests: [
 *                 Date-object,
 *                 Date-object,
 *                 ...
 *             ],
 *             numberOfHarvests: 123
 *         },
 *         1: {
 *             ...
 *         },
 *         ...
 *     }
 * }
 * 
 * @param {*} year 
 * @param {*} parsedHarvestDates 
 */
export function buildWeekObject(year, parsedHarvestDates) {

    const weekObject = {};

    for (let isoWeek of getArrayOfWeeks()) {    // Loops from 1 to 52
        const harvestDatesInWeek = getHarvestsForWeek(year, isoWeek, parsedHarvestDates);

        console.log("Harvests for week " + isoWeek + " year " + year);
        console.log(harvestDatesInWeek);

        weekObject[isoWeek] = buildWeekDaysObject(harvestDatesInWeek);
    }

    return weekObject;
}


/**
 * {
 *     0: {
 *         harvests: [
 *             Date-object,
 *             Date-object,
 *             ...
 *         ],
 *         numberOfHarvests: 123
 *     },
 *     1: {
 *         ...
 *     },
 *     ...
 * }
 * 
 * @param {*} allHarvestDatesInWeek 
 */
function buildWeekDaysObject(allHarvestDatesInWeek) {
    
    const daysObject = {};

    for (let day of [1,2,3,4,5,6,7]) {     // Loops from 1 to 7, note - it's ISO days: From monday to sunday.
        const harvestsForDay = allHarvestDatesInWeek.filter(date => getISODay(date) === day);
        
        daysObject[day] = {};
        daysObject[day]['harvests'] = buildHarvestArray(harvestsForDay)

        // Attach the date to every day. We know the week number, and the year number, but what is there
        daysObject[day]['date'] = harvestsForDay.length > 0 ? harvestsForDay[0] : "Unknown date"
    }

    return daysObject;
}



/**
 * Build an object of harvest dates for each day in the month.
 * 
 * @param {Array<Date>} allHarvestDatesInWeek
 */
function buildHarvestArray(harvestsInDay) {

    if (harvestsInDay.length === 0) {
        return [];
    }

    // Sort the harvest date objects by time ascending.
    sortDatesDescending(harvestsInDay);

    return harvestsInDay;
}
