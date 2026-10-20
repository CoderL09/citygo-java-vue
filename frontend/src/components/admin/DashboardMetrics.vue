<script setup lang="ts">
import { computed, onMounted, onUnmounted, shallowRef } from 'vue'
import { Tickets, Calendar, Wallet, TrendCharts, RefreshRight } from '@element-plus/icons-vue'
import { adminOrders } from '@/api/payment'
import { get } from '@/api/http'
import { formatPrice } from '@/utils/format'
interface DailyReport {
  days: { paidCents: number; orders: number }[]
}
const pending = shallowRef<number>(),
  confirmed = shallowRef<number>(),
  report = shallowRef<DailyReport>(),
  loading = shallowRef(true),
  failed = shallowRef(false)
let epoch = 0
const metrics = computed(() => [
  {
    label: '待确认订单',
    value: pending.value === undefined ? '—' : String(pending.value),
    unit: '笔',
    note: '等待处理的已支付预约',
    icon: Tickets,
    tone: 'orange',
  },
  {
    label: '待履约预约',
    value: confirmed.value === undefined ? '—' : String(confirmed.value),
    unit: '笔',
    note: '已确认，等待到场体验',
    icon: Calendar,
    tone: 'green',
  },
  {
    label: '今日模拟支付',
    value: report.value ? formatPrice(report.value.days.reduce((s, d) => s + d.paidCents, 0)) : '—',
    unit: '元',
    note: '按上海日期统计支付成功金额',
    icon: Wallet,
    tone: 'blue',
  },
  {
    label: '今日新订单',
    value: report.value ? String(report.value.days.reduce((s, d) => s + d.orders, 0)) : '—',
    unit: '笔',
    note: '包含今日创建的全部订单',
    icon: TrendCharts,
    tone: 'purple',
  },
])
async function load() {
  const run = ++epoch
  loading.value = true
  failed.value = false
  pending.value = undefined
  confirmed.value = undefined
  report.value = undefined
  const today = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Shanghai' }).format(new Date())
  const [p, c, r] = await Promise.allSettled([
    adminOrders(1, 'PENDING_CONFIRMATION'),
    adminOrders(1, 'CONFIRMED'),
    get<DailyReport>('/admin/reports', { params: { from: today, to: today } }),
  ])
  if (run !== epoch) return
  if (p.status === 'fulfilled') pending.value = p.value.total
  if (c.status === 'fulfilled') confirmed.value = c.value.total
  if (r.status === 'fulfilled') report.value = r.value
  failed.value = [p, c, r].some((x) => x.status === 'rejected')
  loading.value = false
}
onMounted(load)
onUnmounted(() => epoch++)
</script>
<template>
  <section class="dashboard-metrics" aria-label="运营指标" :aria-busy="loading">
    <article v-for="metric in metrics" :key="metric.label" class="metric-card">
      <div class="metric-top">
        <span>{{ metric.label }}</span
        ><span class="metric-icon" :class="metric.tone"
          ><el-icon><component :is="metric.icon" /></el-icon
        ></span>
      </div>
      <div class="metric-value">
        {{ loading ? '…' : metric.value }}<small>{{ metric.unit }}</small>
      </div>
      <p>{{ metric.note }}</p>
    </article>
    <div v-if="failed" class="metrics-error" role="alert">
      部分指标加载失败，未显示为零。<el-button text :icon="RefreshRight" @click="load"
        >重试</el-button
      >
    </div>
  </section>
</template>
<style scoped>
.dashboard-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 26px;
}
.metric-card {
  padding: 21px 22px;
  background: #fff;
  border: 1px solid var(--city-line);
  border-radius: 13px;
  min-width: 0;
}
.metric-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 9px;
  font-size: 12px;
  color: #718075;
}
.metric-icon {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  font-size: 17px;
  flex-shrink: 0;
}
.orange {
  background: #fff1e5;
  color: #c98041;
}
.green {
  background: #eaf3e8;
  color: #588354;
}
.blue {
  background: #ebf0f8;
  color: #6686b0;
}
.purple {
  background: #f0eef8;
  color: #9b87b1;
}
.metric-value {
  font-size: 31px;
  font-weight: 650;
  line-height: 1.3;
  letter-spacing: -0.8px;
  margin: 15px 0 13px;
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
}
.metric-value small {
  font-size: 11px;
  font-weight: 400;
  color: #89958c;
  letter-spacing: 0;
  margin-left: 7px;
}
.metric-card p {
  font-size: 11px;
  color: #69776d;
  margin: 0;
  line-height: 1.6;
}
.metrics-error {
  grid-column: 1/-1;
  font-size: 12px;
  color: var(--el-color-danger);
}
@media (max-width: 1150px) {
  .metric-card {
    padding: 18px 17px;
  }
  .dashboard-metrics {
    gap: 12px;
  }
  .metric-value {
    font-size: 28px;
  }
}
@media (max-width: 980px) {
  .dashboard-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 420px) {
  .metric-card {
    padding: 16px 13px;
  }
  .metric-top {
    font-size: 11px;
  }
  .metric-icon {
    width: 26px;
    height: 26px;
    font-size: 14px;
  }
  .metric-value {
    font-size: 26px;
  }
}
</style>
