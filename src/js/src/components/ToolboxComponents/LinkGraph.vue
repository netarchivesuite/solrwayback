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
          <label class="linkGraphLabel">Max. node degree:</label>
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
        <label class="linkGraphLabel">Time frame:</label>
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
import configs from './../../configs'

export default {
  name: 'LinkGraph',
  components: {
    VueSlider
  },
  mixins: [StringManipulationUtils],
  data() {
    return {
      sliderValues:[new Date(configs.visualizations.ngram.startYear,0,1).getTime(),new Date().getTime()],
      maxValue:new Date().getTime(),
      minValue:new Date(configs.visualizations.ngram.startYear,0,1).getTime(),
      getDate: v => `${this.createDateFromNumber(v)}`,
      linkNumber:25,
      ingoing:false,
      loading:false,
      domain:'',
      highlighted:false
    }
  },
  mounted () {
    window.addEventListener('resize', function() {
      if(document.getElementById('svgDiagram') !== null) {
        let height = document.getElementById('linkGraphContainer').offsetHeight - document.getElementById('graphControlsContainer').offsetHeight
        document.getElementById('svgDiagram').setAttribute('height', height + 'px')
      }
    })
    const routerQuery = this.$router.history.current.query
    if(routerQuery.domain) {
      routerQuery.domain ? this.domain = routerQuery.domain : null
      routerQuery.dateStart ? this.sliderValues[0] = parseInt(routerQuery.dateStart) : null
      routerQuery.dateEnd ? this.sliderValues[1] = parseInt(routerQuery.dateEnd) : null
      routerQuery.facetLimit ? this.linkNumber = parseInt(routerQuery.facetLimit) : null
      if(routerQuery.ingoing) {
        routerQuery.ingoing === 'true' ? this.ingoing = true : this.ingoing = false
      }
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
      const time = new Date(date)
      // get the date - and if it's below 9, add a 0 before it (so we get 04 fx)
      const day = time.getDate() >= 9 ? time.getDate() : '0' + (+time.getDate())
      // get the month, add 1 to it (because javascript, lol) and add a 0 before it (so we get 04/08 fx)
      const month = time.getMonth() >= 9 ? ((+time.getMonth()) + (+1)) : '0' + ((+time.getMonth()) + (+1))
      // get the year - which is a nice method.
      const year = time.getFullYear()
      return day + '-' + month + '/' + year
    },
    mouseoverNode(node) {
      d3.select(node).select('text').transition()
                .duration(300)
                .attr('x', 10)
                .style('fill', '#002E70')
                .style('stroke', 'null')
                .style('stroke-width', '.5px')
                .style('font', '30px sans-serif')
              if(d3.select(node).select('circle').attr('style') !== 'fill: red;') {
                d3.select(node).select('circle').transition()
                  .duration(300)
                  .attr('r', 15)
                  .style('fill', ' #002E70')
              }
    },
    highlightNode(node) {
      d3.select(node).select('text').transition()
                .duration(300)
                .attr('x', 10)
                .style('fill', '#002E70')
                .style('stroke', 'null')
                .style('stroke-width', '.5px')
                .style('font', '40px sans-serif')
              if(d3.select(node).select('circle').attr('style') !== 'fill: red;') {
                d3.select(node).select('circle').transition()
                  .duration(300)
                  .attr('r', 16)
                  .style('fill', ' #002E70')
              }
    },
    normalNode(node) {
      d3.select(node).select('text').transition()
                .duration(300)
                .attr('x', null)
                .style('fill', 'black')
                .style('stroke', 'null')
                .style('stroke-width', '1px')
                .style('font', '16px "Trebuchet MS", Ubuntu')
              if(d3.select(node).select('circle').attr('style') !== 'fill: red;') {
                d3.select(node).select('circle').transition()
                  .duration(300)
                  .attr('r', 5)
                  .style('fill', 'black')
                }
    },
    irrelevantNode(node) {
      d3.select(node).select('text').transition()
                .duration(300)
                .attr('x', null)
                .style('fill', '#ddd')
                .style('stroke', 'null')
                .style('stroke-width', '1px')
                .style('font', '16px "Trebuchet MS", Ubuntu')
              if(d3.select(node).select('circle').attr('style') !== 'fill: red;') {
                d3.select(node).select('circle').transition()
                  .duration(300)
                  .attr('r', 5)
                  .style('fill', '#ddd')
                }
    },
    buildSvg(result) {
      document.getElementById('graphContainer').innerHTML = ''
      let _this = this
      let width =  document.getElementById('graphContainer').offsetWidth, height = document.getElementById('graphContainer').offsetHeight
      let svg = d3.select('#graphContainer').append('svg')  
        .attr('id', 'svgDiagram')
        .attr('width', width)
        .attr('height', height)
        .call(d3.behavior.zoom().on('zoom', function () {
        svg.attr('transform', 'translate(' + d3.event.translate + ')' + ' scale(' + d3.event.scale + ')')
      }))
      .on('click', function() {
        if(event.defaultPrevented) { return }
        if(_this.highlighted === true) {
          result.links.forEach((item, index) => {
            let point1 = d3.select('#point-' + item.target.group)
            let point2 = d3.select('#point-' + item.source.group)
            let link = d3.select('#link-' + index)
            _this.normalNode(point1[0][0])
            _this.normalNode(point2[0][0])
            link.attr('class', 'link')
          })
        }
      })
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
        .distance(300)
        .charge(-300)
        .size([width, height])

      let startDrag = force.drag().on('dragstart', function() { d3.event.sourceEvent.stopPropagation() })

      force.nodes(result.nodes)
           .links(result.links)
           .start()
      let link = svg.selectAll('.link')
          .data(result.links)
          .enter().append('line')
          .attr('class', 'link active')
          .attr('id', function(d, i) { return 'link-' + i })
          .style('marker-end',  'url(#suit)') // Modified line 
          .style('stroke-width', function(d) { return Math.sqrt(d.weight) })

      let node = svg.selectAll('.node')
          .data(result.nodes)
          .enter().append('g')
          .attr('class', 'node')
          .attr('id', function(d) { return 'point-' + d.group })
          .attr('data-name', function(d) { return d.name })
          .call(startDrag)
          .on('mouseover', function() {
             _this.highlighted ? null : _this.mouseoverNode(this)
          })
          .on('mouseout', function() {
             _this.highlighted ? null : _this.normalNode(this)
          })
          .on('click', function() {
            if(event.defaultPrevented) { return }
            event.stopPropagation()
            let highlight = d3.select(this).select('circle').attr('r') === '16'
            highlight ? _this.highlighted = false : _this.highlighted = true
            let prime = d3.select(this)[0][0].dataset.name
            let connected = []
            let nonConnected = []
            result.links.forEach((item, index) => {
              let point = d3.select('#point-' + item.target.group)
              let link = d3.select('#link-' + index)
                if(item.source.name === prime  || item.target.name === prime) {
                  item.number = index
                  connected.push(item)
                }
                else {
                  item.number = index
                  nonConnected.push(item)
                }
            })
            nonConnected.forEach((item) => {
              let point1 = d3.select('#point-' + item.target.group)
              let point2 = d3.select('#point-' + item.source.group)
              let link = d3.select('#link-' + item.number)
              highlight ? _this.normalNode(point1[0][0]) : _this.irrelevantNode(point1[0][0])
              highlight ? _this.normalNode(point2[0][0]) : _this.irrelevantNode(point2[0][0])
              highlight ? link.attr('class', 'link') : link.attr('class', 'link inactive')
            })
            connected.forEach((item) => {
              let point1 = d3.select('#point-' + item.target.group)
              let point2 = d3.select('#point-' + item.source.group)
              let link = d3.select('#link-' + item.number)
              highlight ? _this.normalNode(point1[0][0]) : _this.highlightNode(point1[0][0])
              highlight ? _this.normalNode(point2[0][0]) : _this.highlightNode(point2[0][0])
              highlight ? link.attr('class', 'link') : link.attr('class', 'link')
            })
          })
          .on('dblclick', function() {
            let domain = d3.select(this).select('text').text()
            document.getElementById('graphContainer').innerHTML = ''
            this.loading = true
            requestService.getLinkGraph(domain,_this.linkNumber, _this.ingoing, _this.sliderValues[0], _this.sliderValues[1])
            .then(result => (_this.buildSvg(result)), error => console.log('Error, no link graph created.'))
          })

      node.append('circle')
          .attr('r',function(d){return d.size})
          .style('fill', function(d){return d.color})

      node.append('text')
          .attr('dx', 5)
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