<template>
  <div class="SingleResultEntry">
    {{ result.content_type }} // {{ result.type }}
    <br>
    {{ result }}
  </div>
</template>

<script>
//import { mapState, mapActions } from 'vuex'

export default {
  name: "SingleEntry",
  components: {  
  },
  props: {
    result: {
      type: Object,
      required: true
    },
  },
  data () {
    return {     
    }
  },
  computed: {
  },
  mounted () {
  },
  
  methods: {
  }
}

</script>

    
