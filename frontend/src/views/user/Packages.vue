<script setup lang="ts">
import { onMounted, onUnmounted, ref, shallowRef } from 'vue'
import { Search, Box, ArrowRight } from '@element-plus/icons-vue'
import { listPackages, type Package } from '@/api/booking'
import { formatPrice } from '@/utils/format'
const items = ref<Package[]>([]),
  keyword = shallowRef(''),
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
    const res = await listPackages(false, p, keyword.value)
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
  <div class="packages-page">
    <div class="page-heading">
      <div>
        <h1>一次出发，多重体验。</h1>
        <p>把喜欢的活动组合起来，让行程更有期待。</p>
      </div>
      <router-link to="/" class="back-discover">继续探索活动</router-link>
    </div>
    <form class="package-search" @submit.prevent="load(1)">
      <el-input
        v-model="keyword"
        :prefix-icon="Search"
        placeholder="搜索套餐名称"
        aria-label="搜索套餐名称"
        clearable
        @clear="load(1)"
      /><el-button type="primary" native-type="submit" :loading="loading">搜索</el-button>
    </form>
    <el-result v-if="error" icon="error" title="套餐加载失败"
      ><template #extra><el-button @click="load()">重试</el-button></template></el-result
    >
    <div v-else v-loading="loading" class="package-grid">
      <el-empty v-if="!loading && !items.length" description="暂无在售套餐，不妨先看看单场活动"
        ><router-link to="/"><el-button>探索活动</el-button></router-link></el-empty
      >
      <article v-for="p in items" :key="p.id" class="package-card">
        <div class="package-top">
          <span class="package-icon"
            ><el-icon><Box /></el-icon></span
          ><span class="package-version">组合套餐 · V{{ p.version }}</span>
        </div>
        <h2>{{ p.name }}</h2>
        <p>{{ p.description || '多种活动体验，一次预约安排。' }}</p>
        <div class="package-bottom">
          <span class="package-price"
            ><small>¥</small>{{ formatPrice(p.packagePriceCents) }}<em>/份</em></span
          ><el-button type="primary" plain @click="$router.push('/packages/' + p.id)"
            >查看套餐 <el-icon><ArrowRight /></el-icon
          ></el-button>
        </div>
      </article>
    </div>
    <el-pagination
      v-if="!error && total > 0"
      :current-page="page"
      :page-size="10"
      :total="total"
      layout="prev,pager,next,total"
      @current-change="load"
    />
  </div>
</template>
<style scoped>
.package-search {
  display: flex;
  gap: 12px;
  max-width: 500px;
  margin-bottom: 27px;
}
.package-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
  min-height: 150px;
}
.package-grid > .el-empty {
  grid-column: 1/-1;
}
.package-card {
  border: 1px solid var(--city-line);
  background: #fff;
  border-radius: 15px;
  padding: 28px;
  min-width: 0;
}
.package-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 25px;
}
.package-icon {
  width: 47px;
  height: 47px;
  display: grid;
  place-items: center;
  font-size: 25px;
  background: #edf1e3;
  color: #7a8b4d;
  border-radius: 13px;
}
.package-version {
  font-size: 10px;
  color: #8c967d;
}
.package-card h2 {
  font-size: 20px;
  margin-bottom: 12px;
  overflow-wrap: anywhere;
}
.package-card > p {
  font-size: 12px;
  color: #85907f;
  line-height: 1.8;
  min-height: 45px;
}
.package-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  border-top: 1px solid var(--city-line);
  padding-top: 22px;
  margin-top: 25px;
  flex-wrap: wrap;
}
.package-price {
  font-size: 27px;
  font-weight: 650;
  color: #346443;
  white-space: nowrap;
}
.package-price small {
  font-size: 15px;
  margin-right: 4px;
}
.package-price em {
  font-size: 11px;
  font-style: normal;
  font-weight: 400;
  color: #939c8c;
  margin-left: 7px;
}
.package-bottom .el-icon {
  margin-left: 8px;
}
.back-discover {
  font-size: 12px;
}
.el-pagination {
  justify-content: center;
}
@media (max-width: 700px) {
  .package-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }
  .package-card {
    padding: 22px;
  }
  .package-card h2 {
    font-size: 18px;
  }
}
</style>
