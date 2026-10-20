<template>
  <router-view />
</template>
<script setup lang="ts">
import {watch} from 'vue'
import {useAuthStore} from '@/stores/auth'
import {useNotifications} from '@/stores/notifications'
const auth=useAuthStore(),notices=useNotifications()
watch(()=>auth.user?.id,id=>{if(id)notices.start();else notices.stop()},{immediate:true})
</script>
