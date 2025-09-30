// Global notifier state
import { defineStore } from 'pinia'
import { useSearchStore } from '../store/search.store'

export const useNotifierStore = defineStore('notifier', {

  state: () => ({
    notifications: []
  }),

  
  actions: {
    setNotification( notification) {
      this.notifications.push(notification)
    },
    dismissNotification ( notification) {
      this.notifications.pop()
      const search = useSearchStore()
      search.setLoadingStatus(false)
    },
    resetState(){
      this.$reset()
    }
  }

})

  