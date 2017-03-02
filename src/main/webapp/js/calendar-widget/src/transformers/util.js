/**
 * Sort the date array descending (oldest first).
 * Note: Mutates the input array
 * 
 * @param {Date} dateArray 
 */
export function sortDatesDescending(dateArray) {
    // Sort the harvest date objects by time ascending.
    return dateArray.sort((dateA, dateB) => dateA.getTime() - dateB.getTime());
}
