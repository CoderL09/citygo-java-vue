import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, setToken } from './token'
import type { ApiResponse } from './types'
import router from '@/router'

/**
 * 统一 axios 实例：注入 JWT、解包 ApiResponse（code=0 成功）、
 * 401/禁用时清 token 并跳登录（后端校验为准，前端守卫只是体验）。
 */
export const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const t = getToken()
  if (t) config.headers.Authorization = `Bearer ${t}`
  return config
})

const AUTH_CODES = new Set([40100, 40101, 40103])

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse
    if (body && typeof body.code === 'number') {
      if (body.code === 0) return body.data as never
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new ApiError(body.code, body.message))
    }
    return response.data
  },
  (error) => {
    const body = error?.response?.data as ApiResponse | undefined
    const silent = error?.config?.headers?.['X-Citygo-Silent'] === '1'
    const code = body?.code
    const message = body?.message ?? '网络异常，请稍后重试'
    const currentToken=getToken()
    const belongsToCurrent=error?.config?.headers?.Authorization === (currentToken ? `Bearer ${currentToken}` : undefined)
    if (AUTH_CODES.has(code as number) && !belongsToCurrent) return Promise.reject(new ApiError(code ?? -1,message))
    if (AUTH_CODES.has(code as number)) {
      setToken(null)
      ElMessage.error(message)
      const current = router.currentRoute.value
      if (current.path !== '/login') {
        router.push({ path: '/login', query: { redirect: current.fullPath } })
      }
    } else if (!silent) {
      ElMessage.error(message)
    }
    return Promise.reject(new ApiError(code ?? -1, message))
  },
)

export class ApiError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return http.get(url, config) as Promise<T>
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return http.post(url, data, config) as Promise<T>
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return http.put(url, data, config) as Promise<T>
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return http.delete(url, config) as Promise<T>
}
