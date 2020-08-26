<template>
  <div class="SingleEntryStandardInfo">
    <p class="scoreInfo">
      score: <span class="highlightText">{{ result.score }}</span>
    </p>
    <p class="entryInfo">
      <span class="highlightText titleInfo">
        <a :href="getPlaybackURL(result.source_file_path, result.source_file_offset)" target="_blank"><span>{{ result.title || 'No title' }}</span></a>
      </span>
    </p><p class="entryInfo type">
      <span class="attri">type:</span> <span class="val">{{ result.content_type_norm }}, {{ result.type }} @ {{ result.domain }}</span>
    </p>
    <p class="entryInfo date">
      <span class="attri">date:</span> <span class="val">{{ refactoredDate(result.wayback_date) }}</span>
    </p>
    <p class="entryInfo url">
      <span class="attri">url:</span> <span class="val">{{ result.url }}</span>
    </p>
    <div v-if="result.highlight.content">
      <hr class="informationDivider">
      <p class="highlightText entryInfo">
        Content:
      </p>
      <div class="snippetContainer">
        <span>"</span>
        <p class="snippetHighlight" v-html="result.highlight.content[0]" />
        <span>"</span>
      </div>
    </div>
  </div>
</template>

<script>
import configs from '../../configs'

export default {
  name: 'SearchSingleItemStandardInfo',
  props: {
    result: {
      type: Object,
      required: true
    }
  },
  methods: {
    refactoredDate(date) {
      date = date.toString()
      return date.substring(6,8) + '/' + date.substring(4,6) + '-' + date.substring(0,4)
    },
    getPlaybackURL(source_file_path, source_file_offset) {
      return `${configs.playbackConfig.solrwaybackBaseURL}services/viewForward?source_file_path=${source_file_path}&offset=${source_file_offset}`    
    }
  }
}

</script>

    
