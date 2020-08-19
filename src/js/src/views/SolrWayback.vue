<template>
  <div class="contentContainer">
    <h1>Solr<span>Wayback</span></h1>
    <search-box />
    <search-result />
    <!--<router-link class="aboutLink" to="/about">Om Solrwayback search</router-link> -->
  </div>
</template>

<script>
 import SearchBox from '../components/SearchBox'
 import SearchResult from '../components/SearchResult'

export default {
  name: 'SolrWayback',
   components: {
   SearchBox,
   SearchResult
  },
}
</script>
