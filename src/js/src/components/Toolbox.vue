<template>
  <div class="toolboxContainer">
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
        <button :class="currentTool === 'htmltags' ? 'activeTool' : ''" @click="setCurrentTool('htmltags')">
          HTML-tags
        </button>
        <button :class="currentTool === 'gephiexport' ? 'activeTool' : ''" @click="setCurrentTool('gephiexport')">
          Link graph Gephi export
        </button>
        <button :class="currentTool === 'smurf' ? 'activeTool' : ''" @click="navigateTo('ngram')">
          Smurf
        </button>
        <hr>
      </div>
      <wordcloud v-if="currentTool === 'wordcloud'" />
      <link-graph v-if="currentTool === 'linkgraph'" />
      <domain-stats v-if="currentTool === 'domainstats'" />
      <html-tags v-if="currentTool === 'htmltags'" />
      <gephi-export v-if="currentTool === 'gephiexport'" />
      <smurf v-if="currentTool === 'smurf'" />
    </div>
  </div>
</template>

<script>
import Wordcloud from './ToolboxComponents/Wordcloud'
import LinkGraph from './ToolboxComponents/LinkGraph'
import DomainStats from './ToolboxComponents/DomainStats'
import HtmlTags from './ToolboxComponents/HtmlTags'
import GephiExport from './ToolboxComponents/GephiExport'
import Smurf from './ToolboxComponents/Smurf'


export default {
  name: 'Toolbox',
  components: {
    Wordcloud, 
    LinkGraph, 
    DomainStats,
    HtmlTags,
    GephiExport,
    Smurf,
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
    },
    navigateTo(destination) {
      this.$router.push(destination)
    }
  }
  
}
</script>
