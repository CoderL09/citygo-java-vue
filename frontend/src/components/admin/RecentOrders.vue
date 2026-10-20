<script setup lang="ts">
import { onMounted, onUnmounted, shallowRef } from 'vue'
import { ArrowRight, RefreshRight } from '@element-plus/icons-vue'
import { adminOrders } from '@/api/payment'
import type { OrderSummary } from '@/api/booking'
import { formatDateTime, formatPrice } from '@/utils/format'
import StatusBadge from '@/components/ui/StatusBadge.vue'
const items = shallowRef<OrderSummary[]>([]),
  loading = shallowRef(false),
  failed = shallowRef(false)
let epoch = 0
async function load() {
  const run = ++epoch
  loading.value = true
  failed.value = false
  try {
    const res = await adminOrders(1)
    if (run === epoch) items.value = res.items.slice(0, 5)
  } catch {
    if (run === epoch) failed.value = true
  } finally {
    if (run === epoch) loading.value = false
  }
}
onMounted(load)
onUnmounted(() => epoch++)
</script>
<template>
  <section class="recent-orders">
    <header class="recent-heading">
      <div>
        <h2>最近预约</h2>
        <p>及时关注每一笔新的城市体验</p>
      </div>
      <router-link to="/admin/orders"
        >查看全部 <el-icon><ArrowRight /></el-icon
      ></router-link>
    </header>
    <el-alert v-if="failed" title="最近订单加载失败" type="error" :closable="false"
      ><el-button text :icon="RefreshRight" @click="load">重试</el-button></el-alert
    ><el-table v-else v-loading="loading" :data="items" empty-text="暂无预约，新的订单将在这里显示"
      ><el-table-column
        prop="orderNo"
        label="订单号"
        min-width="215"
        show-overflow-tooltip
      /><el-table-column prop="contactName" label="预约人" width="110" /><el-table-column
        label="订单金额"
        width="110"
        ><template #default="{ row }"
          ><strong>¥{{ formatPrice(row.totalCents) }}</strong></template
        ></el-table-column
      ><el-table-column label="状态" width="115"
        ><template #default="{ row }"
          ><StatusBadge :status="row.status" /></template></el-table-column
      ><el-table-column label="创建时间" min-width="160"
        ><template #default="{ row }">{{
          formatDateTime(row.createdAt)
        }}</template></el-table-column
      ></el-table
    >
  </section>
</template>
<style scoped>
.recent-orders {
  background: #fff;
  border: 1px solid var(--city-line);
  border-radius: 14px;
  overflow: hidden;
}
.recent-heading {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 15px;
}
.recent-heading h2 {
  font-size: 16px;
  margin-bottom: 5px;
}
.recent-heading p {
  font-size: 11px;
  color: #6c796f;
  margin: 0;
}
.recent-heading a {
  font-size: 11px;
  display: flex;
  gap: 8px;
  align-items: center;
  white-space: nowrap;
}
.recent-orders :deep(.el-table .cell) {
  padding-left: 23px;
  padding-right: 17px;
}
.recent-orders :deep(.el-table td) {
  font-size: 11px;
}
.recent-orders :deep(.el-table td:first-child) {
  color: #647869;
}
.recent-orders :deep(.el-table__inner-wrapper::before) {
  display: none;
}
@media (max-width: 600px) {
  .recent-heading {
    padding: 20px 18px;
  }
  .recent-heading h2 {
    font-size: 15px;
  }
  .recent-heading p {
    font-size: 11px;
  }
}
</style>
