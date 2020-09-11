<template>
  <div class="notification"
       :class="current === true ? notification.type : notification.type + ' collapsed'">
    <button class="notificationCloseBtn"
            type="button"
            @click="doDismiss(notification)">
      ✕
    </button>
    <h2 v-if="notification.title">
      {{ notification.title }}
    </h2>
    <hr>
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
    current: {
      type:Boolean,
      required:true
    }
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

    
