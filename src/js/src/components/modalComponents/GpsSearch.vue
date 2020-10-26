

<template>
  <div class="GpsSearchContainer">
    <div :class="getMapSize()">
      <div class="gpsControls">
        <span>Longtitude:</span>
        <input v-model="longtitude"
               placeholder="Longtitude">
        <span>Latitude:</span>
        <input v-model="latitude"
               placeholder="Latitude">
        <span>Radius in KM:</span>
        <input v-model="radius"
               placeholder="Radius">
        <button class="searchButton">
          Search
        </button>
      </div>
      <div id="gpsMap" />
      <div :class="dividerPosition()">
        <button v-if="resultSize !== 'full'" class="dividerButton" @click="changeView('back')">
          «
        </button>
        <button v-if="mapSize !== 'full'" class="dividerButton" @click="changeView('forward')">
          »
        </button>
      </div>
    </div>
    <div :class="getResultSize()">
      <image-search-results v-if="results.searchType === 'image'" />
    </div>
  </div>
</template>

<script>

import L from 'leaflet'
import '../../../node_modules/leaflet/dist/leaflet.css'
import ImageSearchResults from '../searchResults/ImageSearchResults'
import { mapState } from 'vuex'


export default {
  name: 'GpsSearch',
  components: {
    ImageSearchResults
  },
  data: () => ({
    longtitude:0,
    latitude:0,
    radius:50,
    searchMap:null,
    selected:null,
    resultSize:'hidden',
    mapSize:'full',
  }),
  computed: {
    ...mapState({
      results: state => state.Search.results,
    }),
  },
  mounted () {
    console.log(L)
    this.createMap()
  },
  methods: {
    createMap() {
        console.log(L)
        this.searchMap = L.map('gpsMap', null, { zoomControl: false }).setView([56.1572, 10.2107], 7)
        //If https problems occur, try https://a.tile.openstreetmap.org/{z}/{x}/{y}.png instead.
        //Old access point: https://{s}.tile.osm.org/{z}/{x}/{y}.png
        L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a>'
      }).addTo(this.searchMap)
      this.selected = L.featureGroup().addTo(this.searchMap)
      this.searchMap.on('click', this.setNewSearchArea)
    },
    setNewSearchArea(e) {
      this.selected.clearLayers()
      L.circle( e.latlng, {
      color: '#002E70',
      weight:'1',
      fillColor: '#002E70',
      fillOpacity: 0.2,
      radius: this.radius * 1000
      }).addTo(this.selected)
    },
    getMapSize() {
      return 'gpsMapContainer ' + this.mapSize
    },
    getResultSize() {
      return 'gpsResultsContainer ' + this.resultSize
    },
    dividerPosition() {
      let decision = ''
      if(this.mapSize === 'full') {
        decision = 'end'
      }
      else if(this.resultSize === 'full') {
        decision = 'start'
      }
      else {
        decision = 'half'
      }
      return 'dividerButtons ' + decision
    },
    changeView(direction) {
      if(direction === 'forward') {
        switch(this.mapSize) {
          case 'hidden': this.mapSize = 'half'
                         this.resultSize = 'half'
                         break
          case 'half' :  this.mapSize = 'full'
                         this.resultSize = 'hidden'
                         break
        }
      }
      else {
        switch(this.mapSize) {
          case 'full':   this.mapSize = 'half'
                         this.resultSize = 'half'
                         break
          case 'half' :  this.mapSize = 'hidden'
                         this.resultSize = 'full'
                         break
        }
      }
      let callbackmap = this.searchMap
      //Horrible, horrible hack to have the map revalidate the size 
      //Just to make sure that it's aligned to the size of the users screen, and the right tiles are loaded.
      // https://stackoverflow.com/questions/24412325/resizing-a-leaflet-map-on-container-resize
      setTimeout(function() { callbackmap.invalidateSize() }, 200)
    }
    
  }
}

</script>

    
