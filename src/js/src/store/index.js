import Vue from 'vue'
import Vuex from 'vuex'
import modules from './modules'


Vue.use(Vuex)

//We need strict mode only when developing (debugging purposes)
const debug = process.env.NODE_ENV !== 'production'
export default new Vuex.Store({
  modules,
  strict: debug

})
