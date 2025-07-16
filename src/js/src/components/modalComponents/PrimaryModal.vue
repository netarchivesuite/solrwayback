<template>
  <div ref="modalContainer"
       class="modalContainer"
       tabindex="0"
       @keyup.esc="closeModal">
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

import SearchGuidelines from './SearchGuidelines.vue'
import SearchVisualization from './SearchVisualization.vue'
import GpsSearch from './GpsSearch.vue'
import CollectionInfo from './CollectionInfo.vue'
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
  mounted() {
    this.$refs.modalContainer.focus()
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