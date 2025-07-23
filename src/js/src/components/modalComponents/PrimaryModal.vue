<template>
  <div ref="modalContainer"
       class="modalContainer"
       tabindex="0"
       @keyup.esc="closeModal">
    <button class="closeButton" @click="closeModal()">
      ✕
    </button>
    <div :class="this.modalStore.currentModal === 'gpssearch' ? 'modalContent gpssearch' : 'modalContent'">
      <search-guidelines v-if="this.modalStore.currentModal === 'guidelines'" />
      <search-visualization v-if="this.modalStore.currentModal === 'visualization'" />
      <gps-search v-if="this.modalStore.currentModal === 'gpssearch'" />
      <collection-info v-if="this.modalStore.currentModal === 'collectioninfo'" />
    </div>
  </div>
</template>

<script>

import SearchGuidelines from './SearchGuidelines.vue'
// import SearchVisualization from './SearchVisualization.vue'
import GpsSearch from './GpsSearch.vue'
import CollectionInfo from './CollectionInfo.vue'
// import { mapState, mapActions } from 'vuex'
import { mapStores, mapActions } from 'pinia'
import { useModalStore } from '../../store/modal.store'

export default {
  name: 'PrimaryModal',
  components: {
    SearchGuidelines,
    // SearchVisualization,
    GpsSearch,
    CollectionInfo
  },
  computed: {
    // ...mapState({
    //   showModal: state => state.Modal.showModal,
    //   currentModal: state => state.Modal.currentModal,
    // })
    ...mapStores(useModalStore)
  },
  mounted() {
    this.$refs.modalContainer.focus()
},
  methods: {
       ...mapActions(useModalStore, {
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