/**
 * Copy to clipboard function
 * Kudos:
 * https://stackoverflow.com/questions/400212/how-do-i-copy-to-the-clipboard-in-javascript 
 * 
 * @param {String} text
 *   
 */
export function copyTextToClipboard(text) {
  let textArea = document.createElement('textarea')
  let copyWentOK = false
  
  //
  // *** This styling is an extra step which is likely not required. ***
  //
  // Why is it here? To ensure:
  // 1. the element is able to have focus and selection.
  // 2. if element was to flash render it has minimal visual impact.
  // 3. less flakyness with selection and copying which **might** occur if
  //    the textarea element is not visible.
  //
  // The likelihood is the element won't even render, not even a
  // flash, so some of these are just precautions. However in
  // Internet Explorer the element is visible whilst the popup
  // box asking the user for permission for the web page to
  // copy to the clipboard.
  //
  
  // Place in top-left corner of screen regardless of scroll position.
  textArea.style.position = 'fixed'
  textArea.style.top = 0
  textArea.style.left = 0
  
  // Ensure it has a small width and height. Setting to 1px / 1em
  // doesn't work as this gives a negative w/h on some browsers.
  textArea.style.width = '2em'
  textArea.style.height = '2em'
  
  // We don't need padding, reducing the size if it does flash render.
  textArea.style.padding = 0
  
  // Clean up any borders.
  textArea.style.border = 'none'
  textArea.style.outline = 'none'
  textArea.style.boxShadow = 'none'
  
  // Avoid flash of white box if rendered for any reason.
  textArea.style.background = 'transparent'
  
  textArea.value = text
  
  document.body.appendChild(textArea)
  textArea.focus()
  textArea.select()
  
  try {
    copyWentOK = document.execCommand('copy')
  } catch (err) {
    copyWentOK = false
    console.log('Oops, unable to copy')
  }
  
  document.body.removeChild(textArea)
  return copyWentOK
}


  /**
 * Standard debounce code deduced from the lodash implementation
 * Kudos:
 * https://gist.github.com/nmsdvid/8807205 
 * 
 * @param {Function} callback
 * @param {Int} delay
 *   
 */
export function debounce(callback, delay = 250) {
  let timeoutId
  return (...args) => {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => {
      timeoutId = null
      callback(...args)
    }, delay)
  }
}
  
  