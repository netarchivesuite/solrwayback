<template>
  <div class="modalContainer">
    <button class="closeButton" @click="closeModal()">
      ✕
    </button>
    <div class="modalContent">
      <search-guidelines v-if="currentModal === 'guidelines'" />
    </div>
  </div>
</template>

<script>

import SearchGuidelines from './SearchGuidelines'
import { mapState, mapActions } from 'vuex'

export default {
  name: 'PrimaryModal',
  components: {
    SearchGuidelines
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