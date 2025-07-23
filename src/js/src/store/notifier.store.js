// Global notifier state
import { defineStore } from 'pinia';
import * as Vue from 'vue';

export const useNotifierStore = defineStore('notifier', {

  state: () => ({
    notifications: []
  }),

  // import Vue from 'vue'

  // const initialState = () => ({
  //   notifications: [],
    
  // })
  
  // const state = initialState()
  
  actions: {
    setNotification( notification) {
      // commit('setNotification', notification)
      this.notifications.push(notification)
    },
    dismissNotification ( notification) {
      // commit('dismissNotification', notification)
      // TODO - fix / find out what this does
      // Vue.delete(this.notifications, notification.__ob__.vmCount)
      this.dispatch('Search/setLoadingStatus', false)
    },
    resetState(){
      this.$reset()
    }
  }

})
  
  // const mutations = {
   
  //   setNotification(state, notification) {
  //     state.notifications.push(notification)
  //   },

  //   dismissNotification(state, notification) {
  //     Vue.delete(state.notifications, notification.__ob__.vmCount)
  //     this.dispatch('Search/setLoadingStatus', false)
  //   },
    
  //   resetState(state) {
  //     const newState = initialState()
  //     Object.keys(newState).forEach(key => {
  //           state[key] = newState[key]
  //     })
  //   },
  // }
  
  // export default {
  //   namespaced: true,
  //   state,
  //   actions,
  //   mutations
  // }
  