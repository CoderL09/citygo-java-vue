<script setup lang="ts">
import {computed,onUnmounted,ref,watch} from 'vue'
import QRCode from 'qrcode'
import {issueCode,type CheckinCode} from '@/api/fulfillment'
import {formatDateTime} from '@/utils/format'
const props=defineProps<{orderId:number;version:number}>()
const ticket=ref<CheckinCode>(),image=ref(''),busy=ref(false),generated=ref(0),tick=ref(Date.now())
const timer=setInterval(()=>tick.value=Date.now(),1000)
const remaining=computed(()=>ticket.value?Math.max(0,ticket.value.ttlSeconds-Math.floor((tick.value-generated.value)/1000)):0)
let epoch=0
watch(()=>[props.orderId,props.version],()=>{epoch++;ticket.value=undefined;image.value='';busy.value=false})
async function generate(){if(busy.value)return;const run=epoch;busy.value=true;try{const result=await issueCode(props.orderId),png=await QRCode.toDataURL(result.code,{width:256,margin:3,errorCorrectionLevel:'M'});if(run!==epoch)return;ticket.value=result;image.value=png;generated.value=Date.now();tick.value=generated.value}catch{}finally{if(run===epoch)busy.value=false}}
onUnmounted(()=>{epoch++;clearInterval(timer)})
</script>
<template><section class="checkin-code"><div class="heading"><div><h3>到场核销码</h3><p>向已登录的工作人员出示，有效期 5 分钟。</p></div><el-button :loading="busy" @click="generate">{{ticket?'刷新核销码':'生成核销码'}}</el-button></div><template v-if="ticket"><div v-if="remaining>0" class="qr"><img :src="image" width="256" height="256" alt="预约核销二维码"/><el-tag>剩余 {{remaining}} 秒</el-tag></div><el-alert v-else title="核销码已过期，请刷新后出示" type="warning" :closable="false"/><p>失效时间：{{formatDateTime(ticket.expiresAt)}}。刷新或改期后，旧码立即失效。</p><el-input :model-value="ticket.code" readonly aria-label="核销码"/><el-text size="small" type="info">二维码不含联系人或登录凭证，请勿公开分享。</el-text></template></section></template>
<style scoped>.checkin-code{border-top:1px solid var(--el-border-color-light);margin-top:20px;padding-top:12px}.heading{display:flex;justify-content:space-between;gap:16px;align-items:center;flex-wrap:wrap}h3{margin-bottom:6px}p{line-height:1.6;color:var(--el-text-color-secondary)}.qr{display:flex;flex-direction:column;align-items:center;gap:8px;margin:16px 0}.qr img{max-width:100%;height:auto}.el-text{display:block;margin-top:8px}</style>
