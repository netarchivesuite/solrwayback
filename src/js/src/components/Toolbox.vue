<template>
  <div ref="toolboxContainer"
       class="toolboxContainer"
       tabindex="0"
       @keyup.esc="closeToolbox">
    <button class="toolboxCloseButton" @click="closeToolbox">
      ✕
    </button>
    <div class="toolboxContent">
      <div class="toolboxNavigation">
        <h3>
          <span class="toolboxIcon" />
          Available tools:
        </h3>
        <button :class="currentTool === 'wordcloud' ? 'activeTool' : ''" @click="setCurrentTool('wordcloud')">
          Wordcloud
        </button>
        <button :class="currentTool === 'linkgraph' ? 'activeTool' : ''" @click="setCurrentTool('linkgraph')">
          Link graph
        </button>
        <button :class="currentTool === 'domainstats' ? 'activeTool' : ''" @click="setCurrentTool('domainstats')">
          Domain stats
        </button>
        <button :class="currentTool === 'gephiexport' ? 'activeTool' : ''" @click="setCurrentTool('gephiexport')">
          Link graph Gephi export
        </button>
        <button :class="currentTool === 'ngramnetarchive' ? 'activeTool' : ''" @click="setCurrentTool('ngramnetarchive')">
          Ngram Netarchive 
        </button>
        <hr>
      </div>
      <wordcloud v-if="currentTool === 'wordcloud'" />
      <link-graph v-if="currentTool === 'linkgraph'" />
      <domain-stats v-if="currentTool === 'domainstats'" />
      <gephi-export v-if="currentTool === 'gephiexport'" />
      <ngram-netarchive v-if="currentTool === 'ngramnetarchive'" />
    </div>
  </div>
</template>

<script>
import Wordcloud from './ToolboxComponents/Wordcloud'
import LinkGraph from './ToolboxComponents/LinkGraph'
import DomainStats from './ToolboxComponents/DomainStats'
import GephiExport from './ToolboxComponents/GephiExport'
import NgramNetarchive from './ToolboxComponents/NgramNetarchive'


export default {
  name: 'Toolbox',
  components: {
    Wordcloud, 
    LinkGraph, 
    DomainStats,
    GephiExport,
    NgramNetarchive
   
  },
  data() {
    return {
      currentTool:'wordcloud'
    }
  },
  mounted() {
    this.$refs.toolboxContainer.focus()
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
