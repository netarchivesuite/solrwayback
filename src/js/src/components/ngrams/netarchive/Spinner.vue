<template>
  <div v-if="loading" class="spinner">
    LOADING...
  </div>
</template>
<script>
import { mapState} from 'vuex'

export default {
  name: 'Spinner',
  computed: {
    ...mapState({
      loading: state => state.Ngram.loading,
    })
  }
}
</script>
