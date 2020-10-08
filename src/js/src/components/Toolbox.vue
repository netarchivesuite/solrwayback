<template>
  <div class="toolboxContainer">
    <button class="toolboxCloseButton" @click="closeToolbox">
      ✕
    </button>
    <div class="toolboxContent">
      <div class="toolboxNavigation">
        <h3>Available tools: </h3>
        <button :class="currentTool === 'wordcloud' ? 'activeTool' : ''" @click="setCurrentTool('wordcloud')">
          Wordcloud
        </button>
        <button :class="currentTool === 'linkgraph' ? 'activeTool' : ''" @click="setCurrentTool('linkgraph')">
          Link graph
        </button>
        <button :class="currentTool === 'domainstats' ? 'activeTool' : ''" @click="setCurrentTool('domainstats')">
          Domain stats
        </button>
        <hr>
      </div>
      <wordcloud v-if="currentTool === 'wordcloud'" />
      <link-graph v-if="currentTool === 'linkgraph'" />
      <domain-stats v-if="currentTool === 'domainstats'" />
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import Wordcloud from './ToolboxComponents/Wordcloud'
import LinkGraph from './ToolboxComponents/LinkGraph'
import DomainStats from './ToolboxComponents/DomainStats'


export default {
  name: 'Toolbox',
  components: {
    Wordcloud, 
    LinkGraph, 
    DomainStats
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
    setCurrentTool(tool) {
      this.currentTool = tool
    }
  }
  
}
</script>
