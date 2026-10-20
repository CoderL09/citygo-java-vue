<template>
  <el-card style="max-width: 520px; margin: auto">
    <template #header>个人资料</template>
    <el-form label-position="top" @submit.prevent="save">
      <el-form-item label="用户名"><el-input :model-value="auth.user?.username" disabled /></el-form-item>
      <el-form-item label="昵称"><el-input v-model="nickname" maxlength="64" /></el-form-item>
      <el-form-item label="联系电话"><el-input v-model="phone" maxlength="20" /></el-form-item>
      <el-button type="primary" :loading="saving" @click="save">保存资料</el-button>
    </el-form>
  </el-card>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { put } from '@/api/http'
import type { UserView } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const nickname = ref(auth.user?.nickname || '')
const phone = ref(auth.user?.phone || '')
const saving = ref(false)
async function save() {
  if (saving.value) return
  saving.value = true
  try {
    auth.applyUser(await put<UserView>('/user/profile', { nickname: nickname.value, phone: phone.value }))
    ElMessage.success('资料已保存')
  } finally { saving.value = false }
}
</script>
