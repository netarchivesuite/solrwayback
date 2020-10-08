<template>
  <div class="toolboxContainer">
    <button class="toolboxCloseButton" @click="closeToolbox">
      ✕
    </button>
    <div class="toolboxContent">
      <div class="toolboxNavigation">
        <h3>Available tools: </h3>
        <button>Wordcloud</button>
        <button>Link graph</button>
        <button>Domain stats</button>
        <hr>
      </div>
      <wordcloud v-if="currentTool === 'wordcloud'" />
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import Wordcloud from './ToolboxComponents/Wordcloud'

export default {
  name: 'Toolbox',
  components: {
    Wordcloud
  },
  data() {
    return {
      currentTool:'wordcloud'
    }
  },
  methods: {
    closeToolbox() {
      this.$emit('close-toolbox', this.index)
    },
  }
  
}
</script>
