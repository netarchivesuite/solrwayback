<template>
  <div class="modalContainer">
    <button class="closeButton" @click="closeModal()">
      ✕
    </button>
    <div class="modalContent">
      <search-guidelines v-if="currentModal === 'guidelines'" />
    </div>
  </div>
</template>

<script>

import SearchGuidelines from './SearchGuidelines'

export default {
  name: 'PrimaryModal',
  components: {
    SearchGuidelines
  },
  props: {
    currentModal: {
      type:String,
      required:true
    },
  },
  data() {
    return {
    }
  },
  methods: {
    closeModal() {
      this.$emit('close-modal')
    },
  }
  
}
</script>