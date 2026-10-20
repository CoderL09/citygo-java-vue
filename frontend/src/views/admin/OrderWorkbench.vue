<template>
  <el-card v-loading="loading"
    ><template #header>订单工作台</template>
    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable @change="load(1)"
        ><el-option v-for="s in states" :key="s" :label="statusText(s)" :value="s" /></el-select
      ><el-input
        v-model="keyword"
        placeholder="搜索订单号"
        @keyup.enter="load(1)"
        clearable
      /><el-button @click="load(1)">搜索</el-button>
    </div>
    <el-alert v-if="error" title="订单加载失败" type="error" :closable="false"
      ><el-button @click="load()">重试</el-button></el-alert
    >
    <el-table :data="items" empty-text="暂无订单"
      ><el-table-column prop="orderNo" label="订单号" min-width="210" /><el-table-column
        prop="contactName"
        label="联系人"
        width="100"
      /><el-table-column label="金额" width="100"
        ><template #default="{ row }">¥{{ formatPrice(row.totalCents) }}</template></el-table-column
      ><el-table-column label="状态" width="120"
        ><template #default="{ row }"
          ><StatusBadge :status="row.status" /></template></el-table-column
      ><el-table-column label="操作" min-width="280"
        ><template #default="{ row }"
          ><el-button size="small" @click="show(row.id)">详情</el-button
          ><template v-if="row.status === 'PENDING_CONFIRMATION'"
            ><el-button size="small" type="primary" :disabled="busy" @click="act(row.id, 'confirm')"
              >确认</el-button
            ><el-button size="small" type="danger" :disabled="busy" @click="act(row.id, 'reject')"
              >拒绝并退款</el-button
            ></template
          ><el-button
            v-if="row.status === 'CONFIRMED'"
            size="small"
            type="success"
            :disabled="busy"
            @click="act(row.id, 'complete')"
            >核销</el-button
          ></template
        ></el-table-column
      ></el-table
    >
    <el-pagination
      :current-page="page"
      :page-size="10"
      :total="total"
      layout="prev,pager,next,total"
      @current-change="load"
    />
    <el-drawer v-model="visible" title="订单详情" size="min(720px, 96vw)"
      ><template v-if="detail"
        ><h3>{{ detail.order.orderNo }}</h3>
        <StatusBadge :status="detail.order.status" />
        <p>{{ detail.order.contactName }} · {{ detail.order.contactPhone }}</p>
        <div v-for="i in detail.items" :key="i.id">
          <h4>{{ i.name }} × {{ i.quantity }}</h4>
          <p
            v-for="s in i.packageSnapshot?.sessions ||
            (i.sessionSnapshot ? [i.sessionSnapshot] : [])"
            :key="s.sessionId"
          >
            {{ s.activityName }} · {{ formatDateTime(s.startTime) }} · {{ s.address }}
          </p>
        </div>
        <FulfillmentPanel :order-id="detail.order.id" :status="detail.order.status" admin />
        <h3>支付记录</h3>
        <p v-for="p in records?.payments" :key="p.id">
          {{ p.requestNo }} · {{ p.status }} · ¥{{ formatPrice(p.amountCents) }}
        </p>
        <h3>退款记录</h3>
        <p v-for="r in records?.refunds" :key="r.id">
          {{ r.refundNo }} · {{ r.status }} · {{ r.failReason }}
        </p></template
      ></el-drawer
    >
  </el-card>
</template>
<script setup lang="ts">
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { onMounted, ref } from 'vue'
import FulfillmentPanel from '@/components/FulfillmentPanel.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminOrders,
  adminOrder,
  orderAction,
  paymentRecords,
  type PaymentRecords,
} from '@/api/payment'
import type { OrderDetail, OrderSummary } from '@/api/booking'
import { formatDateTime, formatPrice } from '@/utils/format'
import { statusText } from '@/utils/order'
const states = [
  'PENDING_PAYMENT',
  'PENDING_CONFIRMATION',
  'CONFIRMED',
  'COMPLETED',
  'CANCELLED',
  'CLOSED',
  'REFUNDING',
  'REFUNDED',
]
const items = ref<OrderSummary[]>([]),
  page = ref(1),
  total = ref(0),
  status = ref(''),
  keyword = ref(''),
  loading = ref(false),
  error = ref(false),
  busy = ref(false),
  visible = ref(false),
  detail = ref<OrderDetail>(),
  records = ref<PaymentRecords>()
async function load(p = page.value) {
  page.value = p
  loading.value = true
  error.value = false
  try {
    const r = await adminOrders(p, status.value, keyword.value)
    items.value = r.items
    total.value = r.total
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}
async function show(id: number) {
  detail.value = await adminOrder(id)
  records.value = await paymentRecords(id, true)
  visible.value = true
}
async function act(id: number, action: 'confirm' | 'reject' | 'complete') {
  let reason: string | undefined
  if (action === 'reject') {
    try {
      const r = await ElMessageBox.prompt('拒绝后整单全额退款，退款失败会保留记录。', '拒绝预约', {
        inputValue: '无法接待',
        inputValidator: (v) => Boolean(v?.trim()) || '请填写原因',
      })
      reason = r.value
    } catch {
      return
    }
  } else if (action === 'complete') {
    try {
      await ElMessageBox.confirm('确认整单到场核销？系统会校验所有场次均已开始。', '到场核销')
    } catch {
      return
    }
  }
  busy.value = true
  try {
    await orderAction(id, action, reason)
    ElMessage.success('订单状态已更新')
    await load()
  } catch {
  } finally {
    busy.value = false
  }
}
onMounted(() => load())
</script>
<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  max-width: 660px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.toolbar .el-select {
  width: 180px;
}
.toolbar .el-input {
  width: 260px;
}
.el-pagination {
  margin-top: 18px;
}
</style>
