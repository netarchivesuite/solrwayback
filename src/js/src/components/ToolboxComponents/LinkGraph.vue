<template>
  <div id="linkGraphContainer" class="linkGraphContainer">
    <div id="graphControlsContainer" class="graphControlsContainer">
      <h2 class="toolboxHeadline">
        Link graph
      </h2>
      <div class="linkGraphSettings">
        <div class="linkGraphDomainContainer contain">
          <input v-model="domain"
                 placeholder="Enter domain, like 'kb.dk'"
                 :class="$_checkDomain(domain) ? '' : 'urlNotTrue'"
                 @keyup.enter="!loading ? loadLinkGraph(domain) : null">
        </div>
        <div class="generateButtonContainer contain">
          <button :disabled="loading" class="linkGraphButton" @click.prevent="loadLinkGraph(domain)">
            Generate
          </button>
        </div>
        <div class="linkNumberContainer contain">
          <label class="linkGraphLabel">Number of links:</label>
          <div class="linkNumbersliderContainer">
            <vue-slider v-model="linkNumber"
                        tooltip="always"
                        :min="0"
                        :max="25" />
          </div>
        </div>
        <div class="directionContainer contain">
          <label class="linkGraphLabel label">Link direction:</label>
          <input id="linkGraphRadioOne"
                 v-model="ingoing"
                 type="radio"
                 value="true">
          <label class="label" for="linkGraphRadioTwo">Ingoing</label>
          <input id="linkGraphRadioTwo"
                 v-model="ingoing"
                 type="radio"
                 value="false">
          <label class="label" for="linkGraphRadioTwo">Outgoing</label>
        </div> 
      </div>
      <div class="sliderContainer">
        <label class="linkGraphLabel">Timeframe:</label>
        <vue-slider v-model="sliderValues"
                    tooltip="always"
                    :min="minValue"
                    :max="maxValue"
                    :interval="1"
                    :tooltip-formatter="getDate" />
      </div>
      <hr class="informationDivider">
    </div>
    <div v-if="loading" class="spinner" />
    <div id="graphContainer" :class="loading ? 'hideGraph' : ''" />
  </div>
</template>

<script>
import VueSlider from 'vue-slider-component'
import 'vue-slider-component/theme/default.css'
import StringManipulationUtils from './../../mixins/StringManipulationUtils'
import { requestService } from '../../services/RequestService'
import * as d3 from 'd3'

export default {
  name: 'LinkGraph',
  components: {
    VueSlider
  },
  mixins: [StringManipulationUtils],
  data() {
    return {
      maxValue:new Date().getTime(),
      minValue:788914800000,
      sliderValues:[788914800000,new Date().getTime()],
      getDate: v => `${this.createDateFromNumber(v)}`,
      linkNumber:10,
      ingoing:false,
      loading:false,
      domain:''
    }
  },
  mounted () {
    window.addEventListener('resize', function() {
     let height = document.getElementById('linkGraphContainer').offsetHeight - document.getElementById('graphControlsContainer').offsetHeight
     document.getElementById('svgDiagram').setAttribute('height', height + 'px')
    })
    const routerQuery = this.$router.history.current.query
    if(routerQuery.domain && routerQuery.dateStart && routerQuery.dateEnd && routerQuery.facetLimit && routerQuery.ingoing) {
      routerQuery.domain ? this.domain = routerQuery.domain : null
      routerQuery.dateStart ? this.sliderValues[0] = parseInt(routerQuery.dateStart) : null
      routerQuery.dateEnd ? this.sliderValues[1] = parseInt(routerQuery.dateEnd) : null
      routerQuery.facetLimit ? this.linkNumber = parseInt(routerQuery.facetLimit) : null
      routerQuery.ingoing 
        ? routerQuery.ingoing === 'true' 
          ? this.ingoing = true 
          : this.ingoing = false 
        : null
      this.loading = true
      requestService.getLinkGraph(this.domain,this.linkNumber, this.ingoing, this.sliderValues[0], this.sliderValues[1]).then(result => (this.buildSvg(result)), error => console.log('Error, no link graph created.'))
    }
  },
  methods: {
    loadLinkGraph(domain) {
      this.loading = true
      requestService.getLinkGraph(this.domain,this.linkNumber, this.ingoing, this.sliderValues[0], this.sliderValues[1]).then(result => (this.buildSvg(result)), error => console.log('Error, no link graph created.'))
    },
    createDateFromNumber(date) {
      let time = new Date(date)
      let day = time.getDay()
      let month = time.getMonth() >= 9 ? ((+time.getMonth()) + (+1)) : '0' + ((+time.getMonth()) + (+1))
      let year = time.getFullYear()
      return day + '-' + month + '/' + year
    },
    buildSvg(result) {
      document.getElementById('graphContainer').innerHTML = ''
      let _this = this
      var width =  document.getElementById('graphContainer').offsetWidth, height = document.getElementById('graphContainer').offsetHeight
      var svg = d3.select('#graphContainer').append('svg')  
        .attr('id', 'svgDiagram')
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
      .style('stroke', '#002E70')
      .style('opacity', '1')

      let force = d3.layout.force()
        .gravity(.05)
        .distance(100)
        .charge(-100)
        .size([width, height])

      let drag = force.drag().on('dragstart', function() { d3.event.sourceEvent.stopPropagation() })

      force.nodes(result.nodes)
           .links(result.links)
           .start()
      let link = svg.selectAll('.link')
          .data(result.links)
        .enter().append('line')
          .attr('class', 'link')
          .style('marker-end',  'url(#suit)') // Modified line 
        .style('stroke-width', function(d) { return Math.sqrt(d.weight) })

      let node = svg.selectAll('.node')
          .data(result.nodes)
          .enter().append('g')
          .attr('class', 'node') 
          .on('click', function() {
            if(d3.select(this).select('circle').attr('r') === '16') {
              d3.select(this).select('text').transition()
                .duration(750)
                .attr('x', null)
                .style('fill', 'black')
                .style('stroke', 'null')
                .style('stroke-width', '1px')
                .style('font', '16px "Trebuchet MS", Ubuntu')
              if(d3.select(this).select('circle').attr('style') !== 'fill: red;') {
                d3.select(this).select('circle').transition()
                  .duration(750)
                  .attr('r', 5)
                  .style('fill', 'black')
                }
            }
            else {
              d3.select(this).select('text').transition()
                .duration(750)
                .attr('x', 22)
                .style('fill', '#002E70')
                .style('stroke', 'null')
                .style('stroke-width', '.5px')
                .style('font', '40px sans-serif')
              if(d3.select(this).select('circle').attr('style') !== 'fill: red;') {
                d3.select(this).select('circle').transition()
                  .duration(750)
                  .attr('r', 16)
                  .style('fill', ' #002E70')
              }
            }
          })
          .on('dblclick', function() {
            let domain = d3.select(this).select('text').text()
            requestService.getLinkGraph(domain,_this.linkNumber, _this.ingoing, _this.sliderValues[0], _this.sliderValues[1])
            .then(result => (_this.buildSvg(result)), error => console.log('Error, no link graph created.'))
            console.log(requestService)
          })
          .call(drag)

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
      this.loading = false
    },
  }
}
</script>