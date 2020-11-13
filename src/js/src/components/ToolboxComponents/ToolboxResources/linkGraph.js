import * as d3 from 'd3'

//action to take on mouse click

function click() {
  d3.select(this).select('text').transition()
      .duration(750)
      .attr('x', 22)
      .style('fill', 'steelblue')
      .style('stroke', 'lightsteelblue')
      .style('stroke-width', '.5px')
      .style('font', '40px sans-serif')
  d3.select(this).select('circle').transition()
      .duration(750)
      .attr('r', 16)
      .style('fill', 'lightsteelblue')
}

//action to take on mouse double click
function dblclick(min, max) {
var newDomain= d3.select(this).select('text').text()
var min = $('#rangeslider').dateRangeSlider('min').getTime()
var max = $('#rangeslider').dateRangeSlider('max').getTime()
location.href='/solrwayback/waybacklinkgraph.jsp?domain='+newDomain+'&facetLimit=14&ingoing=true&dateStart='+min+'&dateEnd='+max
}

var width =  1920, height = 1000
var svg = d3.select('#graphContainer').append('svg')   
  .attr('width', width)
  .attr('height', height)
  .call(d3.behavior.zoom().on('zoom', function () {
  svg.attr('transform', 'translate(' + d3.event.translate + ')' + ' scale(' + d3.event.scale + ')')
}))
.append('g')

svg.append('defs').selectAll('marker')
.data(['suit', 'licensing', 'resolved'])
.enter().append('marker')
.attr('id', function(d) { return d })
.attr('viewBox', '0 -5 10 10')
.attr('refX', 25)
.attr('refY', 0)
.attr('markerWidth', 6)
.attr('markerHeight', 6)
.attr('orient', 'auto')
.append('path')
.attr('d', 'M0,-5L10,0L0,5 L10,0 L0, -5')
.style('stroke', '#4679BD')
.style('opacity', '0.6')

var force = d3.layout.force()
  .gravity(.05)
  .distance(100)
  .charge(-100)
  .size([width, height])

var serviceUrl='services/waybacklinkgraph'+getServiceParameters()

d3.json(serviceUrl, function(json) {
force
    .nodes(json.nodes)
    .links(json.links)
    .start()

var link = svg.selectAll('.link')
    .data(json.links)
  .enter().append('line')
    .attr('class', 'link')
    .style('marker-end',  'url(#suit)') // Modified line 
  .style('stroke-width', function(d) { return Math.sqrt(d.weight) })

var node = svg.selectAll('.node')
    .data(json.nodes)
    .enter().append('g')
    .attr('class', 'node') 
    .on('click', click)
    .on('dblclick', dblclick)
    .call(force.drag)

node.append('circle')
    .attr('r',function(d){return d.size})
    .style('fill', function(d){return d.color})

node.append('text')
    .attr('dx', 12)
    .attr('dy', '.35em')
    .text(function(d) { return d.name })

force.on('tick', function() {
  link.attr('x1', function(d) { return d.source.x })
      .attr('y1', function(d) { return d.source.y })
      .attr('x2', function(d) { return d.target.x })
      .attr('y2', function(d) { return d.target.y })

  node.attr('transform', function(d) { return 'translate(' + d.x + ',' + d.y + ')' })
})
})

function getServiceParameters(){
  var ingoingCheckbox = document.getElementById('ingoingCheckbox')
  var facets =  document.getElementById('fader').value
  var checked=ingoingCheckbox.checked      
  var min=  $('#rangeslider').dateRangeSlider('min').getTime()
  var max=  $('#rangeslider').dateRangeSlider('max').getTime()
  
  var serviceParameters='?domain=flickr.com&facetLimit='+facets+'&ingoing='+checked+'&dateStart='+ min+'&dateEnd='+ max
  return serviceParameters
}
