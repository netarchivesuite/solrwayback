/**
 * Sort the date array descending (oldest first).
 * Note: Mutates the input array
 * 
 * @param {Date} dateArray 
 */
export function sortDatesDescending(dateArray) {
  // Sort the harvest date objects by time ascending.
  return dateArray.sort((dateA, dateB) => dateA.getTime() - dateB.getTime())
}

/**
 * Converts date to human readable output
 * 
 * @param {Date} date
 * @param {Boolean} showWeekday  
 */
export function toHumanDate(date, showWeekday = false) {
  const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
  const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
  if (date instanceof Date) {
      let dateString = `${months[date.getMonth()]} ${date.getDate()}, ${date.getFullYear()}`
      return showWeekday ? days[date.getDay()] + ', ' + dateString : dateString
  }
  return date
}


