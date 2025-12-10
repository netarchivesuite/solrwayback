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
import { getQueryHistoryCount, downloadHistory } from '../utils/queryHistoryTracker'

/**
 * QueryHistoryButton Component
 * 
 * Provides a UI button for downloading the current session's query history.
 * Uses utility functions from queryHistoryTracker for all functionality.
 */
export default {
  name: 'QueryHistoryButton',
  
  data() {
    return {
      historyCount: 0
    }
  },
  
  mounted() {
    // Initialize history count (with error handling)
    try {
      this.updateHistoryCount()
      
      // Listen for storage events to update count
      if (typeof window !== 'undefined') {
        window.addEventListener('storage', this.updateHistoryCount)
        window.addEventListener('queryHistoryUpdated', this.updateHistoryCount)
      }
    } catch (e) {
      console.warn('Query history button initialization failed:', e)
      this.historyCount = 0
    }
  },
  
  beforeUnmount() {
    if (typeof window !== 'undefined') {
      window.removeEventListener('storage', this.updateHistoryCount)
      window.removeEventListener('queryHistoryUpdated', this.updateHistoryCount)
    }
  },
  
  methods: {
    /**
     * Update the history count from session storage
     */
    updateHistoryCount() {
      try {
        this.historyCount = getQueryHistoryCount()
      } catch (e) {
        console.warn('Failed to get query history count:', e)
        this.historyCount = 0
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
     * Handle the download button click
     */
    handleDownload() {
      if (this.historyCount === 0) {
        return
      }
      
      try {
        // Call the download function from the utility module
        downloadHistory()
        
        // Optionally show a notification
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
  margin-left: 10px;
  margin-right: 0px;
  padding: 0px;
  float: right;
  margin-top: -4px;
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
