import Vue from "vue";
import Vuex from "vuex";
import { searchStore } from './modules/SearchStore'


Vue.use(Vuex);

export default new Vuex.Store({
  state: {},
  mutations: {},
  actions: {},
  modules: {
    searchStore,
  },
  strict: true

});
