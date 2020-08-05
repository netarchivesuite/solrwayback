<template>
<form @submit.prevent="handleSubmit">
        
          <input id="query"
                 v-model="query"
                 type="text"
                 class=""
                 placeholder="Enter search term">
          <button type="submit" class="">
            Search
          </button>
       
    </form>
</template> 

   <script>

// import { mapState, mapActions } from 'vuex'

import {SearchService} from '../services/search-service'



export default {
  components: {
    
  },
  data () {
    return {
      query: '',
     
    }
  },
  computed: {
   
  },
  mounted () {
    
  },
  
  methods: {
  

    handleSubmit (e) {
      this.query = e.target.query.value
      if (this.query) {
          SearchService.search(this.query)

       console.log(this.query)
      }
    }
  }
}

</script>

    