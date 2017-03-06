import startOfISOWeek from 'date-fns/start_of_iso_week'
import endOfISOWeek from 'date-fns/end_of_iso_week'
import isBefore from 'date-fns/is_before'
import isAfter from 'date-fns/is_after'

/**
 * Returns an array of the months in the year (0-11)
 */
export function getArrayOfMonths() {
    return [...Array(12).keys()];       // [0, 1, 2, ..., 11]
};

/**
 * Returns an array of the months in the year (0-51)
 */
export function getArrayOfWeeks() {
    return [...Array(52).keys()].map(n => n + 1);       // [1, 2, ..., 52]
};

/**
 * Returns an array of the harvest for a given month and year.
 * 
 * @param {number} year 
 * @param {number} month 
 * @param {Array<Date>} parsedHarvestDates 
 */
export function getHarvestsForMonth(year, month, parsedHarvestDates) {
    return parsedHarvestDates
        .filter(date => date.getMonth() === month && date.getFullYear() === year);
}


/**
 * Returns an array of the harvests for a given ISO-week of the year.
 * 
 * @param {number} year 
 * @param {number} month 
 * @param {Array<Date>} parsedHarvestDates 
 */
export function getHarvestsForWeek(year, week, parsedHarvestDates) {
    const startOfWeek = new Date(year)
    const endOfWeek = new Date(year, //TODO????)

    return parsedHarvestDates
        .filter(date => date.getFullYear() === year)
        .filter(date => isBefore(date, endOfISOWeek(date)))
        .filter(date => isAfter(date, startOfISOWeek(date)));
}


/**
 * Given a Date object, return the number of days in the month.
 * Source: http://stackoverflow.com/questions/1184334/get-number-days-in-a-specified-month-using-javascript
 * 
 * It takes adds one to the month of the dateObject, but sets the day to 0. 
 * This gives the last day of the month of the dateObject.
 * 
 * @param {Date} dateObject 
 */
export function getDaysInMonth(dateObject) {
    return new Date(dateObject.getFullYear(), dateObject.getMonth() + 1, 0).getDate();
}

