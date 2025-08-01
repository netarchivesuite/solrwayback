<template>
  <div ref="imgBox"
       class="searchSingleItemFocusImage"
       tabindex="0"
       @click="closeImage"
       @keyup.esc="closeImage">
    <img :src="image" @click.stop>
    <button @click="closeImage">
      ✕
    </button>
  </div>
</template>

<script>


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
  mounted() {
    this.$refs.imgBox.focus()
},
  methods: {
    closeImage() {
      this.$emit('close-window', this.index)
    },
  }
}
</script>

    
