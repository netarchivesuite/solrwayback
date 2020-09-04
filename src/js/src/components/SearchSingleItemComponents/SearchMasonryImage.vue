<template>
  <div class="columnImageContainer">
    <img 
      loading="lazy"
      :src="result.downloadUrl">
    <!--<div class="imageButtonContainer">
      {{ number }}
      <button>Search for image</button>
      <button>Pages linking to image</button> -->
  </div>
  </div>
</template>

<script>
export default {
    name: 'SearchMasonryImage',
  components: {  
  },
  props: {
    result: {
      type: Object,
      required: true
    },
    number: {
      type: Number,
      required: true
    },
  },
  data () {
    return {     
    }
  },
  methods: {
  }
}
</script>