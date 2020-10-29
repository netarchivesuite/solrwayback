

<template>
  <div class="GpsSearchContainer">
    <div :class="getMapSize()">
      <div class="gpsControls">
        <span>Longitude:</span>
        <input v-model="longitude"
               placeholder="Longitude">
        <span>Latitude:</span>
        <input v-model="latitude"
               placeholder="Latitude">
        <span>Radius in KM:</span>
        <input v-model="radius"
               placeholder="Radius">
        <span>Query:</span>
        <input v-model="imgQuery"
               placeholder="Search term">       
        <button :disabled="longitude === 0 || longitude === '' || latitude === 0 || latitude === ''" class="searchButton" @click="doGeoSearch()">
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
      <image-search-results v-if="imgResults.searchType === 'geoImage'" :img-results="imgResults" />
    </div>
  </div>
</template>

<style lang="scss">
@import '../../../node_modules/leaflet/dist/leaflet.css';
@import '../../../node_modules/leaflet.markercluster/dist/MarkerCluster.Default.css';
</style>

<script>

import L from 'leaflet'
import icon from '../../../node_modules/leaflet/dist/images/marker-icon.png'
import iconShadow from '../../../node_modules/leaflet/dist/images/marker-shadow.png'
import Markercluster from '../../../node_modules/leaflet.markercluster/dist/leaflet.markercluster.js'
import ImageSearchResults from '../searchResults/ImageSearchResults'
import SearchUtils from './../../mixins/SearchUtils'
import { mapState, mapActions } from 'vuex'
import { requestService } from '../../services/RequestService'

export default {
  name: 'GpsSearch',
  components: {
    ImageSearchResults
  },
  mixins: [SearchUtils],
  data: () => ({
    longitude:0,
    latitude:0,
    radius:50,
    imgQuery:'*:*',
    searchMap:null,
    selected:null,
    resultSize:'hidden',
    mapSize:'full',
    imgResults:{},
    imageLayer:null,
  }),
  computed: {
    latLngRad() {
      return `${this.latitude}|${this.longitude}|${this.radius}`
    }
  },
  watch: {
    latLngRad(newVal, oldVal) {
      this.setNewSearchArea()
    }
  },
  mounted () {
    this.createMap()
  },
  methods: {
    ...mapActions('Search', {
      setLoadingStatus:'setLoadingStatus'
    }),
    createMap() {
        this.searchMap = L.map('gpsMap', null, { zoomControl: false }).setView([56.1572, 10.2107], 7)
        //If https problems occur, try https://a.tile.openstreetmap.org/{z}/{x}/{y}.png instead.
        //Old access point: https://{s}.tile.osm.org/{z}/{x}/{y}.png
        L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a>'
      }).addTo(this.searchMap)
      const defaultIcon = L.icon({
        iconSize: [25, 41],
        iconAnchor: [12.5, 25],
        //popupAnchor: [0, -30],
        iconUrl: icon,
        shadowUrl: iconShadow
      })
      L.Marker.prototype.options.icon = defaultIcon
      const resizeObserver = new ResizeObserver(() => {
        this.searchMap.invalidateSize()
      })
      resizeObserver.observe(document.getElementById('gpsMap'))
      
      this.selected = L.featureGroup().addTo(this.searchMap)
      this.searchMap.on('click', this.setNewSearchArea)
    },
    doGeoSearch() {
      this.setLoadingStatus(true)
      requestService.fireGeoImageSearchRequest(this.imgQuery,this.latitude,this.longitude,this.radius)
      .then(result => (this.imgResults = result.response,this.mapSize = 'half', this.resultSize = 'half', this.plotImagesOnMap(this.imgResults.images), this.setLoadingStatus(false)), error => (console.log('Error in seaching for images by location.'), this.setLoadingStatus(false)))
      //this.requestGeoImageSearch({query:this.imgQuery,latitude:this.latitude,longitude: this.longitude,radius: this.radius}).then(this.mapSize = 'half', this.resultSize = 'half',this.recalculateMap())
    },
    setNewSearchArea(e) {
      this.selected.clearLayers()
      if(e !== undefined) {
      this.longitude = e.latlng.wrap().lng.toFixed(6)
      this.latitude = e.latlng.wrap().lat.toFixed(6)
      }
      L.circle( {lat:this.latitude, lng:this.longitude}, {
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
    plotImagesOnMap(images) {
      this.imageLayer !== null ? this.searchMap.removeLayer(this.imageLayer) : null
      this.imageLayer = L.markerClusterGroup()
      images.forEach((item, index) => {
        let newMarker = L.marker(new L.LatLng(item.latitude,item.longitude), { title:item.urlNorm } )
        newMarker.bindPopup(`<span title="${item.resourceName}">${item.resourceName}</span><img src="${item.imageUrl}&height=200&width=200" />`)
        this.imageLayer.addLayer(newMarker)
      })	
      this.searchMap.addLayer(this.imageLayer)
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
    }
    
  }
}

</script>

    
