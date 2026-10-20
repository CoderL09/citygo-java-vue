<template>
  <div v-loading="loading">
    <el-page-header content="订单详情" @back="$router.push('/orders')" />
    <el-result v-if="error" icon="error" title="订单不存在或加载失败"
      ><template #extra><el-button @click="load">重试</el-button></template></el-result
    >
    <el-card v-if="detail && !error" style="margin-top: 20px">
      <div class="bar">
        <h3>{{ detail.order.orderNo }}</h3>
        <StatusBadge :status="detail.order.status" />
      </div>
      <p>预约联系人：{{ detail.order.contactName }} · {{ detail.order.contactPhone }}</p>
      <p>创建时间：{{ formatDateTime(detail.order.createdAt) }}</p>
      <p v-if="detail.order.remark">备注：{{ detail.order.remark }}</p>
      <h3>下单时的预约快照</h3>
      <div v-for="item in detail.items" :key="item.id" class="item">
        <h3>
          {{ item.name }} × {{ item.quantity }} <span>¥{{ formatPrice(item.lineTotalCents) }}</span>
        </h3>
        <p v-if="item.packageSnapshot">
          套餐版本 {{ item.packageSnapshot.packageVersion }} · 每份 ¥{{
            formatPrice(item.unitPriceCents)
          }}
        </p>
        <div
          v-for="s in item.packageSnapshot?.sessions ||
          (item.sessionSnapshot ? [item.sessionSnapshot] : [])"
          :key="s.sessionId"
        >
          <strong>{{ s.activityName }}</strong>
          <p>{{ s.address }}</p>
          <p>{{ formatDateTime(s.startTime) }} 至 {{ formatDateTime(s.endTime) }}</p>
        </div>
      </div>
      <p class="total">订单金额 ¥{{ formatPrice(detail.order.totalCents) }}</p>
      <el-alert
        v-if="detail.order.status === 'PENDING_PAYMENT'"
        :title="'待支付，有效期至 ' + formatDateTime(detail.order.expireAt)"
        type="info"
        :closable="false"
      />
      <div class="actions">
        <template v-if="detail.order.status === 'PENDING_PAYMENT'">
          <el-select v-model="scenario" aria-label="模拟支付结果" style="width: 180px"
            ><el-option label="模拟成功" value="SUCCESS" /><el-option
              label="模拟失败"
              value="FAIL" /><el-option label="模拟结果未知" value="TIMEOUT" /><el-option
              label="模拟通知延迟"
              value="DELAYED"
          /></el-select>
          <el-button type="primary" :loading="busy" @click="pay">模拟支付</el-button>
          <el-button :disabled="busy" @click="cancel">取消订单</el-button>
        </template>
        <el-button
          v-if="detail.order.status === 'PENDING_CONFIRMATION'"
          :disabled="busy"
          @click="urge"
          >催确认</el-button
        >
        <el-button
          v-if="['PENDING_CONFIRMATION', 'CONFIRMED'].includes(detail.order.status)"
          :loading="busy"
          @click="refund"
          >申请全额退款</el-button
        >
        <el-button :disabled="busy" @click="reorder">再来一单</el-button>
        <el-button :disabled="busy" @click="load">刷新状态</el-button>
      </div>
      <el-alert
        v-if="detail.order.status === 'PENDING_PAYMENT'"
        title="当前为模拟支付演示，不会产生真实扣款。退款也为模拟操作。"
        type="warning"
        :closable="false"
      />
      <el-alert
        v-if="detail.order.status === 'REFUNDING'"
        title="正在处理全额退款。失败会保留记录，由运营重试；退款成功后释放名额。"
        type="info"
        :closable="false"
      />
      <h3 v-if="records?.payments.length">模拟支付记录</h3>
      <p v-for="p in records?.payments" :key="p.id">
        {{ p.channel }} · ¥{{ formatPrice(p.amountCents) }} · {{ p.status }}
      </p>
      <h3 v-if="records?.refunds.length">模拟退款记录</h3>
      <p v-for="r in records?.refunds" :key="r.id">
        ¥{{ formatPrice(r.amountCents) }} · {{ r.status }}
        <el-text v-if="r.failReason" type="danger">{{ r.failReason }}</el-text>
      </p>
    </el-card>
    <FulfillmentPanel
      v-if="detail && !error"
      :order-id="detail.order.id"
      :status="detail.order.status"
      @changed="load"
    />
  </div>
</template>
<script setup lang="ts">
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { onMounted, onUnmounted, ref } from 'vue'
import FulfillmentPanel from '@/components/FulfillmentPanel.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  payOrder,
  cancelOrder,
  refundOrder,
  paymentRecords,
  type PaymentRecords,
} from '@/api/payment'
import { useRoute, useRouter } from 'vue-router'
import { post } from '@/api/http'
import { getOrder, type OrderDetail } from '@/api/booking'
import { formatDateTime, formatPrice } from '@/utils/format'
const route = useRoute(),
  detail = ref<OrderDetail>(),
  loading = ref(false),
  error = ref(false)
const busy = ref(false),
  scenario = ref('SUCCESS'),
  records = ref<PaymentRecords>()
async function load() {
  loading.value = true
  error.value = false
  try {
    detail.value = await getOrder(Number(route.params.id))
    records.value = await paymentRecords(Number(route.params.id))
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}
async function urge() {
  busy.value = true
  try {
    await post('/orders/' + route.params.id + '/urge')
    ElMessage.success('提醒已提交，运营可在通知中查看')
  } catch {
  } finally {
    busy.value = false
  }
}
async function pay() {
  if (busy.value) return
  busy.value = true
  try {
    const result = await payOrder(Number(route.params.id), scenario.value)
    const paid = result.payments.some((p) => p.status === 'SUCCESS')
    ElMessage[paid ? 'success' : 'info'](
      paid ? '模拟支付成功' : '支付未确认，请查询最新状态或稍后重试',
    )
    await load()
  } catch {
  } finally {
    busy.value = false
  }
}
async function cancel() {
  try {
    await ElMessageBox.confirm('确认取消待支付订单？已付款订单应走退款流程。', '取消订单')
  } catch {
    return
  }
  busy.value = true
  try {
    await cancelOrder(Number(route.params.id))
    await load()
  } catch {
  } finally {
    busy.value = false
  }
}
async function refund() {
  let reason: string
  try {
    const r = await ElMessageBox.prompt(
      '请输入全额退款原因。已到最早场次开始时间时不能申请。',
      '申请退款',
      { inputValidator: (v) => Boolean(v?.trim()) || '请填写原因', inputValue: '行程变化' },
    )
    reason = r.value
  } catch {
    return
  }
  busy.value = true
  try {
    await refundOrder(Number(route.params.id), reason)
    await load()
  } catch {
  } finally {
    busy.value = false
  }
}
let poll: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  load()
  poll = setInterval(() => {
    if (
      !busy.value &&
      detail.value &&
      ['PENDING_PAYMENT', 'REFUNDING'].includes(detail.value.order.status)
    )
      load()
  }, 10000)
})
onUnmounted(() => {
  if (poll) clearInterval(poll)
})

const reorderRouter = useRouter()
async function reorder() {
  busy.value = true
  try {
    const r = await post<{ added: number; skipped: { reason: string }[] }>(
      '/orders/' + route.params.id + '/reorder',
    )
    if (r.skipped.length)
      await ElMessageBox.alert(r.skipped.map((i) => i.reason).join('；'), '部分商品无法加入')
    ElMessage.success('已按当前价格加入 ' + r.added + ' 项')
    await reorderRouter.push('/cart')
  } finally {
    busy.value = false
  }
}
</script>
<style scoped>
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin: 20px 0;
}
.bar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.bar h3 {
  word-break: break-all;
}
.item {
  padding: 20px 0;
  border-top: 1px solid var(--el-border-color-light);
}
.item span {
  float: right;
}
.total {
  text-align: right;
  font-weight: 600;
  font-size: 22px;
}
p {
  color: var(--el-text-color-regular);
}
</style>
