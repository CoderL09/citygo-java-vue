import { del, get, http, put } from './http'
import type { AcceptingOrdersView, FileView } from './types'

export function uploadFile(file: File) {
  const form = new FormData()
  form.append('file', file)
  // 响应拦截器已解包为 data，绕过 axios 静态类型
  return http.post('/admin/files', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }) as unknown as Promise<FileView>
}

export function deleteFile(id: number) {
  return del<void>(`/admin/files/${id}`)
}

export function getAcceptingOrders() {
  return get<AcceptingOrdersView>('/settings/accepting-orders')
}

export function setAcceptingOrders(acceptingOrders: boolean) {
  return put<AcceptingOrdersView>('/admin/settings/accepting-orders', { acceptingOrders })
}
