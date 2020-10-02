<template>
  <div class="aboutContainer">
    test
  </div>
</template>

<script>

import { mapState, mapActions } from 'vuex'

export default {
  name: 'AboutComponent',
  
  computed: {
    ...mapState({
    }),
  },
  mounted () {
  },
 
  methods: {
    ...mapActions('Search', {
    }),
  }
}

</script>

    
