<template>
  <div class="notification"
       :class="notification.type">
    <button class="notificationCloseBtn"
            type="button"
            @click="doDismiss(notification)">
      X
    </button>
    <h2 v-if="notification.title">
      {{ notification.title }}
    </h2>
    <p>
      {{ notification.text }}
    </p>
  </div>
</template>

<script>
import { mapState, mapActions } from 'vuex'

export default {
  name: 'NotificationItem',
 
  props: {
    notification: {
      type: Object,
      required: true,
      validator: function (obj) {
      return 'type' in obj &&
            'title' in obj &&
            'text' in obj
      }
    },
  },

  computed: {
    ...mapState({
      notifications: state => state.Notifier.notifications
    }),
  },

  mounted () {
    let timeout = this.notification.hasOwnProperty('timeout') ? this.notification.timeout : true
        if (timeout) {
  	        let delay = this.notification.delay || 3000
            this.timer = setTimeout(() => {
            this.doDismiss(this.notification)
        }, delay)
    }
  },
 
  methods: {
     doDismiss: function (notification) {
    	clearTimeout(this.timer)
      this.$emit('dismiss-notification', notification)
    }
  }
}

</script>

    
