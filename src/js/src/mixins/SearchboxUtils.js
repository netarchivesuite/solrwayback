export default {
  methods: {
    $_getSizeOfTextArea(id) {
      let textarea = document.getElementById(id)
      textarea.style.height = '1px'
      textarea.style.height = textarea.scrollHeight + 'px'
    },
  }
}
