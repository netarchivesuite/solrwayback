<template>
  <div ref="notificationItem"
       class="notification"
       tabindex="0"
       :class="current === true ? notification.type : notification.type + ' collapsed'"
       @keyup.esc="doDismiss(notification)">
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
    <br>
    <p v-if="notification.srvMessage">
      <span>server message: </span>{{ notification.srvMessage }}
    </p>
  </div>
</template>

<script>
// import { mapState, mapActions } from 'vuex'
import { mapStores } from 'pinia'
import { useNotifierStore } from '../../store/notifier.store'

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
    // ...mapState({
    //   notifications: state => state.Notifier.notifications
    // }),
    ...mapStores(useNotifierStore)
  },

  mounted () {
    let timeout = this.notification.hasOwnProperty('timeout') ? this.notification.timeout : true
        if (timeout) {
  	        let delay = this.notification.delay || 5000
            this.timer = setTimeout(() => {
            this.doDismiss(this.notification)
        }, delay)
    }
    this.$refs.notificationItem.focus()
  },
 
  methods: {
     doDismiss: function (notification) {
    	clearTimeout(this.timer)
      this.$emit('dismiss-notification', notification)
    }
  }
}

</script>

    
