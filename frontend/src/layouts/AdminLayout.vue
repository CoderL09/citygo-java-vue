<script setup lang="ts">
import { computed, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Menu, SwitchButton, TopRight, ArrowRight } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useNotifications } from '@/stores/notifications'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import AdminNavigation from '@/components/admin/AdminNavigation.vue'
const auth = useAuthStore(),
  notices = useNotifications(),
  route = useRoute(),
  router = useRouter(),
  mobileOpen = shallowRef(false)
const title = computed(
  () =>
    (
      ({
        '/admin': '运营首页',
        '/admin/orders': '订单工作台',
        '/admin/checkin': '扫码核销',
        '/admin/reports': '运营统计',
        '/admin/activities': '活动管理',
        '/admin/packages': '套餐管理',
        '/admin/categories': '分类管理',
        '/admin/staff': '员工管理',
        '/admin/audit': '操作审计',
        '/admin/refunds': '退款记录',
        '/admin/notifications': '通知中心',
      }) as Record<string, string>
    )[route.path] || '管理工作台',
)
const displayName = computed(() => auth.user?.nickname || auth.user?.username || '工作人员')
const dateLabel = new Intl.DateTimeFormat('zh-CN', {
  timeZone: 'Asia/Shanghai',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())
watch(
  () => route.path,
  () => (mobileOpen.value = false),
)
async function handleLogout() {
  try {
    await auth.logout()
  } catch {
    return
  }
  router.push('/login?redirect=/admin')
}
</script>
<template>
  <div class="admin-layout">
    <a href="#admin-main" class="skip-link">跳至页面内容</a>
    <aside class="admin-sidebar">
      <router-link to="/admin" class="admin-brand" aria-label="CityGo 管理端首页"
        ><BrandLogo light subtitle="运营工作台" /></router-link
      ><AdminNavigation />
      <div class="sidebar-bottom">
        <div class="sidebar-identity">
          <span class="staff-avatar">{{ displayName.slice(0, 1) }}</span>
          <div>
            <strong>{{ displayName }}</strong
            ><small>{{ auth.user?.role === 'ADMIN' ? '系统管理员' : '运营人员' }}</small>
          </div>
          <el-button text class="exit-button" aria-label="退出登录" @click="handleLogout"
            ><el-icon><SwitchButton /></el-icon
          ></el-button>
        </div>
      </div>
    </aside>
    <div class="admin-body">
      <header class="admin-topbar">
        <div class="breadcrumb">
          <el-button
            text
            class="mobile-menu-button"
            aria-label="打开管理导航"
            @click="mobileOpen = true"
            ><el-icon :size="21"><Menu /></el-icon></el-button
          ><span class="workspace-label">管理工作台</span
          ><el-icon class="breadcrumb-divider"><ArrowRight /></el-icon><strong>{{ title }}</strong>
        </div>
        <div class="topbar-actions">
          <span class="date-label">{{ dateLabel }}</span
          ><router-link to="/" class="view-user"
            ><el-icon><TopRight /></el-icon>查看用户端</router-link
          ><router-link to="/admin/notifications" class="admin-notice" aria-label="通知"
            ><el-badge :value="notices.unread" :hidden="!notices.unread"
              ><el-icon :size="20"><Bell /></el-icon></el-badge
          ></router-link>
        </div>
      </header>
      <main id="admin-main" class="admin-content" tabindex="-1"><router-view /></main>
      <footer class="admin-footer">
        CityGo 运营工作台<span>每一份预约，都值得被认真对待。</span>
      </footer>
    </div>
    <el-drawer
      v-model="mobileOpen"
      direction="ltr"
      size="260px"
      title="管理端导航"
      class="admin-mobile-drawer"
      destroy-on-close
      ><BrandLogo light subtitle="运营工作台" /><AdminNavigation
        @navigate="mobileOpen = false"
      /><el-button class="mobile-logout" @click="handleLogout">退出登录</el-button></el-drawer
    >
  </div>
</template>
<style scoped>
.admin-layout {
  min-height: 100svh;
  background: #f3f6f4;
  display: flex;
}
.admin-sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  width: 236px;
  background: #173e32;
  display: flex;
  flex-direction: column;
  z-index: 20;
  overflow-y: auto;
}
.admin-brand {
  padding: 31px 28px 15px;
  display: block;
}
.sidebar-bottom {
  margin-top: auto;
  padding: 18px;
  border-top: 1px solid #315545;
}
.sidebar-identity {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #e1ecde;
}
.staff-avatar {
  width: 34px;
  height: 34px;
  background: #66856a;
  border-radius: 10px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}
.sidebar-identity strong {
  display: block;
  font-size: 12px;
  font-weight: 550;
  max-width: 105px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sidebar-identity small {
  font-size: 10px;
  color: #8fab98;
  display: block;
  margin-top: 2px;
}
.sidebar-identity .exit-button {
  margin-left: auto;
  color: #b1c9b8;
  padding: 5px;
  min-height: 28px;
}
.admin-body {
  margin-left: 236px;
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.admin-topbar {
  height: 78px;
  min-height: 78px;
  background: white;
  border-bottom: 1px solid var(--city-line);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 0 34px;
}
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 13px;
  font-size: 12px;
  white-space: nowrap;
}
.breadcrumb strong {
  font-weight: 550;
  font-size: 13px;
}
.workspace-label {
  color: #86958b;
}
.breadcrumb-divider {
  color: #aab8ae;
  font-size: 10px;
}
.topbar-actions {
  display: flex;
  align-items: center;
  gap: 23px;
}
.date-label {
  font-size: 11px;
  color: #87938a;
}
.view-user {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #627569;
  font-size: 12px;
}
.admin-notice {
  color: #607466;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin-right: 8px;
}
.admin-content {
  padding: 32px 34px;
  flex: 1;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  min-width: 0;
}
.admin-footer {
  display: flex;
  justify-content: space-between;
  padding: 19px 34px;
  font-size: 11px;
  color: #929e95;
}
.mobile-menu-button {
  display: none;
}
.admin-footer span {
  color: #a4aea7;
}
@media (max-width: 1100px) {
  .admin-topbar {
    padding: 0 23px;
  }
  .admin-content {
    padding: 27px 23px;
  }
  .date-label {
    display: none;
  }
  .topbar-actions {
    gap: 16px;
  }
  .admin-sidebar {
    width: 218px;
  }
  .admin-body {
    margin-left: 218px;
  }
  .admin-brand {
    padding-left: 23px;
  }
}
@media (max-width: 800px) {
  .admin-sidebar {
    display: none;
  }
  .admin-body {
    margin-left: 0;
  }
  .admin-topbar {
    height: 66px;
    min-height: 66px;
    padding: 0 17px;
  }
  .mobile-menu-button {
    display: inline-flex;
    padding: 5px;
  }
  .workspace-label,
  .breadcrumb-divider {
    display: none;
  }
  .breadcrumb {
    gap: 9px;
  }
  .admin-content {
    padding: 24px 16px;
  }
  .topbar-actions {
    gap: 17px;
  }
  .view-user {
    font-size: 11px;
  }
  .admin-footer {
    padding: 15px 18px;
  }
  .admin-footer span {
    display: none;
  }
}
</style>
<style>
.admin-mobile-drawer {
  background: #173e32 !important;
  color: #e7f0e5;
}
.admin-mobile-drawer .el-drawer__header {
  color: #e7f0e5;
  margin-bottom: 0;
}
.admin-mobile-drawer .el-drawer__body {
  padding: 18px 12px;
}
.admin-mobile-drawer .brand-logo {
  margin-left: 16px;
}
.admin-mobile-drawer .admin-navigation {
  padding-top: 8px;
}
.admin-mobile-drawer .mobile-logout {
  margin-left: 16px;
  background: #2d5b45;
  border-color: #4c7058;
  color: #fff;
}
</style>
