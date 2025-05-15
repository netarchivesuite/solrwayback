import Chart from 'chart.js'

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
  }
}