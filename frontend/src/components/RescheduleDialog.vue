<script setup lang="ts">
import {ref,watch} from 'vue'
import {ElMessage} from 'element-plus'
import {moveOrder,type Fulfillment} from '@/api/fulfillment'
import {formatDateTime,formatPrice} from '@/utils/format'
const props=defineProps<{orderId:number;state:Fulfillment;open:boolean}>()
const emit=defineEmits<{ 'update:open':[boolean];changed:[Fulfillment] }>()
const target=ref<number>(),busy=ref(false),attempt=ref<{target:number;version:number;key:string}>()
watch(()=>props.open,open=>{if(open){target.value=undefined;attempt.value=undefined}})
async function submit(){if(busy.value||!target.value)return;const version=props.state.version;if(!attempt.value||attempt.value.target!==target.value||attempt.value.version!==version)attempt.value={target:target.value,version,key:crypto.randomUUID()};busy.value=true;try{const r=await moveOrder(props.orderId,attempt.value.target,attempt.value.version,attempt.value.key);emit('changed',r);emit('update:open',false);ElMessage.success('改期成功，原始订单金额保持不变')}catch{}finally{busy.value=false}}
</script>
<template><el-dialog :model-value="open" title="预约改期" width="min(620px,94vw)" :close-on-click-modal="!busy" :close-on-press-escape="!busy" :show-close="!busy" @update:model-value="emit('update:open',$event)"><el-alert title="整单改到同一活动、与下单时相同单价的场次。改期成功后旧核销码失效。" type="info" :closable="false"/><p>当前场次：{{state.current?formatDateTime(state.current.startTime):'—'}}</p><el-empty v-if="!state.candidates.length" description="暂无可改期的同价场次，可稍后刷新"/><el-radio-group v-else v-model="target" :disabled="busy" class="choices"><el-radio v-for="s in state.candidates" :key="s.id" :value="s.id" border><span>{{formatDateTime(s.startTime)}} · ¥{{formatPrice(s.priceCents)}} · 余 {{s.available}} 名</span></el-radio></el-radio-group><template #footer><el-button :disabled="busy" @click="emit('update:open',false)">取消</el-button><el-button type="primary" :loading="busy" :disabled="!target" @click="submit">确认改期</el-button></template></el-dialog></template>
<style scoped>.choices{display:flex;flex-direction:column;align-items:stretch;gap:10px}.el-radio{margin:0;height:auto;min-height:48px;padding:12px}.el-radio :deep(.el-radio__label){white-space:normal;line-height:1.6}</style>
