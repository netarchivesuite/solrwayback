// Global notifier state

  import Vue from 'vue'

  const initialState = () => ({
    notifications: [],
    
  })
  
  const state = initialState()
  
  const actions = {
    setNotification( {commit}, notification) {
      commit('setNotification', notification)
    },
    dismissNotification ( {commit}, notification) {
      commit('dismissNotification', notification)
    },
    resetState({ commit }) {
      commit('resetState')
    }
  }
  
  const mutations = {
   
    setNotification(state, notification) {
      if(notification === undefined) {
        state.notifications.push(notification)
      }
      else {
        state.notifications.push(notification)
      }
    },

    dismissNotification(state, notification) {
      Vue.delete(state.notifications, notification.__ob__.vmCount)
      this.dispatch('Search/setLoadingStatus', false)
    },
    
    resetState(state) {
      const newState = initialState()
      Object.keys(newState).forEach(key => {
            state[key] = newState[key]
      })
    },
  }
  
  export default {
    namespaced: true,
    state,
    actions,
    mutations
  }
  