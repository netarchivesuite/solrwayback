// Global modal state

const initialState = () => ({
  showModal: false,
  currentModal:''
  
})

const state = initialState()

const actions = {
  updateShowModal( {commit}, shown) {
    commit('updateShowModalSuccess', shown)
  },
  updateCurrentModal ( {commit}, modal) {
    commit('updateCurrentModalSuccess', modal)
  },
  resetState({ commit }) {
    commit('resetState')
  }
}

const mutations = {
 
  updateShowModalSuccess(state, shown) {
    state.showModal = shown
  },

  updateCurrentModalSuccess(state, modal) {
    state.currentModal = modal
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
