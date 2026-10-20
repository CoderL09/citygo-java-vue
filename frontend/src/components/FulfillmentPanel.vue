<script setup lang="ts">
import {ref,watch,onUnmounted} from 'vue'
import {getFulfillment,type Fulfillment} from '@/api/fulfillment'
import {formatDateTime} from '@/utils/format'
import RescheduleDialog from './RescheduleDialog.vue'
import CheckinCode from './CheckinCode.vue'
const props=withDefaults(defineProps<{orderId:number;status:string;admin?:boolean}>(),{admin:false})
const emit=defineEmits<{changed:[]}>()
const state=ref<Fulfillment>(),loading=ref(false),error=ref(false),open=ref(false)
let epoch=0
async function load(){const run=++epoch;loading.value=true;error.value=false;try{const result=await getFulfillment(props.orderId,props.admin);if(run===epoch)state.value=result}catch{if(run===epoch)error.value=true}finally{if(run===epoch)loading.value=false}}
function moved(value:Fulfillment){state.value=value;emit('changed')}
watch(()=>[props.orderId,props.status,props.admin],()=>{open.value=false;void load()},{immediate:true})
onUnmounted(()=>{epoch++})
</script>
<template><el-card class="fulfillment" v-loading="loading" shadow="never"><template #header><div class="heading"><strong>当前预约与履约</strong><el-button text @click="load">刷新预约</el-button></div></template><el-alert v-if="error" title="预约信息加载失败，可点击刷新重试" type="error" :closable="false"/><template v-if="state"><div v-if="state.current"><el-tag v-if="state.version" type="success">已改期 {{state.version}} 次</el-tag><h3>{{state.current.activityName}}</h3><p class="current-time">{{formatDateTime(state.current.startTime)}} 至 {{formatDateTime(state.current.endTime)}}</p><p>{{state.current.address}}</p></div><template v-if="!admin&&status==='CONFIRMED'"><el-button v-if="state.canReschedule" @click="open=true">预约改期</el-button><el-alert v-else :title="state.reason||'当前预约不能改期'" type="info" :closable="false"/><CheckinCode :order-id="orderId" :version="state.version"/></template><el-alert v-if="state.checkin" :title="'已于 '+formatDateTime(state.checkin.checkedAt)+' 完成'+(state.checkin.source==='QR'?'扫码':'人工')+'核销'" type="success" :closable="false"/><template v-if="state.history.length"><h3>改期记录</h3><div v-for="h in state.history" :key="h.id" class="history"><p>{{formatDateTime(h.before.startTime)}} → {{formatDateTime(h.after.startTime)}}</p><el-text type="info" size="small">{{formatDateTime(h.createdAt)}} · {{h.quantity}} 份 · 同价改期</el-text></div></template><RescheduleDialog v-model:open="open" :order-id="orderId" :state="state" @changed="moved"/></template></el-card></template>
<style scoped>.fulfillment{margin-top:20px}.heading{display:flex;align-items:center;justify-content:space-between}.current-time{font-weight:600}.history{padding:10px 0;border-top:1px solid var(--el-border-color-lighter)}p{line-height:1.7;overflow-wrap:anywhere}</style>
