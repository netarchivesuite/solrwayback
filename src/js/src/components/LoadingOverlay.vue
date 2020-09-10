<template>
  <div v-if="loading" class="loadingScreen">
    <div class="spinner">
      <span class="lookingGlass" />
      <div v-for="row in rows" :key="row" class="row">
        <span v-for="(number, index) in getLine(lineOptions, row)"
              :key="index"
              :style="'width: calc(' + number + '% - 6px);'"
              :class="Math.random() > .2 ? 'lines' : 'lines highlighted'" />
      </div>
    </div>
  </div>
</template>
<script>
import { mapState, mapActions } from 'vuex'

export default {
  name: 'LoadingOverlay',
  data() {
    return {
      rows:10,
      lineOptions:[[10,5,5,15,10,10,30,15],[ 20,15,10,5,5,5,10,30], [5,10,15,10,5,20,15,20]]
    }
  },
  computed: {
    ...mapState({
      loading: state => state.Search.loading,
    })
  },
  methods: {
    getLine(lineOptions, number) {
      let lines = [...lineOptions[number % 3]]
      return lines.sort(() => Math.random() - 0.5)
    }
  }
}
</script>
