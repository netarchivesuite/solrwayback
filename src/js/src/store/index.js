import Vue from 'vue'
import Vuex from 'vuex'
//import { searchStore } from './modules/search.store'
import modules from './modules'


Vue.use(Vuex)

//We need strict mode only when developing (debugging purposes)
const debug = process.env.NODE_ENV !== 'production'
console.log(modules.Search)
export default new Vuex.Store({
  modules,
  strict: debug

})
