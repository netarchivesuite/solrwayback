<template>
  <div class="modalContainer">
    <button class="closeButton" @click="closeModal()">
      ✕
    </button>
    <div :class="currentModal === 'gpssearch' ? 'modalContent gpssearch' : 'modalContent'">
      <search-guidelines v-if="currentModal === 'guidelines'" />
      <search-visualization v-if="currentModal === 'visualization'" />
      <gps-search v-if="currentModal === 'gpssearch'" />
    </div>
  </div>
</template>

<script>

import SearchGuidelines from './SearchGuidelines'
import SearchVisualization from './SearchVisualization'
import GpsSearch from './GpsSearch'
import { mapState, mapActions } from 'vuex'

export default {
  name: 'PrimaryModal',
  components: {
    SearchGuidelines,
    SearchVisualization,
    GpsSearch
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