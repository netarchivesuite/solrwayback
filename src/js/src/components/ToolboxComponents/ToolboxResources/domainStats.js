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

export default {

  drawChart: function(chartLabels, sizeInKb, numberOfPages, ingoingLinks, textSize) {
    var colorOne = '#fd7f6f'
    var colorTwo = '#7eb0d5'
    var colorThree = '#b2e061'
    var colorFour = '#bd7ebe'


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

  drawIndividualCharts: function (chartLabels, sizeInKb, numberOfPages, ingoingLinks, textSize) {
    const colors = {
      sizeInKb: '#fd7f6f',
      numberOfPages: '#7eb0d5',
      ingoingLinks: '#b2e061',
      textSize: '#bd7ebe',
    }

    // Size in KB Chart
    new Chart(document.getElementById('size-chart'), {
      type: 'line',
      data: {
        labels: chartLabels,
        datasets: [
          {
            data: sizeInKb,
            label: 'Size in kilobytes',
            borderColor: colors.sizeInKb,
            fill: false,
          },
        ],
      },
      options: {
        title: {
          display: true,
          text: 'Size in Kilobytes',
        },
        scales: {
          yAxes: [
            {
              ticks: {
                beginAtZero: true,
              },
              scaleLabel: {
                display: true,
                labelString: 'Size (KB)',
              },
            },
          ],
        },
      },
    })

    // Number of Pages Chart
    new Chart(document.getElementById('pages-chart'), {
      type: 'line',
      data: {
        labels: chartLabels,
        datasets: [
          {
            data: numberOfPages,
            label: 'Number of Pages',
            borderColor: colors.numberOfPages,
            fill: false,
          },
        ],
      },
      options: {
        title: {
          display: true,
          text: 'Number of Pages',
        },
        scales: {
          yAxes: [
            {
              ticks: {
                beginAtZero: true,
              },
              scaleLabel: {
                display: true,
                labelString: 'Pages',
              },
            },
          ],
        },
      },
    })

    // Ingoing Links Chart
    new Chart(document.getElementById('links-chart'), {
      type: 'line',
      data: {
        labels: chartLabels,
        datasets: [
          {
            data: ingoingLinks,
            label: 'Ingoing Links',
            borderColor: colors.ingoingLinks,
            fill: false,
          },
        ],
      },
      options: {
        title: {
          display: true,
          text: 'Ingoing Links',
        },
        scales: {
          yAxes: [
            {
              ticks: {
                beginAtZero: true,
              },
              scaleLabel: {
                display: true,
                labelString: 'Links',
              },
            },
          ],
        },
      },
    })

    // Text Size Chart
    new Chart(document.getElementById('textsize-chart'), {
      type: 'line',
      data: {
        labels: chartLabels,
        datasets: [
          {
            data: textSize,
            label: 'Average Page Size (Characters)',
            borderColor: colors.textSize,
            fill: false,
          },
        ],
      },
      options: {
        title: {
          display: true,
          text: 'Average Page Size (Characters)',
        },
        scales: {
          yAxes: [
            {
              ticks: {
                beginAtZero: true,
              },
              scaleLabel: {
                display: true,
                labelString: 'Characters',
              },
            },
          ],
        },
      },
    })
  },
}