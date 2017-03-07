import {getArrayOfWeeks, getArrayOfWeekDays, getHarvestsForDay} from './date'
import {sortDatesDescending} from './util'
import getDaysInYear from 'date-fns/get_days_in_year'
import isBefore from 'date-fns/is_before'
import lastDayOfYear from 'date-fns/last_day_of_year'
import addDays from 'date-fns/add_days'

/**
 * Returns the data object of weeks:
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
 * @param {*} parsedHarvestDates for an entire year.
 */
export function buildWeekObject(year, parsedHarvestDates) {

    let day = new Date(year, 0, 1);             // Use the first day of the year.
    const daysInYear = getDaysInYear(day);
    const nextYear = day.getFullYear() + 1;

    let week = 0;                               // Here we'll use a custom week format, just for bookkeeping.
    const weekObject = {};                      // The object to build.

    while (day.getFullYear() < nextYear) {

        let dayOfWeek = (day.getDay() + 6) % 7;     // We want monday as the first day of week!

        if (dayOfWeek === 0) {               // If the day is sunday, increment the week.
            week++;
        }

        if (weekObject[week] === undefined) {
            weekObject[week] = {};
        }

        const harvestsForDay = sortDatesDescending(getHarvestsForDay(day, parsedHarvestDates));

        weekObject[week][dayOfWeek] = {
            date: new Date(day.getTime()),
            harvests: harvestsForDay,
            numberOfHarvests: harvestsForDay.length
        }

        day = addDays(day, 1);
    }

    // Take the first week of the year.
    // Fill it with the missing days (this will help us render it later on).
    if (weekObject[0]) {
        let weekDays = [0, 1, 2, 3, 4, 5, 6];
        
        for (let weekDay of weekDays) {
            if(weekObject[0][weekDay] === undefined) {
                weekObject[0][weekDay] = null;
            }
        }
    }

    return weekObject;
}
