import {getArrayOfWeeks, getArrayOfWeekDays} from './date'


export function buildWeekObject(year, parsedHarvestDates) {

    const weekObject = {};

    for (let week of getArrayOfWeeks()) {
        const allHarvestDatesInWeek = getHarvestsForWeek(year, week, parsedHarvestDates);

        weekObject[week] = buildWeekDaysObject(allHarvestDatesInWeek);
    }

    return weekObject;
}


function buildWeekDaysObject(allHarvestDatesInWeek) {
    
    const daysObject = {};

    for (let day of getArrayOfWeekDays()) {
        const harvestsForDay = 

        daysObject[day] = buildDayObject()
    }

    return daysObject;
}



/**
 * Build an object of harvest dates for each day in the month.
 * 
 * @param {Array<Date>} allHarvestDatesInWeek
 */
function buildDayObject(allHarvestDatesInWeek) {

    return



    if (allHarvestDatesInWeek.length === 0) {
        return {};
    }

    // Sort the harvest date objects by time ascending.
    sortDatesDescending(allHarvestDatesInWeek);

    const arrayOfDays = getArrayOfWeekDays();
    
    // Initialise the object with days as key:
    const daysObject = {};

    for (let day of arrayOfDays) {
        daysObject[day] = [];
    }

    // Populate the daysObject with the harvestDates:
    for (let harvestDate of allHarvestDatesInWeek) {
        daysObject[harvestDate.getDate()].push(harvestDate);                    // daysObject[31] = Date()
    }

    return daysObject;
}
