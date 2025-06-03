import Chart from 'chart.js'

Chart.plugins.register({
  beforeDraw: function(chart) {
    const ctx = chart.chart.ctx
    ctx.save()
    ctx.globalCompositeOperation = 'destination-over'
    ctx.fillStyle = '#fff'
    ctx.fillRect(0, 0, chart.width, chart.height)
    ctx.restore()
  }
})

const colorOne = '#fd7f6f'
const colorTwo = '#7eb0d5'
const colorThree = '#b2e061'
const colorFour = '#bd7ebe'

export default {

  /**
   * Draws a multi-axis line chart displaying all available domain statistics over time.
   *
   * @function
   * @param {Array<string>} chartLabels - Labels for the x-axis (typically dates or time periods).
   * @param {Array<number>} sizeInKb - Data points for size in kilobytes.
   * @param {Array<number>} numberOfPages - Data points for number of pages.
   * @param {Array<number>} ingoingLinks - Data points for ingoing links.
   * @param {Array<number>} textSize - Data points for average page size in characters.
   */
  drawCombinedChart: function(chartLabels, sizeInKb, numberOfPages, ingoingLinks, textSize) {
    var domainGrowthChart = new Chart(document.getElementById('line-chart'), {
        type: 'line',
        data: {
            labels: chartLabels,
            datasets: [
                {
                    data: sizeInKb,
                    label: 'Size in kilobytes',
                    yAxisID: 'kilobytes',
                    borderColor: colorOne,
                    fill: false,
                },
                {
                    data: numberOfPages,
                    label: 'Pages',
                    yAxisID: 'totalpages',
                    borderColor: colorTwo,
                    fill: false
                },
                {
                    data: ingoingLinks,
                    label: 'Ingoing links',
                    fontColor: 'black',
                    yAxisID: 'links',
                    borderColor: colorThree,
                    fill: false
                },
                {
                    data: textSize,
                    label: 'Average page size (In characters)',
                    fontColor: 'black',
                    yAxisID: 'text',
                    borderColor: colorFour,
                    fill: false
                }
            ]
        },
        options: {
            title: {
                display: true,
            },
            scales: {
                yAxes: [
                    {
                        id: 'kilobytes',
                        ticks: {
                            beginAtZero: true,
                            //maxTicksLimit: 5,
                            suggestedMax: 10
                        },
                        scaleLabel: {
                            display: true,
                            labelString: 'Size in kilobytes',
                            fontColor: colorOne,
                        }
                    },
                    {
                        id: 'totalpages',
                        ticks: {
                            beginAtZero: true,
                            //maxTicksLimit: 5,
                            suggestedMax: 10
                        },
                        scaleLabel: {
                            display: true,
                            labelString: 'Pages',
                            fontColor: colorTwo,
                        },
                        gridLines : {
                            display : true,
                            borderDash: [2,4]
                        }
                    },
                    {
                        id: 'links',
                        ticks: {
                            beginAtZero: true,
                            //maxTicksLimit: 5,
                            suggestedMax: 10
                        },
                        scaleLabel: {
                            display: true,
                            labelString: 'Ingoing links',
                            fontColor: colorThree,
                        },
                        gridLines : {
                            display : true,
                            borderDash: [2,4]
                        }
                    },
                    {
                        id: 'text',
                        ticks: {
                            beginAtZero: true,
                            //maxTicksLimit: 5,
                            suggestedMax: 10
                        },
                        scaleLabel: {
                            display: true,
                            labelString: 'Average page size (In characters)',
                            fontColor: colorFour,
                        },
                        gridLines : {
                            display : true,
                            borderDash: [2,4]
                        }
                    }
                ]
            },
            legend: {
                labels: {
                    fontColor: 'black',
                },
                onClick: function(event, legendItem) {
                    var index = legendItem.datasetIndex
                    //toggle the datasets visibility
                    domainGrowthChart.data.datasets[index].hidden = !domainGrowthChart.data.datasets[index].hidden
                    //toggle the related labels' visibility
                    domainGrowthChart.options.scales.yAxes[index].display = !domainGrowthChart.options.scales.yAxes[index].display
                    domainGrowthChart.update()
                }
            }
        }
      }) 
  },

  /**
   * Draws four individual line charts for each domain metric: size in KB, number of pages, ingoing links, and text size.
   *
   * @function
   * @param {Array<string>} chartLabels - Labels for the x-axis (typically dates or time periods).
   * @param {Array<number>} sizeInKb - Data points for size in kilobytes.
   * @param {Array<number>} numberOfPages - Data points for number of pages.
   * @param {Array<number>} ingoingLinks - Data points for ingoing links.
   * @param {Array<number>} textSize - Data points for average page size in characters.
   * @returns {void}
   */
  drawIndividualCharts: function (chartLabels, sizeInKb, numberOfPages, ingoingLinks, textSize) {
    // Size in KB Chart
    this.drawSingleLineChart('size-chart', chartLabels, sizeInKb, 'Size in Kilobytes', colorOne, 'Size (KB)')

    // Number of Pages Chart
    this.drawSingleLineChart('pages-chart', chartLabels, numberOfPages, 'Number of Pages', colorTwo, 'Pages')
    
    // Ingoing Links Chart
    this.drawSingleLineChart('links-chart', chartLabels, ingoingLinks, 'Ingoing Links', colorThree, 'Links')
    
    // Text Size Chart
    this.drawSingleLineChart('textsize-chart', chartLabels, textSize, 'Average Page Size (Characters)', colorFour, 'Characters')
  },

  /**
 * Draws a single line chart on the specified canvas element.
 *
 * This function creates a new Chart.js line chart using the provided data and configuration.
 * It is used to visualize a single metric (such as size, pages, links, or text size) over time.
 *
 * @param {string} elementId - The ID of the canvas element where the chart will be rendered.
 * @param {Array<string>} chartLabels - The labels for the x-axis (typically dates or time periods).
 * @param {Array<number>} dataPoints - The data points to plot on the chart.
 * @param {string} label - The label for the dataset (displayed in the chart legend and title).
 * @param {string} color - The color of the line in the chart (CSS color string).
 * @param {string} yAxisLabel - The label for the y-axis.
 */
  drawSingleLineChart: function (elementId, chartLabels, dataPoints, label, color, yAxisLabel) {
    new Chart(document.getElementById(elementId), {
      type: 'line',
      data: {
        labels: chartLabels,
        datasets: [
          {
            data: dataPoints,
            label: label,
            borderColor: color,
            fill: false,
          },
        ],
      },
      options: {
        title: {
          display: true,
          text: label,
        },
        scales: {
          yAxes: [
            {
              ticks: {
                beginAtZero: true,
              },
              scaleLabel: {
                display: true,
                labelString: yAxisLabel,
              },
            },
          ],
        },
      },
    })
  }
}