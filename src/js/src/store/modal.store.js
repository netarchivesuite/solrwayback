// Global modal state
import { defineStore } from 'pinia';

export const useModalStore = defineStore('modal', {

  state: () => ({
    showModal: false,
    currentModal: ''
  }),

  actions: {
    updateShowModal(shown) {
      // this.updateShowModalSuccess(shown)
      this.showModal = shown
    },
    updateCurrentModal(modal){
      // this.updateCurrentModalSuccess(modal)
      this.currentModal = modal
    },
    resetState(){
      this.$reset()
    }
    // updateShowModalSuccess(state, shown){
    //   state.showModal = shown
    // },
    // updateCurrentModalSuccess(state, modal) {
    //   state.currentModal = modal
    // },
    // resetState(currentState) {
    //   currentState.showModal = this.showModal
    //   currentState.currentModal = this.currentModal
    // }

  }
})
