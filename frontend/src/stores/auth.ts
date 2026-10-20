import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import { getToken, setToken } from '@/api/token'
import type { UserView } from '@/api/types'

/**
 * 认证状态：token 仅存内存（刷新需重新登录，实施书 §4 首版限制）。
 * 退出先由后端将 jti 写入 Redis 撤销列表，成功后清理本地状态。
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserView | null>(null)

  const isAdmin = computed(
    () => user.value?.role === 'ADMIN' || user.value?.role === 'OPERATOR',
  )

  function applyUser(value: UserView | null) {
    user.value = value
  }

  async function login(username: string, password: string) {
    const res = await authApi.login(username, password)
    setToken(res.token)
    user.value = res.user
    return res.user
  }

  async function register(payload: {
    username: string
    password: string
    nickname?: string
    phone?: string
  }) {
    const res = await authApi.register(payload)
    setToken(res.token)
    user.value = res.user
    return res.user
  }

  async function logout() {
    await authApi.logout()
    setToken(null)
    user.value = null
  }

  /** 页面刷新后 token 丢失，按未登录处理（首版限制） */
  function initFromToken() {
    if (!getToken()) return
    authApi
      .fetchMe()
      .then((me) => (user.value = me))
      .catch(() => {
        setToken(null)
        user.value = null
      })
  }

  return { user, isAdmin, applyUser, login, register, logout, initFromToken }
})
