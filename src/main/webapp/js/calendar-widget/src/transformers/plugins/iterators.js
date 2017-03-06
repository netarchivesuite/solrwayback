/**
 * Higher-order function that loops through the harvestDataObject, calling a callback for each month.
 * 
 * @param {Object} datesObject The final object of years, months and days to add data to.
 * @param {Function} actionFunction The callback to execute for every month.
 */
export function doForEachMonthInDatesObject(datesObject, actionFunction) {

    for (let year of Object.keys(datesObject)) {
        for (let month of Object.keys(datesObject[year]['months'])) {
            actionFunction(year, month);
        }
    }
}

/**
 * Higher-order function that loops through the harvestDataObject, calling a callback for each day in the week.
 * 
 * @param {Object} datesObject The final object of years, months and days to add data to.
 * @param {Function} actionFunction The callback to execute for every month.
 */
export function doForEachWeekAndDayInDatesObject(datesObject, actionFunction) {

    for (let year of Object.keys(datesObject)) {
        for (let week of Object.keys(datesObject[year]['weeks'])) {
            for (let day of Object.keys(datesObject[year]['weeks'][week])) {
                if (datesObject[year]['weeks'][week][day] !== null) {
                    actionFunction(year, week, day);
                }
            }
        }
    }
}
