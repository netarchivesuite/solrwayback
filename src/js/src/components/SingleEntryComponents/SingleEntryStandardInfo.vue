<template>
  <div class="SingleEntryStandardInfo">
    {{ result.content_type}} / {{ result.content_type_norm}}
    {{ result }}  
  </div>
</template>

<script>
//import { mapState, mapActions } from 'vuex'

export default {
  name: "SingleEntryStandardInfo",
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

    
