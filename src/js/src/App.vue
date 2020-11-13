<template>
  <div id="app">
    <!-- En enforce component SolrWayback update when moving from '/' to '/search' -->
    <router-view :key="$route.path" />
  </div>
</template>
<script>
export default {
    watch: {
      $route: {
        immediate: true,
        handler(to, from) {
               document.title = to.meta.title || 'SolrWayback'
            }
      }
    }
}
  </script>  

