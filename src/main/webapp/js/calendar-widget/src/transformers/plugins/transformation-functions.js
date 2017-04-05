/**
 * Calculate activity level linearly between 0 and 4.
 * 0 is no activity level at all, 4 is the max level.
 */
export function calculateLinearActivityLevel(harvestsInMonth, maximumHarvests) {
    if (harvestsInMonth > maximumHarvests * 0.75 && harvestsInMonth <= maximumHarvests) {
        return 4;
    } else if (harvestsInMonth > maximumHarvests * 0.50 && harvestsInMonth <= maximumHarvests * 0.75) {
        return 3;
    } else if (harvestsInMonth > maximumHarvests * 0.25 && harvestsInMonth <= maximumHarvests * 0.50) {
        return 2;
    } else if (harvestsInMonth > 0 && harvestsInMonth <= maximumHarvests * 0.25) {
        return 1;
    } 

    return 0;
}


/**
 * Calculate activity level logarithmically.
 */
export function calculateLogarithmicActivityLevel(harvestsInMonth, maximumHarvests) {

    const logarithmicResult = getBaseLog(maximumHarvests, harvestsInMonth);

    if (logarithmicResult > 0.75 && logarithmicResult <= 1) {
        return 4;
    } else if (logarithmicResult > 0.50 && logarithmicResult <= 0.75) {
        return 3;
    } else if (logarithmicResult > 0.25 && logarithmicResult <= 0.50) {
        return 2;
    } else if (logarithmicResult > 0 && logarithmicResult <= 0.25) {
        return 1;
    } 

    return 0;
}


/**
 * The following function returns the logarithm of y with base x, ie. logx(y):
 */
function getBaseLog(x, y) {
    return Math.log(y) / Math.log(x);
}

