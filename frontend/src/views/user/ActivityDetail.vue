<template>
  <div v-loading="loading">
    <el-page-header class="back" content="活动详情" @back="$router.back()" />
    <el-result v-if="error" icon="error" title="活动详情加载失败或已下架">
      <template #extra><el-button @click="load">重试</el-button></template>
    </el-result>
    <el-card v-if="detail && !error" class="activity-information">
      <el-row :gutter="24">
        <el-col :md="10" :sm="24">
          <el-carousel v-if="detail.images.length" height="260px" class="carousel">
            <el-carousel-item v-for="img in detail.images" :key="img.id">
              <img :src="img.url" class="cover" alt="活动图片" />
            </el-carousel-item>
          </el-carousel>
          <div v-else class="activity-placeholder">
            <img :src="cityScene" alt="" /><span>从这里，发现城市新体验</span>
          </div>
        </el-col>
        <el-col :md="14" :sm="24">
          <h2>{{ detail.name }}</h2>
          <el-tag :type="detail.status === 'ON_SALE' ? 'success' : 'info'">
            {{ detail.status === 'ON_SALE' ? '在售' : '已下架' }}
          </el-tag>
          <p class="addr">
            <el-icon><Location /></el-icon>{{ detail.address }}
          </p>
          <p class="desc">{{ detail.description || '暂无介绍' }}</p>
        </el-col>
      </el-row>
    </el-card>

    <el-card v-if="detail && !error" class="sessions">
      <template #header>场次</template>
      <el-table :data="detail?.sessions || []" empty-text="暂无可预约场次">
        <el-table-column label="开始时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="预约截止" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.bookingDeadline) }}</template>
        </el-table-column>
        <el-table-column label="价格（元）" width="110">
          <template #default="{ row }">{{ formatPrice(row.priceCents) }}</template>
        </el-table-column>
        <el-table-column label="剩余名额" width="100">
          <template #default="{ row }">{{ row.available }}/{{ row.capacity }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :disabled="
                saving ||
                row.available <= 0 ||
                new Date(row.bookingDeadline).getTime() <= Date.now()
              "
              @click="addCart(row)"
            >
              加入购物车
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import cityScene from '@/assets/city-scene.svg'
import { useRoute, useRouter } from 'vue-router'
import { addCartItem } from '@/api/booking'
import { ElMessage } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
import { getPublicActivity } from '@/api/activity'
import { useAuthStore } from '@/stores/auth'
import type { ActivityDetailView, SessionView } from '@/api/types'
import { formatDateTime, formatPrice } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const saving = ref(false)
const auth = useAuthStore()
const detail = ref<ActivityDetailView | null>(null)
const loading = ref(false)
const error = ref(false)

async function load() {
  loading.value = true
  error.value = false
  try {
    detail.value = await getPublicActivity(Number(route.params.id))
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

async function addCart(row: SessionView) {
  if (!auth.user) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (auth.user.role !== 'USER') {
    ElMessage.warning('请使用用户账号预约')
    return
  }
  if (saving.value) return
  saving.value = true
  try {
    await addCartItem({ itemType: 'SESSION', itemId: row.id, quantity: 1 })
    ElMessage.success('已加入购物车')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.activity-placeholder {
  height: 280px;
  border-radius: 12px;
  background: #d9e6d3;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}
.activity-placeholder img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.activity-placeholder span {
  position: absolute;
  bottom: 18px;
  left: 18px;
  font-size: 11px;
  padding: 6px 10px;
  border-radius: 6px;
  background: #ffffffe6;
  color: #56724a;
}
.activity-information h2 {
  font-size: 27px;
  margin-top: 8px;
}
.activity-information .desc {
  font-size: 14px;
  line-height: 1.9;
}
.activity-information .addr {
  font-size: 12px;
  margin: 20px 0;
}
.activity-information .cover {
  border-radius: 12px;
}
.sessions :deep(.el-card__header) {
  font-size: 17px;
}

.back {
  margin-bottom: 12px;
}
.carousel {
  border-radius: 6px;
  overflow: hidden;
}
.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.addr,
.desc {
  color: var(--el-text-color-regular);
  margin: 10px 0;
}
.sessions {
  margin-top: 16px;
}
</style>
