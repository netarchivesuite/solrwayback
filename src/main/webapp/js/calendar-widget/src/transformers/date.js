/**
 * Returns an array of the months in the year (0-11)
 */
export function getArrayOfMonths() {
    return [...Array(12).keys()];       // [0, 1, 2, ..., 11]
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
 * Returns an array of the harvests for a given date.
 * 
 * @param {Date} day 
 * @param {Array<Date>} parsedHarvestDates 
 */
export function getHarvestsForDay(day, parsedHarvestDates) {
    return parsedHarvestDates
        .filter(date => isSameDay(date, day));
}


/**
 * Helper function for determining of two dates are the same day.
 * 
 * @param {*} day1 
 * @param {*} day2 
 */
function isSameDay(day1, day2) {
    return day1.getFullYear() === day2.getFullYear() && day1.getMonth() === day2.getMonth() && day1.getDate() === day2.getDate();
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

