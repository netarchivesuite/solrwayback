<template>
  <div class="notifications">
    <notification-item v-for="(notification, index) in this.notifierStore.notifications" 
                       :key="index"
                       :current="index + 1 === this.notifierStore.notifications.length"
                       :notification="notification" 
                       @dismiss-notification="doDismissNotification" />
  </div>
</template>

<script>
import NotificationItem from './NotificationItem.vue'
// import { mapState, mapActions } from 'vuex'
import { mapStores, mapActions } from 'pinia'
import { useNotifierStore } from '../../store/notifier.store'

export default {
  name: 'Notifications',
  
  components: {  
    NotificationItem,
  },

  computed: {
    // ...mapState({
    //   notifications: state => state.Notifier.notifications
    // }),
    ...mapStores(useNotifierStore)
  },

  methods: {
  ...mapActions(useNotifierStore, {
      dismissNotification: 'dismissNotification'
      
  }),

    doDismissNotification(notification) {
      this.dismissNotification(notification)
    }
  }
}

</script>

    
