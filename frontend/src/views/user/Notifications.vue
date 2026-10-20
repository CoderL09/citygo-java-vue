<template><el-card><template #header><div class="bar"><span>通知中心</span><span><el-tag :type="notices.online?'success':'info'">{{notices.online?'消息在线':'消息离线'}}</el-tag>　<el-button @click="notices.pull">刷新</el-button><el-button :disabled="!notices.hasUnread" @click="notices.readAll">全部已读</el-button></span></div></template><el-alert v-if="notices.error" title="通知暂时无法更新，可稍后刷新。已有通知会保留。" type="warning" :closable="false"/><el-empty v-if="!notices.items.length" description="暂无通知"/><div v-for="n in displayed" :key="n.id" class="notice"><div><strong>{{n.title}}</strong>　<el-tag v-if="!n.read" size="small">未读</el-tag><p>{{n.content}}</p><el-text type="info">{{formatDateTime(n.createdAt)}}</el-text></div><el-button v-if="!n.read" @click="notices.markRead(n.id)">标为已读</el-button></div><el-pagination v-if="notices.items.length" v-model:current-page="page" :page-size="10" :total="notices.items.length" layout="prev,pager,next,total"/></el-card></template>
<script setup lang="ts">
import {computed,ref} from 'vue'
import {useNotifications} from '@/stores/notifications'
import {formatDateTime} from '@/utils/format'
const notices=useNotifications(),page=ref(1),displayed=computed(()=>notices.items.slice((page.value-1)*10,page.value*10))
</script>
<style scoped>.bar,.notice{display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap}.notice{padding:20px 0;border-bottom:1px solid var(--el-border-color-light)}p{word-break:break-all}</style>
