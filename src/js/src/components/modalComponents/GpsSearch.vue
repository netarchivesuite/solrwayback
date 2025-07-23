

<template>
  <div class="GpsSearchContainer">
    <div :class="getMapSize()">
      <div class="gpsControls">
        <span>Longitude:</span>
        <input v-model="longitude"
               placeholder="Longitude"
               @keyup.enter="doGeoSearch()">
        <span>Latitude:</span>
        <input v-model="latitude"
               placeholder="Latitude"
               @keyup.enter="doGeoSearch()">
        <span>Radius in KM:</span>
        <input v-model="radius"
               placeholder="Radius"
               @keyup.enter="doGeoSearch()">
        <span>Query:</span>
        <input v-model="imgQuery"
               placeholder="Search term"
               @keyup.enter="doGeoSearch()">       
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

<script>

import L from 'leaflet'
import icon from '../../../node_modules/leaflet/dist/images/marker-icon.png'
import iconShadow from '../../../node_modules/leaflet/dist/images/marker-shadow.png'
import * as Markercluster from '../../../node_modules/leaflet.markercluster/dist/leaflet.markercluster.js'
import ImageSearchResults from '../searchResults/ImageSearchResults.vue'
import SearchUtils from './../../mixins/SearchUtils'
// import { mapState, mapActions } from 'vuex'
import { mapActions } from 'pinia'
import { useSearchStore } from '../../store/search.store.js'
import { useNotifierStore } from '../../store/notifier.store.js'
import { requestService } from '../../services/RequestService'
import configs from '../../configs'

export default {
  name: 'GpsSearch',
  components: {
    ImageSearchResults
  },
  mixins: [SearchUtils],
  data: () => ({
    longitude: 0,
    latitude: 0,
    radius: 50,
    imgQuery:'*:*',
    searchMap:null,
    selected:null,
    resultSize:'hidden',
    mapSize:'full',
    imgResults:{},
    imageLayer:null,
    maxImagesReturned:500
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
    this.longitude = configs.leaflet.map.longitude
    this.latitude = configs.leaflet.map.latitude
    this.radius = configs.leaflet.map.radius / 1000
    this.createMap()
  },
  methods: {
    ...mapActions(useSearchStore, {
      setLoadingStatus:'setLoadingStatus'
    }),
    ...mapActions(useNotifierStore, {
      setNotification: 'setNotification'
     
    }),
    createMap() {
        this.searchMap = L.map('gpsMap', null, { zoomControl: false }).setView([this.latitude, this.longitude], 7)
        //If https problems occur, try https://a.tile.openstreetmap.org/{z}/{x}/{y}.png instead.
        //Old access point: https://{s}.tile.osm.org/{z}/{x}/{y}.png
        L.tileLayer(configs.leaflet.source || 'http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
        attribution: configs.leaflet.attribution || '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a>'
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
      this.imgResults = {}
      requestService.fireGeoImageSearchRequest(this.imgQuery,this.latitude,this.longitude,this.radius)
      .then(result => (this.imgResults = result.response, this.checkForResultNumber(result.response.images.length), this.setScreenOnSuccessfullResult()),
            error => (console.log('Error in seaching for images by location.'),this.setServerSideErrorNotification(), this.setLoadingStatus(false)))
    },
    checkForResultNumber(imageNumber) {
      if(imageNumber >= this.maxImagesReturned) {
        this.setNotification({
          	title: 'Max limit images reached',
            text: 'The maximum of 500 images has been found. Decrease radius to narrow the search further.',
            type: 'error',
            timeout: true
          })
      }
    },
    setServerSideErrorNotification() {
      this.setNotification({
          	title: 'We are so sorry!',
            text: 'Something went wrong when searching - If this persists, contact an administrator.',
            type: 'error',
            timeout: false
      })
    },
    setScreenOnSuccessfullResult() {
      this.mapSize = 'half'
      this.resultSize = 'half'
      this.plotImagesOnMap(this.imgResults.images)
      this.setLoadingStatus(false)
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
      let position = ''
      if(this.mapSize === 'full') {
        position = 'end'
      }
      else if(this.resultSize === 'full') {
        position = 'start'
      }
      else {
        position = 'half'
      }
      return 'dividerButtons ' + position
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

<style lang="scss">
  @import '../../../node_modules/leaflet/dist/leaflet.css';
  @import '../../../node_modules/leaflet.markercluster/dist/MarkerCluster.Default.css';
  </style>
  

    
