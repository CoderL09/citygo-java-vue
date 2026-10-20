<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Bell, ArrowDown, ShoppingBag } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useNotifications } from '@/stores/notifications'
import BrandLogo from '@/components/ui/BrandLogo.vue'
const auth = useAuthStore(),
  notices = useNotifications(),
  router = useRouter()
const route = useRoute()
const pageHeading = computed(
  () =>
    (
      ({
        '/contacts': {
          title: '为下一次出发，做好准备。',
          description: '管理常用联系人，预约时直接选择。',
        },
        '/cart': {
          title: '你的下一站，已在这里。',
          description: '确认喜欢的活动与场次，安排这次出发。',
        },
        '/orders': {
          title: '我的城市预约',
          description: '查看预约进度、调整行程，出发时出示核销码。',
        },
        '/profile': {
          title: '认识一下，城市探索者。',
          description: '更新个人资料，让每次预约更方便。',
        },
        '/notifications': {
          title: '消息与提醒',
          description: '及时了解预约进度，不错过每一个重要时刻。',
        },
      }) as Record<string, { title: string; description: string }>
    )[route.path],
)
const displayName = computed(() => auth.user?.nickname || auth.user?.username || '')
async function handleLogout() {
  try {
    await auth.logout()
  } catch {
    return
  }
  router.push('/')
}
</script>
<template>
  <div class="user-layout">
    <a href="#main-content" class="skip-link">跳至页面内容</a>
    <header class="user-header">
      <div class="header-inner">
        <router-link to="/" class="brand" aria-label="CityGo 城市活动首页"
          ><BrandLogo
        /></router-link>
        <nav class="user-nav" aria-label="用户端导航">
          <router-link to="/" exact-active-class="is-current" active-class="">活动</router-link
          ><router-link to="/packages" active-class="is-current">套餐</router-link
          ><template v-if="!auth.user || auth.user.role === 'USER'"
            ><router-link to="/contacts" active-class="is-current">联系人</router-link
            ><router-link to="/cart" active-class="is-current"
              ><el-icon><ShoppingBag /></el-icon>购物车</router-link
            ><router-link to="/orders" active-class="is-current">我的订单</router-link></template
          >
        </nav>
        <div class="user-actions">
          <router-link to="/admin" class="admin-entry">管理端</router-link
          ><router-link
            v-if="auth.user"
            :to="auth.isAdmin ? '/admin/notifications' : '/notifications'"
            class="notice-link"
            aria-label="通知"
            ><el-badge :value="notices.unread" :hidden="!notices.unread"
              ><el-icon :size="20"><Bell /></el-icon></el-badge></router-link
          ><el-dropdown v-if="auth.user" trigger="click"
            ><button type="button" class="account-button" :aria-label="'账户菜单：' + displayName">
              <span class="avatar">{{ displayName.slice(0, 1) }}</span
              ><span class="account-name">{{ displayName }}</span
              ><el-icon><ArrowDown /></el-icon></button
            ><template #dropdown
              ><el-dropdown-menu
                ><el-dropdown-item @click="$router.push('/profile')">个人资料</el-dropdown-item
                ><el-dropdown-item v-if="auth.isAdmin" @click="$router.push('/admin')"
                  >进入管理端</el-dropdown-item
                ><el-dropdown-item divided @click="handleLogout"
                  >退出登录</el-dropdown-item
                ></el-dropdown-menu
              ></template
            ></el-dropdown
          ><template v-else
            ><router-link to="/login" class="login-link">登录</router-link
            ><router-link to="/register" class="join-link">开始探索</router-link></template
          >
        </div>
      </div>
    </header>
    <main id="main-content" class="main" tabindex="-1">
      <div v-if="pageHeading" class="page-heading">
        <div>
          <h1>{{ pageHeading.title }}</h1>
          <p>{{ pageHeading.description }}</p>
        </div>
      </div>
      <router-view />
    </main>
    <footer class="user-footer">
      <BrandLogo />
      <p>让每一次出发，都离喜欢的生活近一点。</p>
      <div>
        <router-link to="/">探索活动</router-link><router-link to="/packages">精选套餐</router-link
        ><router-link to="/admin">管理端入口</router-link>
      </div>
      <small>CityGo 城市活动预约平台</small>
    </footer>
  </div>
</template>
<style scoped>
.user-layout {
  min-height: 100svh;
  display: flex;
  flex-direction: column;
}
.user-header {
  background: #fff;
  border-bottom: 1px solid var(--city-line);
}
.header-inner {
  max-width: 1280px;
  margin: auto;
  min-height: 88px;
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 14px 36px;
}
.brand {
  flex-shrink: 0;
}
.user-nav {
  display: flex;
  align-items: center;
  gap: 25px;
  align-self: stretch;
}
.user-nav a {
  display: flex;
  align-items: center;
  gap: 5px;
  position: relative;
  color: #6a756e;
  white-space: nowrap;
  font-size: 14px;
}
.user-nav a.is-current {
  color: var(--city-pine);
  font-weight: 650;
}
.user-nav a.is-current::after {
  position: absolute;
  content: '';
  bottom: 2px;
  height: 3px;
  width: 18px;
  background: var(--city-pine);
  border-radius: 2px;
  left: calc(50% - 9px);
}
.user-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 20px;
  white-space: nowrap;
}
.admin-entry {
  font-size: 12px;
  color: #879087;
}
.login-link {
  color: var(--city-ink);
}
.join-link {
  background: var(--city-pine);
  color: #fff;
  padding: 10px 16px;
  border-radius: 9px;
  font-size: 13px;
}
.notice-link {
  display: inline-flex;
  padding: 6px;
  color: #54685d;
}
.account-button {
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  color: var(--city-ink);
}
.account-name {
  max-width: 95px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.avatar {
  background: #e7efe9;
  color: #336c50;
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 600;
}
.main {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 36px;
  flex: 1;
  min-width: 0;
}
.user-footer {
  max-width: 1208px;
  width: calc(100% - 72px);
  margin: 35px auto 0;
  padding: 32px 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  border-top: 1px solid var(--city-line);
}
.user-footer :deep(.brand-word) {
  font-size: 21px;
}
.user-footer :deep(.brand-caption) {
  display: none;
}
.user-footer p {
  font-size: 12px;
  color: #859087;
  margin: 0;
}
.user-footer div {
  margin-left: auto;
  display: flex;
  gap: 18px;
  font-size: 12px;
}
.user-footer div a {
  color: #66766c;
}
.user-footer small {
  width: 100%;
  font-size: 11px;
  color: #9aa39c;
}
@media (hover: hover) and (pointer: fine) {
  .user-nav a:hover,
  .admin-entry:hover {
    color: var(--city-pine);
  }
}
@media (max-width: 1100px) {
  .header-inner {
    gap: 24px;
    padding: 14px 24px;
  }
  .user-nav {
    gap: 18px;
  }
  .user-actions {
    gap: 12px;
  }
  .admin-entry {
    display: none;
  }
  .main {
    padding: 28px 24px;
  }
  .user-footer {
    width: calc(100% - 48px);
  }
}
@media (max-width: 800px) {
  .header-inner {
    flex-wrap: wrap;
    gap: 12px 18px;
    padding: 16px 20px 0;
  }
  .user-nav {
    order: 3;
    width: 100%;
    gap: 27px;
    min-height: 48px;
    overflow: auto;
    flex-wrap: nowrap;
  }
  .user-nav a {
    min-height: 44px;
  }
  .user-actions {
    gap: 15px;
  }
  .account-name {
    display: none;
  }
  .main {
    padding: 24px 18px;
  }
  .user-footer {
    width: calc(100% - 36px);
    gap: 15px;
  }
  .user-footer p {
    width: 100%;
  }
  .user-footer div {
    margin-left: 0;
  }
  .join-link {
    padding: 8px 12px;
  }
  .user-footer {
    margin-top: 15px;
  }
  .user-actions .admin-entry {
    display: none;
  }
}
@media (max-width: 380px) {
  .user-nav {
    gap: 20px;
  }
  .user-nav a {
    font-size: 13px;
  }
  .header-inner {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
