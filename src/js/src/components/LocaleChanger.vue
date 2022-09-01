<template>
  <div class="localeChanger">
    <div class="localeChangerContainer">
      <a href="javascript:void(0);" @click.prevent="toggleLang">
        {{ $t('solrWayback:toggle_language') }}: <span v-html="lang"></span>
      </a>
    </div>
  </div>
</template>
<script>
export default {
  name: 'LocaleChanger',
  data() {
    return { pl: true, lang: 'en' }
  },
  methods: {
    toggleLang() {
      this.pl = !this.pl
      if (this.pl) {
        this.lang = 'en'
        this.$i18n.locale = 'pl'
      } else {
        this.lang = 'pl'
        this.$i18n.locale = 'en'
      }
    },
  },
}
</script>