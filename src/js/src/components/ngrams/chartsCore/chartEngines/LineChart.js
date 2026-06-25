// chartsCore/chartEngines/LineChart.js
import { Chart } from 'chart.js';

// Keep track of chart instances so we can destroy/reuse
const chartInstances = {};

/**
 * Draws a line chart with one or more datasets on a canvas using Chart.js v2.
 * @param {string} elementId - ID of the canvas element.
 * @param {Array<string>} chartLabels - X-axis labels.
 * @param {Array<Object>} datasets - Array of dataset objects: { data, label, borderColor }
 * @param {Object} chartOptions - Chart.js options object.
 * @param {Array<string>} rawLabels - Raw labels used for click callbacks.
 * @returns {Chart} - Chart.js instance
 */
export function drawSingleLineChart(elementId, chartLabels, datasets, chartOptions = {}, rawLabels = chartLabels) {
  const canvas = document.getElementById(elementId);
  if (!canvas) return null;

  // Destroy previous chart if exists
  if (chartInstances[elementId]) {
    chartInstances[elementId].destroy();
  }

  const defaultOptions = {
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
          display: true,
          labelString: 'Count'
        }
      }],
      xAxes: [{
        scaleLabel: {
          display: true,
          labelString: 'X'
        }
      }]
    }
  };

  const mergedOptions = {
    ...defaultOptions,
    ...chartOptions,
    scales: chartOptions.scales || defaultOptions.scales,
  };

  chartInstances[elementId] = new Chart(canvas, {
    type: 'line',
    data: {
      labels: chartLabels,
      rawLabels,
      datasets: datasets.map(ds => ({
        ...ds,
        borderColor: ds.borderColor || 'blue',
        fill: ds.fill === undefined ? false : ds.fill
      }))
    },
    options: {
      ...mergedOptions
    }
  });

  return chartInstances[elementId];
}

export function clearChart(elementId) {
  if (chartInstances[elementId]) {
    chartInstances[elementId].destroy();
    delete chartInstances[elementId];
  }
}
