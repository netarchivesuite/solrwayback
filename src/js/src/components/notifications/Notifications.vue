<template>
  <div class="notifications">
    <notification-item v-for="(notification, index) in notifications" 
                       :key="index"
                       :current="index + 1 === notifications.length"
                       :notification="notification" 
                       @dismiss-notification="doDismissNotification" />
  </div>
</template>

<script>
import NotificationItem from './NotificationItem.vue'
import { mapState, mapActions } from 'vuex'

export default {
  name: 'Notifications',
  
  components: {  
    NotificationItem,
  },

  computed: {
    ...mapState({
      notifications: state => state.Notifier.notifications
    }),
  },

  methods: {
  ...mapActions('Notifier', {
      dismissNotification: 'dismissNotification'
      
  }),

    doDismissNotification(notification) {
      this.dismissNotification(notification)
    }
  }
}

</script>

    
