<script setup lang="ts">
import { computed, reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import AuthShell from '@/components/ui/AuthShell.vue'
const auth = useAuthStore(),
  router = useRouter(),
  route = useRoute(),
  loading = shallowRef(false),
  form = reactive({ username: '', password: '' })
const adminContext = computed(
  () => typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/admin'),
)
async function submit() {
  if (loading.value) return
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    const safe =
      redirect.startsWith('/') &&
      !redirect.startsWith('//') &&
      (!redirect.startsWith('/admin') || auth.isAdmin)
    await router.push(safe ? redirect : auth.isAdmin ? '/admin' : '/')
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <AuthShell
    :title="adminContext ? '管理端登录' : '欢迎回来'"
    :subtitle="
      adminContext
        ? '使用管理员或运营账号，进入 CityGo 工作台。'
        : '登录 CityGo，继续你的城市探索。'
    "
    ><div class="login-modes">
      <router-link to="/login" :class="{ active: !adminContext }">用户登录</router-link
      ><router-link to="/login?redirect=/admin" :class="{ active: adminContext }"
        >管理端登录</router-link
      >
    </div>
    <el-form :model="form" label-position="top" @submit.prevent="submit"
      ><el-form-item label="用户名"
        ><el-input
          v-model="form.username"
          :prefix-icon="User"
          autocomplete="username"
          placeholder="请输入用户名"
          :disabled="loading" /></el-form-item
      ><el-form-item label="密码"
        ><el-input
          v-model="form.password"
          :prefix-icon="Lock"
          type="password"
          autocomplete="current-password"
          show-password
          placeholder="请输入密码"
          :disabled="loading" /></el-form-item
      ><el-button type="primary" class="auth-submit" native-type="submit" :loading="loading"
        >登录</el-button
      >
      <p v-if="adminContext" class="role-hint">
        管理账号由系统管理员创建，普通注册账号用于活动预约。
      </p>
      <div v-else class="auth-foot">
        还没有账号？<router-link to="/register">创建 CityGo 账号</router-link>
      </div></el-form
    ></AuthShell
  >
</template>
<style scoped>
.login-modes {
  display: flex;
  background: #f2f5f2;
  border-radius: 9px;
  padding: 4px;
  gap: 4px;
  margin-bottom: 27px;
}
.login-modes a {
  width: 50%;
  padding: 9px;
  text-align: center;
  font-size: 12px;
  color: #7a857b;
  border-radius: 7px;
}
.login-modes a.active {
  background: white;
  color: #2e6746;
  box-shadow: 0 1px 5px #294d3210;
  font-weight: 600;
}
.auth-submit {
  width: 100%;
  min-height: 46px;
  margin-top: 8px;
}
.auth-foot {
  font-size: 12px;
  text-align: center;
  color: #909990;
  margin-top: 23px;
}
.auth-foot a {
  font-weight: 600;
  margin-left: 7px;
}
.role-hint {
  font-size: 11px;
  color: #929b91;
  line-height: 1.9;
  margin: 19px 0 0;
}
.el-form-item {
  margin-bottom: 22px;
}
</style>
