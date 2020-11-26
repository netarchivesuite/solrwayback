export default {
  methods: {
    $_getSizeOfTextArea(id) {
      this.$refs[id].style.height = '1px'
      this.$refs[id].style.height = this.$refs[id].scrollHeight + 'px'
    },
  }
}
