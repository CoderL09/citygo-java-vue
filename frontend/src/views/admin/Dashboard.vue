<script setup lang="ts">
import { computed } from 'vue'
import { Plus, Camera, ArrowRight, Location, Box, Collection, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import DashboardMetrics from '@/components/admin/DashboardMetrics.vue'
import RecentOrders from '@/components/admin/RecentOrders.vue'
import PlatformStatus from '@/components/admin/PlatformStatus.vue'
const auth = useAuthStore()
const name = computed(() => auth.user?.nickname || auth.user?.username || '工作人员')
const entries = computed(() => [
  {
    path: '/admin/activities',
    title: '活动管理',
    description: '维护活动内容、场次与图片',
    icon: Location,
  },
  {
    path: '/admin/packages',
    title: '套餐管理',
    description: '组合城市体验，管理套餐版本',
    icon: Box,
  },
  {
    path: '/admin/categories',
    title: '分类管理',
    description: '整理分类，让活动更好被发现',
    icon: Collection,
  },
  ...(auth.user?.role === 'ADMIN'
    ? [
        {
          path: '/admin/staff',
          title: '员工管理',
          description: '管理团队账号和操作权限',
          icon: User,
        },
      ]
    : []),
])
</script>
<template>
  <div class="dashboard">
    <div class="page-heading">
      <div>
        <h1>你好，{{ name }}</h1>
        <p>欢迎回到工作台，今天也有值得期待的城市体验。</p>
      </div>
      <div class="page-heading-actions">
        <el-button :icon="Camera" @click="$router.push('/admin/checkin')">扫码核销</el-button
        ><el-button type="primary" :icon="Plus" @click="$router.push('/admin/activities')"
          >管理活动</el-button
        >
      </div>
    </div>
    <DashboardMetrics />
    <div class="dashboard-columns">
      <RecentOrders />
      <aside class="quick-panel">
        <div class="quick-heading">
          <h2>快捷工作区</h2>
          <p>常用工具，触手可及</p>
        </div>
        <router-link v-for="entry in entries" :key="entry.path" :to="entry.path" class="quick-entry"
          ><span class="quick-icon"
            ><el-icon><component :is="entry.icon" /></el-icon></span
          ><span class="quick-copy"
            ><strong>{{ entry.title }}</strong
            ><small>{{ entry.description }}</small></span
          ><el-icon class="quick-arrow"><ArrowRight /></el-icon
        ></router-link>
        <div class="work-note">
          <span class="note-line" />
          <p>预约确认后，记得及时关注<br />预约改期和到场核销。</p>
        </div>
      </aside>
    </div>
    <PlatformStatus />
  </div>
</template>
<style scoped>
.dashboard .page-heading h1 {
  font-size: 27px;
  letter-spacing: -0.7px;
  margin-bottom: 8px;
}
.dashboard .page-heading p {
  font-size: 12px;
}
.dashboard .page-heading-actions .el-button {
  font-size: 12px;
  min-height: 38px;
}
.dashboard-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 22px;
  align-items: start;
}
.quick-panel {
  border: 1px solid var(--city-line);
  border-radius: 14px;
  background: #fff;
  overflow: hidden;
}
.quick-heading {
  padding: 24px 23px 13px;
}
.quick-heading h2 {
  font-size: 16px;
  margin-bottom: 5px;
}
.quick-heading p {
  font-size: 11px;
  color: #8a978b;
  margin-bottom: 0;
}
.quick-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 21px;
}
.quick-icon {
  width: 35px;
  height: 35px;
  background: #f0f5ed;
  display: grid;
  place-items: center;
  border-radius: 9px;
  color: #5c8056;
  flex-shrink: 0;
  font-size: 17px;
}
.quick-copy {
  min-width: 0;
}
.quick-copy strong {
  display: block;
  font-size: 12px;
  color: #40563f;
  font-weight: 550;
}
.quick-copy small {
  display: block;
  font-size: 11px;
  color: #6b7969;
  margin-top: 4px;
  line-height: 1.6;
}
.quick-arrow {
  margin-left: auto;
  font-size: 12px;
  color: #a5b39f;
}
.work-note {
  margin: 16px 21px 23px;
  border-radius: 9px;
  padding: 15px;
  background: #f3f6ee;
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.note-line {
  display: block;
  width: 3px;
  height: 28px;
  border-radius: 2px;
  background: #bed199;
  margin-top: 4px;
  flex-shrink: 0;
}
.work-note p {
  color: #869477;
  font-size: 11px;
  line-height: 1.8;
  margin: 0;
}
@media (hover: hover) and (pointer: fine) {
  .quick-entry:hover {
    background: #f6f9f2;
  }
}
@media (max-width: 1200px) {
  .dashboard-columns {
    grid-template-columns: 1fr;
  }
  .quick-panel {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    padding-bottom: 12px;
  }
  .quick-heading {
    grid-column: 1/-1;
  }
  .work-note {
    display: none;
  }
  .quick-entry {
    padding: 13px 22px;
  }
}
@media (max-width: 600px) {
  .dashboard .page-heading h1 {
    font-size: 25px;
  }
  .dashboard .page-heading p {
    font-size: 11px;
  }
  .quick-panel {
    grid-template-columns: 1fr;
  }
  .quick-entry {
    padding: 12px 20px;
  }
  .quick-copy small {
    font-size: 11px;
  }
}
</style>
