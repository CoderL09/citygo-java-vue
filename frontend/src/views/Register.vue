<script setup lang="ts">
import { reactive, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import AuthShell from '@/components/ui/AuthShell.vue'
const auth = useAuthStore(),
  router = useRouter(),
  loading = shallowRef(false),
  form = reactive({ username: '', password: '', nickname: '', phone: '' })
async function submit() {
  if (loading.value) return
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      phone: form.phone || undefined,
    })
    ElMessage.success('注册成功，已自动登录')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <AuthShell title="开启你的城市探索" subtitle="创建账号，把喜欢的活动加入你的日程。"
    ><el-form :model="form" label-position="top" @submit.prevent="submit"
      ><el-form-item label="用户名（3-32 位字母数字下划线）"
        ><el-input
          v-model="form.username"
          autocomplete="username"
          placeholder="例如 city_explorer"
          :disabled="loading" /></el-form-item
      ><el-form-item label="密码（至少 6 位）"
        ><el-input
          v-model="form.password"
          type="password"
          autocomplete="new-password"
          show-password
          placeholder="设置你的登录密码"
          :disabled="loading"
      /></el-form-item>
      <div class="optional-fields">
        <el-form-item label="昵称（可选）"
          ><el-input
            v-model="form.nickname"
            autocomplete="nickname"
            placeholder="怎么称呼你"
            :disabled="loading" /></el-form-item
        ><el-form-item label="手机号（可选）"
          ><el-input
            v-model="form.phone"
            autocomplete="tel"
            placeholder="联系电话"
            :disabled="loading"
        /></el-form-item>
      </div>
      <el-button type="primary" class="auth-submit" native-type="submit" :loading="loading"
        >注册</el-button
      >
      <div class="auth-foot">已有账号？<router-link to="/login">去登录</router-link></div></el-form
    ></AuthShell
  >
</template>
<style scoped>
.auth-submit {
  width: 100%;
  min-height: 46px;
  margin-top: 7px;
}
.auth-foot {
  text-align: center;
  font-size: 12px;
  color: #929b91;
  margin-top: 23px;
}
.auth-foot a {
  font-weight: 600;
  margin-left: 7px;
}
.optional-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.el-form-item {
  margin-bottom: 20px;
}
@media (max-width: 380px) {
  .optional-fields {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
