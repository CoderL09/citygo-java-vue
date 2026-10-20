<script setup lang="ts">
import { onMounted, onUnmounted, ref, shallowRef, useTemplateRef } from 'vue'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { pagePublicActivities } from '@/api/activity'
import { pagePublicCategories } from '@/api/category'
import type { CategoryView, ActivityView } from '@/api/types'
import DiscoverHero from '@/components/discover/DiscoverHero.vue'
import ActivityCard from '@/components/discover/ActivityCard.vue'
const categoryId = shallowRef<number>(),
  categories = ref<CategoryView[]>([]),
  error = shallowRef(false),
  keyword = shallowRef(''),
  page = shallowRef(1),
  total = shallowRef(0),
  items = ref<ActivityView[]>([]),
  loading = shallowRef(false)
const results = useTemplateRef<HTMLElement>('results'),
  pageSize = 9
let epoch = 0,
  closed = false
async function load(toPage?: number) {
  if (toPage) page.value = toPage
  const run = ++epoch
  loading.value = true
  error.value = false
  try {
    const r = await pagePublicActivities({
      categoryId: categoryId.value,
      keyword: keyword.value || undefined,
      page: page.value,
      pageSize,
    })
    if (run === epoch) {
      items.value = r.items
      total.value = r.total
    }
  } catch {
    if (run === epoch) error.value = true
  } finally {
    if (run === epoch) loading.value = false
  }
}
function choose(id?: number) {
  categoryId.value = id
  void load(1)
}
function explore() {
  results.value?.scrollIntoView({
    behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth',
    block: 'start',
  })
}
function reset() {
  keyword.value = ''
  categoryId.value = undefined
  void load(1)
}
onMounted(() => {
  void load(1)
  pagePublicCategories({ type: 'ACTIVITY', page: 1, pageSize: 100 })
    .then((r) => {
      if (!closed) categories.value = r.items
    })
    .catch(() => undefined)
})
onUnmounted(() => {
  closed = true
  epoch++
})
</script>
<template>
  <div class="discover-page">
    <DiscoverHero @explore="explore" />
    <section ref="results" class="discover-results">
      <div class="section-heading">
        <div>
          <h2>发现你的下一次出发</h2>
          <p>从熟悉的城市，走进不一样的日常。</p>
        </div>
        <span class="result-count" aria-live="polite">{{
          error ? '加载失败' : loading ? '正在寻找活动…' : '共 ' + total + ' 场活动'
        }}</span>
      </div>
      <form class="discover-search" @submit.prevent="load(1)">
        <el-input
          v-model="keyword"
          :prefix-icon="Search"
          placeholder="搜索活动名称/地址"
          aria-label="搜索活动名称或地址"
          clearable
          @clear="load(1)"
        /><el-select
          v-model="categoryId"
          placeholder="全部活动分类"
          aria-label="活动分类"
          clearable
          @change="load(1)"
          ><el-option
            v-for="c in categories"
            :key="c.id"
            :label="c.name"
            :value="c.id" /></el-select
        ><el-button type="primary" native-type="submit" :loading="loading">搜索</el-button>
      </form>
      <div v-if="categories.length" class="category-tabs" aria-label="快捷活动分类">
        <button
          type="button"
          :class="{ selected: !categoryId }"
          :aria-pressed="!categoryId"
          @click="choose()"
        >
          全部活动</button
        ><button
          v-for="c in categories.slice(0, 6)"
          :key="c.id"
          type="button"
          :class="{ selected: categoryId === c.id }"
          :aria-pressed="categoryId === c.id"
          @click="choose(c.id)"
        >
          {{ c.name }}
        </button>
      </div>
      <el-result v-if="error" icon="error" title="活动加载失败" sub-title="请检查网络连接后重试。"
        ><template #extra
          ><el-button :icon="RefreshRight" @click="load()">重试</el-button></template
        ></el-result
      >
      <div v-else v-loading="loading" class="activity-grid" :aria-busy="loading">
        <template v-if="!loading && !items.length"
          ><el-empty description="没有找到合适的活动，换个关键词试试"
            ><el-button @click="reset">查看全部活动</el-button></el-empty
          ></template
        ><ActivityCard v-for="(item, i) in items" :key="item.id" :activity="item" :index="i" />
      </div>
      <el-pagination
        v-if="!error && total > 0"
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev,pager,next,total"
        class="discover-pagination"
        @current-change="load()"
      />
    </section>
    <aside class="discover-banner">
      <div>
        <h3>把喜欢的体验，安排在一起。</h3>
        <p>浏览活动套餐，为下一次出发多留一点期待。</p>
      </div>
      <router-link to="/packages">探索活动套餐 <span aria-hidden="true">↗</span></router-link>
    </aside>
  </div>
</template>
<style scoped>
.discover-results {
  scroll-margin-top: 24px;
}
.section-heading h2 {
  font-size: 23px;
}
.section-heading p {
  font-size: 12px;
}
.result-count {
  color: #859285;
  font-size: 11px;
  white-space: nowrap;
}
.discover-search {
  display: flex;
  gap: 12px;
  background: #fff;
  border: 1px solid var(--city-line);
  border-radius: 13px;
  padding: 14px;
  margin-bottom: 17px;
}
.discover-search .el-input {
  flex: 1;
}
.discover-search .el-select {
  width: 185px;
}
.discover-search .el-button {
  min-width: 84px;
}
.category-tabs {
  display: flex;
  gap: 9px;
  flex-wrap: wrap;
  margin: 19px 0 24px;
}
.category-tabs button {
  border: 1px solid transparent;
  background: transparent;
  color: #7c887d;
  padding: 7px 13px;
  font-size: 11px;
  border-radius: 7px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.category-tabs button.selected {
  color: #215c3c;
  background: #e4efdf;
  font-weight: 600;
}
.activity-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
  min-height: 150px;
}
.activity-grid > .el-empty {
  grid-column: 1/-1;
}
.discover-pagination {
  justify-content: center;
  margin-top: 30px;
}
.discover-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 43px;
  padding: 26px 31px;
  background: #e8eee3;
  border-radius: 14px;
}
.discover-banner h3 {
  font-size: 17px;
  margin-bottom: 7px;
}
.discover-banner p {
  color: #798671;
  font-size: 12px;
  margin: 0;
}
.discover-banner a {
  font-size: 12px;
  white-space: nowrap;
  font-weight: 600;
}
.discover-banner a span {
  margin-left: 22px;
  font-size: 17px;
}
@media (max-width: 950px) {
  .activity-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 600px) {
  .activity-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }
  .section-heading {
    gap: 10px;
    align-items: flex-start;
  }
  .section-heading h2 {
    font-size: 20px;
  }
  .section-heading p {
    font-size: 11px;
  }
  .result-count {
    font-size: 10px;
    margin-top: 6px;
  }
  .discover-search {
    flex-wrap: wrap;
    padding: 11px;
    gap: 9px;
  }
  .discover-search .el-input {
    flex-basis: 100%;
  }
  .discover-search .el-select {
    flex: 1;
    width: 0;
  }
  .category-tabs {
    gap: 3px;
  }
  .category-tabs button {
    padding: 7px 10px;
  }
  .discover-banner {
    padding: 23px;
    align-items: flex-start;
    flex-direction: column;
    gap: 17px;
    margin-top: 30px;
  }
}
</style>
