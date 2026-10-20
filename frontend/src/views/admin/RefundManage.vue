<template><el-card v-loading="loading"><template #header>退款记录</template><el-alert v-if="error" title="退款记录加载失败" type="error"><el-button @click="load()">重试</el-button></el-alert><el-table :data="items" empty-text="暂无退款记录"><el-table-column prop="refundNo" label="退款号" min-width="220"/><el-table-column prop="orderId" label="订单 ID" width="100"/><el-table-column label="金额" width="100"><template #default="{row}">¥{{formatPrice(row.amountCents)}}</template></el-table-column><el-table-column prop="status" label="状态" width="110"/><el-table-column label="类型" width="110"><template #default="{row}">{{row.compensation?'异常付款补偿':'整单退款'}}</template></el-table-column><el-table-column prop="failReason" label="失败原因" min-width="180"/><el-table-column label="操作" width="100"><template #default="{row}"><el-button v-if="row.status!=='SUCCESS'" size="small" :disabled="busy" @click="retry(row.id)">重试</el-button></template></el-table-column></el-table><el-pagination :current-page="page" :page-size="10" :total="total" layout="prev,pager,next,total" @current-change="load"/></el-card></template>
<script setup lang="ts">
import {onMounted,ref} from 'vue'
import {adminRefunds,retryRefund,type RefundRecord} from '@/api/payment'
import {formatPrice} from '@/utils/format'
const items=ref<RefundRecord[]>([]),page=ref(1),total=ref(0),loading=ref(false),error=ref(false),busy=ref(false)
async function load(p=page.value){page.value=p;loading.value=true;error.value=false;try{const r=await adminRefunds(p);items.value=r.items;total.value=r.total}catch{error.value=true}finally{loading.value=false}}
async function retry(id:number){busy.value=true;try{await retryRefund(id);await load()}catch{}finally{busy.value=false}}
onMounted(()=>load())
</script>
