import Chart from 'chart.js'

export default {
  drawChart: function(chartLabels, sizeInKb, numberOfPages, ingoingLinks) {
    var domainGrowthChart = new Chart(document.getElementById('line-chart'), {
        type: 'line',
        data: {
            labels: chartLabels,
            datasets: [
                {
                    data: sizeInKb,
                    label: 'Size in Kilobytes',
                    yAxisID: 'kilobytes',
                    borderColor: '#0066cc',
                    fill: false,
                },
                {
                    data: numberOfPages,
                    label: 'Number of pages',
                    yAxisID: 'totalpages',
                    borderColor: '#cc0000',
                    fill: false
                },
                {
                    data: ingoingLinks,
                    label: 'Incoming links',
                    fontColor: 'black',
                    yAxisID: 'links',
                    borderColor: '#009900',
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
                            fontColor: '#0066cc',
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
                            fontColor: '#cc0000',
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
                            fontColor: '#009900',
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