<template>
  <div class="modalContainer">
    <button class="closeButton" @click="closeModal()">
      ✕
    </button>
    <div :class="currentModal === 'gpssearch' ? 'modalContent gpssearch' : 'modalContent'">
      <search-guidelines v-if="currentModal === 'guidelines'" />
      <search-visualization v-if="currentModal === 'visualization'" />
      <gps-search v-if="currentModal === 'gpssearch'" />
      <collection-info v-if="currentModal === 'collectioninfo'" />
    </div>
  </div>
</template>

<script>

import SearchGuidelines from './SearchGuidelines'
import SearchVisualization from './SearchVisualization'
import GpsSearch from './GpsSearch'
import CollectionInfo from './CollectionInfo'
import { mapState, mapActions } from 'vuex'

export default {
  name: 'PrimaryModal',
  components: {
    SearchGuidelines,
    SearchVisualization,
    GpsSearch,
    CollectionInfo
  },
  computed: {
    ...mapState({
      showModal: state => state.Modal.showModal,
      currentModal: state => state.Modal.currentModal,
    })
  },
  methods: {
       ...mapActions('Modal', {
      updateShowModal:'updateShowModal',
      updateCurrentModal:'updateCurrentModal'
    }),
    closeModal() {
      this.updateShowModal(!this.showModal)
      this.updateCurrentModal('')
    },
  }
  
}
</script>