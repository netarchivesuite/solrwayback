// Global modal state
import { defineStore } from 'pinia'

export const useModalStore = defineStore('modal', {

  state: () => ({
    showModal: false,
    currentModal: ''
  }),

  actions: {
    updateShowModal(shown) {
      this.showModal = shown
    },
    updateCurrentModal(modal){
      this.currentModal = modal
    },
    resetState(){
      this.$reset()
    }
  }
})
