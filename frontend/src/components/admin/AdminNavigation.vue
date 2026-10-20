<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  House,
  Tickets,
  Camera,
  DataAnalysis,
  Collection,
  Location,
  Box,
  User,
  Document,
  RefreshLeft,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
const route = useRoute(),
  auth = useAuthStore()
const emit = defineEmits<{ navigate: [] }>()
const groups = computed(() => [
  {
    title: '工作空间',
    items: [
      { path: '/admin', label: '运营首页', icon: House },
      { path: '/admin/orders', label: '订单工作台', icon: Tickets },
      { path: '/admin/checkin', label: '扫码核销', icon: Camera },
      { path: '/admin/reports', label: '运营统计', icon: DataAnalysis },
    ],
  },
  {
    title: '内容与资源',
    items: [
      { path: '/admin/activities', label: '活动管理', icon: Location },
      { path: '/admin/packages', label: '套餐管理', icon: Box },
      { path: '/admin/categories', label: '分类管理', icon: Collection },
    ],
  },
  {
    title: '管理与记录',
    items: [
      { path: '/admin/refunds', label: '退款记录', icon: RefreshLeft },
      ...(auth.user?.role === 'ADMIN'
        ? [
            { path: '/admin/staff', label: '员工管理', icon: User },
            { path: '/admin/audit', label: '操作审计', icon: Document },
          ]
        : []),
    ],
  },
])
</script>
<template>
  <nav class="admin-navigation" aria-label="管理端导航">
    <el-menu router :default-active="route.path" @select="emit('navigate')"
      ><template v-for="group in groups" :key="group.title"
        ><div class="nav-section">{{ group.title }}</div>
        <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path"
          ><el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span
          ><span class="active-mark" aria-hidden="true" /></el-menu-item></template
    ></el-menu>
  </nav>
</template>
<style scoped>
.admin-navigation {
  padding: 0 16px 25px;
}
.admin-navigation :deep(.el-menu) {
  background: transparent;
  border-right: 0;
  --el-menu-text-color: #b4c7bb;
  --el-menu-hover-bg-color: #244e3f;
  --el-menu-active-color: #e7f4dc;
}
.nav-section {
  color: #839d8d;
  font-size: 11px;
  letter-spacing: 1px;
  padding: 25px 16px 10px;
}
.admin-navigation :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  border-radius: 9px;
  margin: 4px 0;
  font-size: 13px;
  padding: 0 15px !important;
  gap: 9px;
}
.admin-navigation :deep(.el-menu-item .el-icon) {
  font-size: 18px;
  margin-right: 2px;
}
.admin-navigation :deep(.el-menu-item.is-active) {
  background: #2f5d47;
  color: #f1f8e9;
}
.active-mark {
  display: none;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #cce7ae;
  margin-left: auto;
}
.admin-navigation :deep(.el-menu-item.is-active) .active-mark {
  display: block;
}
</style>
