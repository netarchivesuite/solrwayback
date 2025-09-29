// chartsCore/chartEngines/LineChart.js
import { Chart } from 'chart.js';

// Optional global plugin: fill background white before drawing
Chart.plugins.register({
  beforeDraw: function(chart) {
    const ctx = chart.chart.ctx;
    ctx.save();
    ctx.globalCompositeOperation = 'destination-over';
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, chart.width, chart.height);
    ctx.restore();
  }
});

// Keep track of chart instances so we can destroy/reuse
const chartInstances = {};

/**
 * Draws a line chart with one or more datasets on a canvas using Chart.js v2.
 * @param {string} elementId - ID of the canvas element.
 * @param {Array<string>} chartLabels - X-axis labels.
 * @param {Array<Object>} datasets - Array of dataset objects: { data, label, borderColor }
 * @param {string} yAxisLabel - Label for the y-axis.
 * @returns {Chart} - Chart.js instance
 */
export function drawSingleLineChart(elementId, chartLabels, datasets, yAxisLabel = 'Count') {
  const ctx = document.getElementById(elementId);
  if (!ctx) return null;

  // Destroy previous chart if exists
  if (chartInstances[elementId]) {
    chartInstances[elementId].destroy();
  }

  chartInstances[elementId] = new Chart(ctx, {
    type: 'line',
    data: {
      labels: chartLabels,
      datasets: datasets.map(ds => ({
        data: ds.data,
        label: ds.label,
        borderColor: ds.borderColor || 'blue',
        fill: false
      }))
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      title: {
        display: true,
        text: ''
      },
      legend: {
        display: true
      },
      scales: {
        yAxes: [{
          ticks: { beginAtZero: true },
          scaleLabel: {
            display: !!yAxisLabel,
            labelString: yAxisLabel
          }
        }],
        xAxes: [{
          scaleLabel: {
            display: true,
            labelString: 'X'
          }
        }]
      }
    }
  });

  return chartInstances[elementId];
}
