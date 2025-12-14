<template>
  <button 
    class="queryHistoryButton" 
    :disabled="historyCount === 0"
    :title="getButtonTitle()"
    @click.prevent="handleDownload">
    <span class="queryHistoryText">Query History</span>
    <span class="queryHistoryIcon" />
    <span v-if="historyCount > 0" class="historyCount">{{ historyCount }}</span>
  </button>
</template>

<script>
/**
 * QueryHistoryButton Component
 * 
 * Provides a UI button for downloading the current session's query history.
 * All tracking is done server-side; this component primarily calls the backend APIs.
 */
export default {
  name: 'QueryHistoryButton',
  
  data() {
    return {
      historyCount: 0,
      pollInterval: null
    }
  },
  
  mounted() {
    this.updateHistoryCount()
    
    // Check for count updates every 2 seconds
    this.pollInterval = setInterval(() => {
      this.updateHistoryCount()
    }, 2000)
  },
  
  beforeUnmount() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval)
    }
  },
  
  methods: {
    /**
     * Fetch history count from server. Used to update button state.
     */
    async updateHistoryCount() {
      try {
        const response = await fetch(`${window.location.origin}/solrwayback/services/queryhistory/count`, {
          credentials: 'same-origin'
        })
        if (response.ok) {
          const data = await response.json()
          this.historyCount = data.count || 0
        }
      } catch (e) {
        console.warn('Failed to get query history count:', e)
      }
    },
    
    /**
     * Get the button title based on history count
     */
    getButtonTitle() {
      if (this.historyCount === 0) {
        return 'No query history available for this session'
      }
      return `Download query history (${this.historyCount} ${this.historyCount === 1 ? 'entry' : 'entries'})`
    },
    
    /**
     * Download history file from server
     */
    async handleDownload() {
      if (this.historyCount === 0) {
        return
      }
      
      try {
        const response = await fetch(`${window.location.origin}/solrwayback/services/queryhistory/download`, {
          credentials: 'same-origin'
        })
        
        if (!response.ok) {
          throw new Error('Failed to download history')
        }
        
        const blob = await response.blob()
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = 'query_history.json'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        
        this.$emit('history-downloaded', this.historyCount)
      } catch (e) {
        console.error('Failed to download query history:', e)
      }
    }
  }
}
</script>

<style scoped>
.queryHistoryButton {
  position: relative;
  background-color: white;
  border: 0px;
  cursor: pointer;
  text-transform: uppercase;
  color: var(--main-highlight-color);
  padding: 0px;
}

.queryHistoryButton:disabled {
  cursor: not-allowed;
  color: #696969;
  opacity: 0.6;
}

.queryHistoryText {
  border-bottom: 1px solid var(--secondary-highlight-color);
  margin-right: 5px;
  display: inline-block;
}

.queryHistoryIcon {
  background-image: url('../assets/icons/download.svg');
  height: 16px;
  position: relative;
  width: 16px;
  margin-right: 0px;
  display: inline-block;
  background-repeat: no-repeat;
  background-size: cover;
  background-position: center;
  top: 3px;
  border-bottom: 0px;
}

.historyCount {
  position: absolute;
  top: -8px;
  right: -8px;
  background-color: #e74c3c;
  color: white;
  border-radius: 10px;
  padding: 2px 5px;
  font-size: 10px;
  font-weight: bold;
  min-width: 16px;
  text-align: center;
  line-height: 1.2;
}
</style>
