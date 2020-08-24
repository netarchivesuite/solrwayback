<template>
  <div class="searchSingleItemFocusImage" @click="closeImage">
    <img :src="image" @click.stop>
    <button @click="closeImage">
      ✕
    </button>
  </div>
</template>

<script>
//import { mapState, mapActions } from 'vuex'

export default {
  name: 'SearchSingleItemFocusImage',
  components: {  
  },
  props: {
    image: {
      type: String,
      required: true
    },
    index: {
      type: Number,
      required: true
    }
  },
  data () {
    return {     
      imageShown:false,
    }
  },
  methods: {
    closeImage() {
      console.log('WE EMITTING')
      this.$emit('close-window', this.index)
    },
  }
}
</script>

    
