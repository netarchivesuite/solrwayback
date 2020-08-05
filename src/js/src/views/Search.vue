<template>
  <div class="search">
       <h1>SolrWayback Search</h1>

Her skal Zahra, Jesper og Jørn lave en hel masse....

<search-box/>
 <router-link to="/about">Om Solrwayback search</router-link>
  </div>
</template>

<script>
 import SearchBox from "../components/SearchBox"
export default {
 


  name: "Search",


   components: {
   SearchBox
  },
 
};
</script>
