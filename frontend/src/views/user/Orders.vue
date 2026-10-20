<script setup lang="ts">
import { onMounted, onUnmounted, shallowRef } from 'vue'
import { Calendar, ArrowRight } from '@element-plus/icons-vue'
import { listOrders, type OrderSummary } from '@/api/booking'
import { formatDateTime, formatPrice } from '@/utils/format'
import StatusBadge from '@/components/ui/StatusBadge.vue'
const items = shallowRef<OrderSummary[]>([]),
  page = shallowRef(1),
  total = shallowRef(0),
  loading = shallowRef(false),
  error = shallowRef(false)
let epoch = 0
async function load(p = page.value) {
  page.value = p
  const run = ++epoch
  loading.value = true
  error.value = false
  try {
    const res = await listOrders(p)
    if (run === epoch) {
      items.value = res.items
      total.value = res.total
    }
  } catch {
    if (run === epoch) error.value = true
  } finally {
    if (run === epoch) loading.value = false
  }
}
onMounted(() => load())
onUnmounted(() => epoch++)
</script>
<template>
  <section v-loading="loading" class="orders-page">
    <div class="orders-summary">
      <span
        ><el-icon><Calendar /></el-icon>你的城市体验记录</span
      ><span>共 {{ total }} 笔预约</span>
    </div>
    <el-result v-if="error" icon="error" title="订单加载失败"
      ><template #extra><el-button @click="load()">重试</el-button></template></el-result
    ><template v-else
      ><el-empty v-if="!loading && !items.length" description="还没有预约，下一段城市体验正在等你"
        ><router-link to="/"><el-button type="primary">探索活动</el-button></router-link></el-empty
      >
      <article v-for="o in items" :key="o.id" class="order">
        <div class="order-top">
          <span class="order-number">{{ o.orderNo }}</span
          ><StatusBadge :status="o.status" />
        </div>
        <div class="order-main">
          <div class="order-info">
            <span class="order-icon" aria-hidden="true"
              ><el-icon><Calendar /></el-icon
            ></span>
            <div>
              <strong>城市活动预约</strong>
              <p>{{ o.contactName }} · {{ formatDateTime(o.createdAt) }}</p>
            </div>
          </div>
          <div class="order-actions">
            <strong class="order-price"><small>¥</small>{{ formatPrice(o.totalCents) }}</strong
            ><el-button @click="$router.push('/orders/' + o.id)"
              >查看订单 <el-icon><ArrowRight /></el-icon
            ></el-button>
          </div>
        </div>
      </article>
      <el-pagination
        :current-page="page"
        :page-size="10"
        :total="total"
        layout="prev,pager,next,total"
        @current-change="load"
    /></template>
  </section>
</template>
<style scoped>
.orders-summary {
  display: flex;
  justify-content: space-between;
  gap: 15px;
  color: #839180;
  font-size: 12px;
  margin: 0 0 19px;
}
.orders-summary > span:first-child {
  display: flex;
  align-items: center;
  gap: 7px;
}
.order {
  background: #fff;
  border: 1px solid var(--city-line);
  border-radius: 14px;
  margin-bottom: 19px;
  overflow: hidden;
}
.order-top {
  background: #fbfcfa;
  padding: 14px 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 15px;
  border-bottom: 1px solid #eef1eb;
}
.order-number {
  font-size: 11px;
  color: #899582;
  overflow-wrap: anywhere;
  min-width: 0;
}
.order-main {
  padding: 25px 22px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}
.order-info {
  display: flex;
  gap: 15px;
  align-items: center;
}
.order-icon {
  width: 49px;
  height: 49px;
  display: grid;
  place-items: center;
  font-size: 24px;
  border-radius: 12px;
  background: #eaf1e4;
  color: #769460;
  flex-shrink: 0;
}
.order-info strong {
  font-size: 15px;
  font-weight: 600;
}
.order-info p {
  margin: 7px 0 0;
  color: #8a9584;
  font-size: 11px;
}
.order-actions {
  display: flex;
  align-items: center;
  gap: 28px;
}
.order-actions .el-icon {
  margin-left: 8px;
}
.order-price {
  font-size: 23px;
  color: #386443;
  font-weight: 650;
}
.order-price small {
  font-size: 13px;
  margin-right: 3px;
}
.el-pagination {
  justify-content: center;
}
@media (max-width: 600px) {
  .order-main {
    padding: 20px 18px;
  }
  .order-top {
    padding: 13px 17px;
  }
  .order-actions {
    width: 100%;
    justify-content: space-between;
    border-top: 1px solid #eef1eb;
    padding-top: 17px;
  }
  .order-info p {
    font-size: 10px;
  }
  .order-info strong {
    font-size: 14px;
  }
}
</style>
